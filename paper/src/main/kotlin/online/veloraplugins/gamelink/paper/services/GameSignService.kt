package online.veloraplugins.gamelink.paper.services

import online.velora.framework.adventure.ComponentUtil
import online.veloraplugins.gamelink.api.game.GameInstance
import online.veloraplugins.gamelink.paper.VeloraGameLinkPlugin
import online.veloraplugins.gamelink.paper.configurations.GameDisplaysConfig
import online.veloraplugins.gamelink.paper.configurations.GameSignsConfig
import online.veloraplugins.gamelink.paper.extensions.toLocation
import org.bukkit.Bukkit
import org.bukkit.DyeColor
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Sign
import org.bukkit.block.data.Directional
import org.bukkit.block.data.Rotatable
import org.bukkit.block.sign.Side
import java.util.concurrent.ConcurrentHashMap

class GameSignService(
    private val plugin: VeloraGameLinkPlugin,
    private val gameRedisService: GameRedisService,
    private val conditionResolver: GameConditionResolver
) {

    /*
     * Sign assignments
     *
     * sign location -> game id
     */

    private val assignments =
        ConcurrentHashMap<String, String>()

    /*
     * Render versions
     *
     * Prevents older scheduled renders from
     * overwriting a newer sign state.
     */

    private val renderVersions =
        ConcurrentHashMap<String, Long>()

    /*
     * Internal display
     *
     * ConditionDisplay and SignDisplay are separate
     * configuration classes, so they are normalized
     * before rendering.
     */

    private data class ResolvedDisplay(
        val condition: String?,
        val allowJoin: Boolean,
        val showState: Boolean,
        val priority: Int,
        val relativeMaterial: String,
        val signOptions: GameDisplaysConfig.SignOptions,
        val lines: List<String>
    )

    /*
     * Refresh all
     */

    fun refreshAll() {

        plugin.debug(
            "SIGN",
            "Starting full sign refresh. " +
                    "Configured signs=${plugin.gameSignsConfig.signs.size}, " +
                    "assignments=${assignments.size}."
        )

        val groupedSigns = plugin
            .gameSignsConfig
            .signs
            .groupBy {
                it.gameType.lowercase()
            }

        groupedSigns.forEach { (gameType, signs) ->

            plugin.debug(
                "SIGN",
                "Refreshing ${signs.size} sign(s) for game type '$gameType'."
            )

            refreshGameType(
                gameType = gameType,
                signs = signs
            )
        }

        cleanupRemovedSigns()

        plugin.debug(
            "SIGN",
            "Finished full sign refresh. " +
                    "Assignments=${assignments.size}."
        )
    }

    /*
     * Refresh game type
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

        plugin.debug(
            "SIGN",
            "Requested refresh for game type '$gameType'. " +
                    "Configured signs=${signs.size}."
        )

        refreshGameType(
            gameType = gameType,
            signs = signs
        )
    }

    /*
     * Refresh game type
     */

    private fun refreshGameType(
        gameType: String,
        signs: List<GameSignsConfig.GameSign>
    ) {

        if (signs.isEmpty()) {

            plugin.debug(
                "SIGN",
                "Skipping refresh for '$gameType': no signs configured."
            )

            return
        }

        val gameConfig =
            conditionResolver.findGameDisplayConfig(
                gameType
            )
                ?: run {

                    plugin.debug(
                        "SIGN",
                        "Unable to refresh '$gameType': " +
                                "no display configuration found."
                    )

                    return
                }

        val networkGames =
            gameRedisService.getByType(
                gameType
            )

        plugin.debug(
            "SIGN",
            "Found ${networkGames.size} network game(s) for '$gameType'."
        )

        /*
         * Determine which games may currently
         * occupy a sign.
         */

        val displayableGames = networkGames
            .asSequence()
            .filter {
                conditionResolver.isDisplayable(
                    it
                )
            }
            .sortedWith(
                compareByDescending<GameInstance> {
                    conditionResolver.getPriority(
                        it
                    )
                }.thenByDescending {
                    it.players
                }.thenBy {
                    it.id.lowercase()
                }
            )
            .toList()

        plugin.debug(
            "SIGN",
            "Displayable games for '$gameType': " +
                    if (displayableGames.isEmpty()) {
                        "none."
                    } else {
                        displayableGames.joinToString { game ->
                            "${game.id}" +
                                    "[state=${game.state}," +
                                    "players=${game.players}/${game.maxPlayers}," +
                                    "priority=${conditionResolver.getPriority(game)}]"
                        }
                    }
        )

        /*
         * Reserve existing valid assignments.
         *
         * One game may only occupy one sign.
         */

        val usedGameIds =
            mutableSetOf<String>()

        signs.forEach { signConfig ->

            val location =
                signConfig.location.toLocation()

            if (location == null) {

                plugin.debug(
                    "SIGN",
                    "Unable to resolve configured sign location " +
                            "'${signConfig.location.world}:" +
                            "${signConfig.location.x}," +
                            "${signConfig.location.y}," +
                            "${signConfig.location.z}'."
                )

                return@forEach
            }

            val key =
                signKey(
                    location
                )

            val gameId =
                assignments[key]
                    ?: return@forEach

            val game =
                gameRedisService.get(
                    gameId
                )
                    ?: run {

                        plugin.debug(
                            "SIGN",
                            "Existing assignment '$gameId' for '$key' " +
                                    "no longer exists in Redis."
                        )

                        assignments.remove(
                            key
                        )

                        return@forEach
                    }

            val valid =
                game.type.equals(
                    gameType,
                    ignoreCase = true
                ) &&
                        conditionResolver.isDisplayable(
                            game
                        ) &&
                        game.id !in usedGameIds

            if (!valid) {

                plugin.debug(
                    "SIGN",
                    "Existing assignment '${game.id}' on '$key' is invalid."
                )

                assignments.remove(
                    key
                )

                return@forEach
            }

            usedGameIds +=
                game.id

            plugin.debug(
                "SIGN",
                "Reserved existing assignment '${game.id}' for '$key'."
            )
        }

        /*
         * Refresh every configured sign.
         */

        signs.forEach { signConfig ->

            refreshSign(
                signConfig = signConfig,
                gameType = gameType,
                gameConfig = gameConfig,
                displayableGames = displayableGames,
                usedGameIds = usedGameIds
            )
        }
    }

    /*
     * Refresh sign
     */

    private fun refreshSign(
        signConfig: GameSignsConfig.GameSign,
        gameType: String,
        gameConfig: GameDisplaysConfig.GameTypeDisplay,
        displayableGames: List<GameInstance>,
        usedGameIds: MutableSet<String>
    ) {

        val location =
            signConfig.location.toLocation()
                ?: run {

                    plugin.debug(
                        "SIGN",
                        "Unable to resolve sign location for '$gameType'."
                    )

                    return
                }

        val key =
            signKey(
                location
            )

        val assignedGameId =
            assignments[key]

        /*
         * Keep current assignment if it is still valid.
         */

        if (assignedGameId != null) {

            val assignedGame =
                gameRedisService.get(
                    assignedGameId
                )

            if (
                assignedGame != null &&
                assignedGame.type.equals(
                    gameType,
                    ignoreCase = true
                ) &&
                conditionResolver.isDisplayable(
                    assignedGame
                )
            ) {

                usedGameIds +=
                    assignedGame.id

                plugin.debug(
                    "SIGN",
                    "Keeping game '${assignedGame.id}' assigned to '$key'."
                )

                updateSign(
                    location = location,
                    gameType = gameType,
                    game = assignedGame
                )

                return
            }

            assignments.remove(
                key
            )

            usedGameIds.remove(
                assignedGameId
            )

            plugin.debug(
                "SIGN",
                "Removed invalid assignment '$assignedGameId' from '$key'."
            )
        }

        /*
         * Find next available game.
         */

        val replacement =
            displayableGames.firstOrNull {
                it.id !in usedGameIds
            }

        if (replacement != null) {

            assignments[key] =
                replacement.id

            usedGameIds +=
                replacement.id

            val display =
                conditionResolver.findMatchingDisplay(
                    replacement
                )

            plugin.debug(
                "SIGN",
                "Assigned game '${replacement.id}' to '$key'. " +
                        "Condition='${display?.condition ?: "NONE"}', " +
                        "priority=${display?.priority}, " +
                        "relativeMaterial='${display?.relativeMaterial}'."
            )

            updateSign(
                location = location,
                gameType = gameType,
                game = replacement
            )

            return
        }

        /*
         * No game available.
         */

        assignments.remove(
            key
        )

        plugin.debug(
            "SIGN",
            "No game available for '$key'. Rendering searching display."
        )

        updateSearchingSign(
            location = location,
            gameType = gameType,
            gameConfig = gameConfig
        )
    }

    /*
     * Update game sign
     */

    fun updateSign(
        location: Location,
        gameType: String,
        game: GameInstance?
    ) {

        val key =
            signKey(
                location
            )

        val gameConfig =
            conditionResolver.findGameDisplayConfig(
                gameType
            )
                ?: run {

                    plugin.debug(
                        "SIGN",
                        "Unable to update '$key': " +
                                "display config '$gameType' not found."
                    )

                    return
                }

        /*
         * No game supplied.
         */

        if (game == null) {

            assignments.remove(
                key
            )

            updateSearchingSign(
                location = location,
                gameType = gameType,
                gameConfig = gameConfig
            )

            return
        }

        /*
         * Resolve first matching condition using the
         * central GameConditionResolver.
         */

        val conditionDisplay =
            conditionResolver.findMatchingDisplay(
                game
            )

        if (conditionDisplay == null) {

            assignments.remove(
                key
            )

            plugin.debug(
                "SIGN",
                "Game '${game.id}' matched no display condition. " +
                        "Switching '$key' to searching."
            )

            updateSearchingSign(
                location = location,
                gameType = gameType,
                gameConfig = gameConfig
            )

            return
        }

        /*
         * A matching condition can explicitly hide
         * the game from physical signs.
         */

        if (!conditionDisplay.showState) {

            assignments.remove(
                key
            )

            plugin.debug(
                "SIGN",
                "Game '${game.id}' matched " +
                        "'${conditionDisplay.condition}' with showState=false. " +
                        "Switching '$key' to searching."
            )

            updateSearchingSign(
                location = location,
                gameType = gameType,
                gameConfig = gameConfig
            )

            return
        }

        plugin.debug(
            "SIGN",
            "Rendering game '${game.id}' on '$key'. " +
                    "Condition='${conditionDisplay.condition}', " +
                    "state='${game.state}', " +
                    "players=${game.players}/${game.maxPlayers}, " +
                    "allowJoin=${conditionDisplay.allowJoin}, " +
                    "priority=${conditionDisplay.priority}, " +
                    "relativeMaterial='${conditionDisplay.relativeMaterial}'."
        )

        render(
            location = location,
            gameType = gameType,
            game = game,
            display = resolveDisplay(
                conditionDisplay
            )
        )
    }

    /*
     * Searching sign
     */

    private fun updateSearchingSign(
        location: Location,
        gameType: String,
        gameConfig: GameDisplaysConfig.GameTypeDisplay
    ) {

        val key =
            signKey(
                location
            )

        assignments.remove(
            key
        )

        val display =
            resolveDisplay(
                gameConfig.searchingForGames
            )

        plugin.debug(
            "SIGN",
            "Rendering searching display on '$key'. " +
                    "relativeMaterial='${display.relativeMaterial}'."
        )

        render(
            location = location,
            gameType = gameType,
            game = null,
            display = display
        )
    }

    /*
     * Render
     */

    private fun render(
        location: Location,
        gameType: String,
        game: GameInstance?,
        display: ResolvedDisplay
    ) {

        val key =
            signKey(
                location
            )

        /*
         * Increment render version.
         */

        val version =
            renderVersions.merge(
                key,
                1L,
                Long::plus
            ) ?: 1L

        plugin.debug(
            "SIGN",
            "Scheduling render version=$version for '$key'. " +
                    "game='${game?.id ?: "SEARCHING"}', " +
                    "condition='${display.condition ?: "SEARCHING"}', " +
                    "relativeMaterial='${display.relativeMaterial}'."
        )

        Bukkit.getRegionScheduler().execute(
            plugin,
            location
        ) {

            /*
             * Ignore outdated render task.
             */

            val latestVersion =
                renderVersions[key]

            if (latestVersion != version) {

                plugin.debug(
                    "SIGN",
                    "Skipping stale render version=$version for '$key'. " +
                            "Latest=$latestVersion."
                )

                return@execute
            }

            val block =
                location.block

            val sign =
                block.state as? Sign
                    ?: run {

                        plugin.debug(
                            "SIGN",
                            "Configured sign '$key' no longer exists. " +
                                    "Block='${block.type}'."
                        )

                        assignments.remove(
                            key
                        )

                        return@execute
                    }

            /*
             * Relative block
             *
             * Already running on the correct region thread.
             * Do not schedule another region task here.
             */

            val relativeLocation =
                getRelativeBlockLocation(
                    sign
                )

            if (relativeLocation != null) {

                applyRelativeMaterial(
                    location = relativeLocation,
                    display = display
                )
            }

            /*
             * Sign side
             */

            val sideType =
                getSide(
                    display
                )

            val side =
                sign.getSide(
                    sideType
                )

            /*
             * Lines
             */

            val lines = display
                .lines
                .take(
                    4
                )
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

            plugin.debug(
                "SIGN",
                "Rendering '$key' side='$sideType': " +
                        lines.mapIndexed { index, line ->
                            "[$index]='$line'"
                        }.joinToString()
            )

            lines.forEachIndexed { index, line ->

                side.line(
                    index,
                    ComponentUtil.parse(
                        line
                    )
                )
            }

            /*
             * Sign options
             */

            applySignOptions(
                sign = sign,
                display = display
            )

            val updated =
                sign.update(
                    true,
                    false
                )

            plugin.debug(
                "SIGN",
                "Updated sign '$key'. " +
                        "version=$version, update()=$updated."
            )
        }
    }

    /*
     * Relative block location
     */

    private fun getRelativeBlockLocation(
        sign: Sign
    ): Location? {

        val block =
            sign.block

        val blockData =
            block.blockData

        val face = when (blockData) {

            is Directional -> {

                plugin.debug(
                    "SIGN",
                    "Directional sign '${formatLocation(block.location)}': " +
                            "facing='${blockData.facing}', " +
                            "relative='${blockData.facing.oppositeFace}'."
                )

                blockData
                    .facing
                    .oppositeFace
            }

            is Rotatable -> {

                plugin.debug(
                    "SIGN",
                    "Rotatable sign '${formatLocation(block.location)}': " +
                            "rotation='${blockData.rotation}', " +
                            "relative='${blockData.rotation.oppositeFace}'."
                )

                blockData
                    .rotation
                    .oppositeFace
            }

            else -> {

                plugin.debug(
                    "SIGN",
                    "Unable to resolve relative block for " +
                            "'${formatLocation(block.location)}'. " +
                            "Unsupported BlockData='${blockData.javaClass.name}'."
                )

                return null
            }
        }

        return block
            .location
            .clone()
            .add(
                face.modX.toDouble(),
                face.modY.toDouble(),
                face.modZ.toDouble()
            )
    }

    /*
     * Relative material
     */

    private fun applyRelativeMaterial(
        location: Location,
        display: ResolvedDisplay
    ) {

        val material =
            Material.matchMaterial(
                display.relativeMaterial
            )
                ?: run {

                    plugin.debug(
                        "SIGN",
                        "Invalid relative material " +
                                "'${display.relativeMaterial}' at " +
                                "'${formatLocation(location)}'."
                    )

                    return
                }

        if (!material.isBlock) {

            plugin.debug(
                "SIGN",
                "Relative material '$material' is not a block."
            )

            return
        }

        val block =
            location.block

        val previous =
            block.type

        if (previous == material) {

            plugin.debug(
                "SIGN",
                "Relative block '${formatLocation(location)}' " +
                        "already uses '$material'."
            )

            return
        }

        block.setType(
            material,
            false
        )

        plugin.debug(
            "SIGN",
            "Updated relative block '${formatLocation(location)}': " +
                    "'$previous' -> '${block.type}'."
        )
    }

    /*
     * Resolve condition display
     */

    private fun resolveDisplay(
        display: GameDisplaysConfig.ConditionDisplay
    ): ResolvedDisplay {

        return ResolvedDisplay(
            condition = display.condition,
            allowJoin = display.allowJoin,
            showState = display.showState,
            priority = display.priority,
            relativeMaterial = display.relativeMaterial,
            signOptions = display.signOptions,
            lines = display.lines
        )
    }

    /*
     * Resolve searching display
     */

    private fun resolveDisplay(
        display: GameDisplaysConfig.SignDisplay
    ): ResolvedDisplay {

        return ResolvedDisplay(
            condition = null,
            allowJoin = display.allowJoin,
            showState = display.showState,
            priority = display.priority,
            relativeMaterial = display.relativeMaterial,
            signOptions = display.signOptions,
            lines = display.lines
        )
    }

    /*
     * Sign side
     */

    private fun getSide(
        display: ResolvedDisplay
    ): Side {

        return when (
            display.signOptions.side.uppercase()
        ) {

            "BACK" ->
                Side.BACK

            else ->
                Side.FRONT
        }
    }

    /*
     * Apply sign options
     */

    private fun applySignOptions(
        sign: Sign,
        display: ResolvedDisplay
    ) {

        val options =
            display.signOptions

        val sideType =
            getSide(
                display
            )

        val side =
            sign.getSide(
                sideType
            )

        plugin.debug(
            "SIGN",
            "Applying sign options at '${formatLocation(sign.location)}': " +
                    "side='$sideType', " +
                    "glow=${options.glow}, " +
                    "color='${options.color}', " +
                    "waxed=${options.waxed}."
        )

        /*
         * Glow
         */

        side.isGlowingText =
            options.glow

        /*
         * Dye color
         */

        val color =
            runCatching {

                DyeColor.valueOf(
                    options.color.uppercase()
                )

            }.getOrNull()

        if (color != null) {

            side.color =
                color

        } else {

            plugin.debug(
                "SIGN",
                "Invalid sign color '${options.color}' at " +
                        "'${formatLocation(sign.location)}'."
            )
        }

        /*
         * Wax
         */

        sign.isWaxed =
            options.waxed
    }

    /*
     * Assigned game id
     */

    fun getAssignedGameId(
        location: Location
    ): String? {

        val key =
            signKey(
                location
            )

        val gameId =
            assignments[key]

        plugin.debug(
            "SIGN",
            "Assignment lookup '$key' -> '${gameId ?: "NONE"}'."
        )

        return gameId
    }

    /*
     * Assigned game
     */

    fun getAssignedGame(
        location: Location
    ): GameInstance? {

        val key =
            signKey(
                location
            )

        val gameId =
            assignments[key]
                ?: return null

        val game =
            gameRedisService.get(
                gameId
            )

        if (game == null) {

            assignments.remove(
                key
            )

            plugin.debug(
                "SIGN",
                "Removed stale assignment '$gameId' from '$key'."
            )

            return null
        }

        return game
    }

    /*
     * Clear assignment
     */

    fun clearAssignment(
        location: Location
    ) {

        val key =
            signKey(
                location
            )

        val gameId =
            assignments.remove(
                key
            )

        /*
         * Incrementing is safer than simply deleting:
         * any already queued render for this sign will
         * become stale.
         */

        renderVersions.merge(
            key,
            1L,
            Long::plus
        )

        plugin.debug(
            "SIGN",
            "Cleared assignment '$key' -> '${gameId ?: "NONE"}'."
        )
    }

    /*
     * Clear assignments
     */

    fun clearAssignments() {

        val count =
            assignments.size

        /*
         * Invalidate existing queued renders before
         * clearing active assignments.
         */

        renderVersions.keys.forEach { key ->

            renderVersions.merge(
                key,
                1L,
                Long::plus
            )
        }

        assignments.clear()

        plugin.debug(
            "SIGN",
            "Cleared $count sign assignment(s)."
        )
    }

    /*
     * Cleanup removed signs
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

        val staleKeys = assignments
            .keys
            .filter {
                it !in configuredKeys
            }

        staleKeys.forEach { key ->

            val gameId =
                assignments.remove(
                    key
                )

            renderVersions.merge(
                key,
                1L,
                Long::plus
            )

            plugin.debug(
                "SIGN",
                "Removed stale sign assignment " +
                        "'$key' -> '${gameId ?: "NONE"}'."
            )
        }
    }

    /*
     * Sign placeholders
     */

    private fun replacePlaceholders(
        text: String,
        gameType: String,
        game: GameInstance?
    ): String {

        return text
            .replace(
                "{game}",
                game?.type
                    ?: gameType
            )
            .replace(
                "{game_id}",
                game?.id
                    ?: ""
            )
            .replace(
                "{players}",
                game?.players
                    ?.toString()
                    ?: "0"
            )
            .replace(
                "{max_players}",
                game?.maxPlayers
                    ?.toString()
                    ?: "0"
            )
            .replace(
                "{state}",
                game?.state
                    ?: ""
            )
            .replace(
                "{server}",
                game?.serverId
                    ?: ""
            )
            .replace(
                "{map}",
                game?.map
                    ?: ""
            )
    }

    /*
     * Sign key
     */

    private fun signKey(
        location: Location
    ): String {

        return signKey(
            world = location.world.name,
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

    /*
     * Format location
     */

    private fun formatLocation(
        location: Location
    ): String {

        return signKey(
            location
        )
    }
}