package online.veloraplugins.gamelink.paper.services

import online.velora.framework.adventure.ComponentUtil
import online.veloraplugins.gamelink.api.game.GameInstance
import online.veloraplugins.gamelink.paper.VeloraGameLinkPlugin
import online.veloraplugins.gamelink.paper.configurations.GameDisplaysConfig
import online.veloraplugins.gamelink.paper.configurations.GameSignsConfig
import online.veloraplugins.gamelink.paper.extensions.toLocation
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Sign
import org.bukkit.block.sign.Side
import java.util.concurrent.ConcurrentHashMap

class GameSignService(
    private val plugin: VeloraGameLinkPlugin,
    private val gameRedisService: GameRedisService
) {

    /*
     * Stable sign assignments
     *
     * key:
     *   world:x:y:z
     *
     * value:
     *   game instance id
     */

    private val assignments =
        ConcurrentHashMap<String, String>()

    /*
     * Refresh all signs
     */

    fun refreshAll() {

        val groupedSigns = plugin
            .gameSignsConfig
            .signs
            .groupBy {
                it.gameType.lowercase()
            }

        groupedSigns.forEach { (gameType, signs) ->

            refreshGameType(
                gameType = gameType,
                signs = signs
            )
        }

        cleanupRemovedSigns()

        plugin.debug(
            "SIGN",
            "Refreshed ${plugin.gameSignsConfig.signs.size} configured sign(s)."
        )
    }

    /*
     * Refresh signs for one game type
     */

    fun refreshGameType(
        gameType: String
    ) {

        val signs = plugin
            .gameSignsConfig
            .signs
            .filter {
                it.gameType.equals(
                    gameType,
                    ignoreCase = true
                )
            }

        refreshGameType(
            gameType = gameType,
            signs = signs
        )
    }

    private fun refreshGameType(
        gameType: String,
        signs: List<GameSignsConfig.GameSign>
    ) {

        if (signs.isEmpty()) {
            return
        }

        val displayConfig = findGameDisplayConfig(
            gameType
        ) ?: run {

            plugin.debug(
                "SIGN",
                "No display configuration found for game type '$gameType'."
            )

            return
        }

        /*
         * Only games which are allowed to occupy a sign.
         *
         * Important:
         * showState controls display visibility.
         * allowJoin does NOT determine whether the game
         * may occupy a sign.
         */

        val availableGames = gameRedisService
            .getByType(gameType)
            .asSequence()
            .filter {
                isDisplayable(
                    game = it,
                    gameConfig = displayConfig
                )
            }
            .sortedWith(
                compareByDescending<GameInstance> { game ->
                    getPriority(
                        game = game,
                        gameConfig = displayConfig
                    )
                }.thenByDescending {
                    it.players
                }
            )
            .toList()

        /*
         * Prevent one game from being assigned
         * to multiple signs of the same game type.
         */

        val usedGameIds =
            mutableSetOf<String>()

        signs.forEach { signConfig ->

            refreshSign(
                signConfig = signConfig,
                gameType = gameType,
                gameConfig = displayConfig,
                availableGames = availableGames,
                usedGameIds = usedGameIds
            )
        }
    }

    /*
     * Refresh individual sign
     */

    private fun refreshSign(
        signConfig: GameSignsConfig.GameSign,
        gameType: String,
        gameConfig: GameDisplaysConfig.GameTypeDisplay,
        availableGames: List<GameInstance>,
        usedGameIds: MutableSet<String>
    ) {

        val location = signConfig
            .location
            .toLocation()
            ?: run {

                plugin.debug(
                    "SIGN",
                    "Unable to resolve sign location " +
                            "'${signConfig.location.world}:" +
                            "${signConfig.location.x}," +
                            "${signConfig.location.y}," +
                            "${signConfig.location.z}'."
                )

                return
            }

        val key = signKey(
            location
        )

        val assignedGameId = assignments[
            key
        ]

        /*
         * Check whether the game currently assigned
         * to this sign is still valid.
         */

        val assignedGame = assignedGameId
            ?.let { gameId ->

                gameRedisService.get(
                    gameId
                )
            }

        /*
         * Keep the existing assignment when:
         *
         * - game still exists
         * - same game type
         * - state exists
         * - showState = true
         * - game isn't already used by another sign
         */

        if (
            assignedGame != null &&
            assignedGame.type.equals(
                gameType,
                ignoreCase = true
            ) &&
            isDisplayable(
                game = assignedGame,
                gameConfig = gameConfig
            ) &&
            assignedGame.id !in usedGameIds
        ) {

            usedGameIds += assignedGame.id

            updateSign(
                location = location,
                gameType = gameType,
                game = assignedGame
            )

            return
        }

        /*
         * Existing assignment is invalid.
         *
         * This is important:
         * immediately free the sign before looking
         * for another game.
         */

        if (assignedGameId != null) {

            assignments.remove(
                key
            )

            plugin.debug(
                "SIGN",
                "Unallocated game '$assignedGameId' from sign '$key'."
            )
        }

        /*
         * Find replacement.
         */

        val replacement = availableGames
            .firstOrNull {
                it.id !in usedGameIds
            }

        if (replacement != null) {

            assignments[key] =
                replacement.id

            usedGameIds +=
                replacement.id

            plugin.debug(
                "SIGN",
                "Assigned game '${replacement.id}' " +
                        "(${replacement.state}) to sign '$key'."
            )

            updateSign(
                location = location,
                gameType = gameType,
                game = replacement
            )

            return
        }

        /*
         * Nothing available.
         *
         * Sign intentionally stays unassigned.
         */

        assignments.remove(
            key
        )

        updateSearchingSign(
            location = location,
            gameType = gameType,
            gameConfig = gameConfig
        )

        plugin.debug(
            "SIGN",
            "Sign '$key' is unassigned; showing searching-for-games."
        )
    }

    /*
     * Update sign
     */

    fun updateSign(
        location: Location,
        gameType: String,
        game: GameInstance?
    ) {

        val gameConfig = findGameDisplayConfig(
            gameType
        ) ?: run {

            plugin.debug(
                "SIGN",
                "No display configuration found for game type '$gameType'."
            )

            return
        }

        if (game == null) {

            /*
             * Null game always means this sign is free.
             */

            assignments.remove(
                signKey(location)
            )

            updateSearchingSign(
                location = location,
                gameType = gameType,
                gameConfig = gameConfig
            )

            return
        }

        val stateDisplay = findStateDisplay(
            gameConfig = gameConfig,
            state = game.state
        )

        /*
         * Unknown state.
         *
         * Free the sign and use fallback.
         */

        if (stateDisplay == null) {

            assignments.remove(
                signKey(location)
            )

            plugin.debug(
                "SIGN",
                "No display configured for game '${game.id}' " +
                        "state '${game.state}'. " +
                        "Unallocating sign and using searching-for-games."
            )

            updateSearchingSign(
                location = location,
                gameType = gameType,
                gameConfig = gameConfig
            )

            return
        }

        /*
         * State explicitly says it should not occupy signs.
         */

        if (!stateDisplay.showState) {

            assignments.remove(
                signKey(location)
            )

            plugin.debug(
                "SIGN",
                "Game '${game.id}' entered state '${game.state}' " +
                        "with showState=false. Sign unallocated."
            )

            updateSearchingSign(
                location = location,
                gameType = gameType,
                gameConfig = gameConfig
            )

            return
        }

        render(
            location = location,
            gameType = gameType,
            game = game,
            display = stateDisplay
        )
    }

    /*
     * Searching display
     */

    private fun updateSearchingSign(
        location: Location,
        gameType: String,
        gameConfig: GameDisplaysConfig.GameTypeDisplay
    ) {

        /*
         * A searching sign must never retain an assignment.
         */

        assignments.remove(
            signKey(location)
        )

        render(
            location = location,
            gameType = gameType,
            game = null,
            display = gameConfig.searchingForGames
        )
    }

    /*
     * Render sign
     */

    private fun render(
        location: Location,
        gameType: String,
        game: GameInstance?,
        display: GameDisplaysConfig.SignDisplay
    ) {

        val block = location.block

        val material = Material.matchMaterial(
            display.material
        ) ?: run {

            plugin.debug(
                "SIGN",
                "Invalid sign material '${display.material}'."
            )

            return
        }

        if (!isSignMaterial(material)) {

            plugin.debug(
                "SIGN",
                "Material '${display.material}' is not a sign material."
            )

            return
        }

        /*
         * Change sign material if required.
         */

        if (block.type != material) {
            block.type = material
        }

        val sign = block.state as? Sign
            ?: run {

                plugin.debug(
                    "SIGN",
                    "Block at '${formatLocation(location)}' " +
                            "is not a sign after applying '$material'."
                )

                return
            }

        val side = sign.getSide(
            Side.FRONT
        )

        val lines = display.lines
            .take(4)
            .map {
                replacePlaceholders(
                    text = it,
                    gameType = gameType,
                    game = game
                )
            }
            .toMutableList()

        while (lines.size < 4) {
            lines += ""
        }

        lines.forEachIndexed { index, line ->

            side.line(
                index,
                ComponentUtil.parse(
                    line
                )
            )
        }

        sign.isWaxed = true

        sign.update(
            true,
            false
        )
    }

    /*
     * Displayable
     */

    fun isDisplayable(
        game: GameInstance
    ): Boolean {

        val gameConfig = findGameDisplayConfig(
            game.type
        ) ?: return false

        return isDisplayable(
            game = game,
            gameConfig = gameConfig
        )
    }

    private fun isDisplayable(
        game: GameInstance,
        gameConfig: GameDisplaysConfig.GameTypeDisplay
    ): Boolean {

        val stateDisplay = findStateDisplay(
            gameConfig = gameConfig,
            state = game.state
        ) ?: return false

        return stateDisplay.showState
    }

    /*
     * Priority
     */

    private fun getPriority(
        game: GameInstance,
        gameConfig: GameDisplaysConfig.GameTypeDisplay
    ): Int {

        return findStateDisplay(
            gameConfig = gameConfig,
            state = game.state
        )?.priority ?: Int.MIN_VALUE
    }

    /*
     * Config lookup
     */

    private fun findGameDisplayConfig(
        gameType: String
    ): GameDisplaysConfig.GameTypeDisplay? {

        return plugin
            .gameDisplaysConfig
            .games
            .entries
            .firstOrNull {
                it.key.equals(
                    gameType,
                    ignoreCase = true
                )
            }
            ?.value
    }

    private fun findStateDisplay(
        gameConfig: GameDisplaysConfig.GameTypeDisplay,
        state: String
    ): GameDisplaysConfig.SignDisplay? {

        return gameConfig
            .states
            .entries
            .firstOrNull {
                it.key.equals(
                    state,
                    ignoreCase = true
                )
            }
            ?.value
    }

    /*
     * Assignment lookup
     */

    fun getAssignedGameId(
        location: Location
    ): String? {

        return assignments[
            signKey(location)
        ]
    }

    fun getAssignedGame(
        location: Location
    ): GameInstance? {

        val gameId = getAssignedGameId(
            location
        ) ?: return null

        val game = gameRedisService.get(
            gameId
        )

        /*
         * Redis game disappeared.
         * Clean assignment immediately.
         */

        if (game == null) {

            assignments.remove(
                signKey(location)
            )

            return null
        }

        return game
    }

    /*
     * Assignment management
     */

    fun clearAssignment(
        location: Location
    ) {

        val removed = assignments.remove(
            signKey(location)
        )

        if (removed != null) {

            plugin.debug(
                "SIGN",
                "Cleared assignment '$removed' from '${signKey(location)}'."
            )
        }
    }

    fun clearAssignments() {

        val count = assignments.size

        assignments.clear()

        plugin.debug(
            "SIGN",
            "Cleared $count sign assignment(s)."
        )
    }

    /*
     * Remove config entries which no longer exist
     */

    private fun cleanupRemovedSigns() {

        val configuredKeys = plugin
            .gameSignsConfig
            .signs
            .mapNotNull {
                it.location
                    .toLocation()
                    ?.let(::signKey)
            }
            .toSet()

        val removed = assignments.keys
            .filter {
                it !in configuredKeys
            }

        removed.forEach {
            assignments.remove(it)
        }

        if (removed.isNotEmpty()) {

            plugin.debug(
                "SIGN",
                "Cleaned ${removed.size} stale sign assignment(s)."
            )
        }
    }

    /*
     * Placeholders
     */

    private fun replacePlaceholders(
        text: String,
        gameType: String,
        game: GameInstance?
    ): String {

        return text
            .replace(
                "{game}",
                game?.type ?: gameType
            )
            .replace(
                "{game_id}",
                game?.id ?: ""
            )
            .replace(
                "{players}",
                game?.players?.toString() ?: "0"
            )
            .replace(
                "{max_players}",
                game?.maxPlayers?.toString() ?: "0"
            )
            .replace(
                "{state}",
                game?.state ?: ""
            )
            .replace(
                "{server}",
                game?.serverId ?: ""
            )
            .replace(
                "{map}",
                game?.map ?: ""
            )
    }

    /*
     * Sign material
     */

    private fun isSignMaterial(
        material: Material
    ): Boolean {

        return material.name.endsWith(
            "_SIGN"
        ) || material.name.endsWith(
            "_WALL_SIGN"
        )
    }

    /*
     * Sign keys
     */

    private fun signKey(
        location: Location
    ): String {

        return signKey(
            world = location.world?.name ?: "unknown",
            x = location.blockX,
            y = location.blockY,
            z = location.blockZ
        )
    }

    private fun signKey(
        world: String,
        x: Int,
        y: Int,
        z: Int
    ): String {

        return "$world:$x:$y:$z"
    }

    private fun formatLocation(
        location: Location
    ): String {

        return signKey(
            location
        )
    }
}