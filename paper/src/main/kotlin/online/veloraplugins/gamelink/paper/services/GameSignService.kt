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
    private val gameRedisService: GameRedisService
) {

    /*
     * Sign assignments
     */

    private val assignments =
        ConcurrentHashMap<String, String>()

    /*
     * Refresh all
     */

    fun refreshAll() {

        plugin.debug(
            "SIGN",
            "Starting full sign refresh. " +
                    "Configured signs=${plugin.gameSignsConfig.signs.size}, " +
                    "current assignments=${assignments.size}."
        )

        val groupedSigns = plugin
            .gameSignsConfig
            .signs
            .groupBy {
                it.gameType.lowercase()
            }

        plugin.debug(
            "SIGN",
            "Grouped signs into ${groupedSigns.size} game type(s): " +
                    groupedSigns.entries.joinToString {
                        "${it.key}=${it.value.size}"
                    }
        )

        groupedSigns.forEach { (gameType, signs) ->

            plugin.debug(
                "SIGN",
                "Refreshing game type '$gameType' with ${signs.size} sign(s)."
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
                    "Configured signs=${plugin.gameSignsConfig.signs.size}, " +
                    "active assignments=${assignments.size}."
        )
    }

    /*
     * Refresh game type
     */

    fun refreshGameType(
        gameType: String
    ) {

        plugin.debug(
            "SIGN",
            "Requested sign refresh for game type '$gameType'."
        )

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
            "Found ${signs.size} configured sign(s) for '$gameType'."
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
                "Skipping '$gameType' refresh because no signs are configured."
            )

            return
        }

        val gameConfig = findGameDisplayConfig(
            gameType
        ) ?: run {

            plugin.debug(
                "SIGN",
                "Unable to refresh signs for '$gameType': " +
                        "display configuration not found."
            )

            return
        }

        plugin.debug(
            "SIGN",
            "Resolved display config for '$gameType'. " +
                    "Configured states=${gameConfig.states.keys.joinToString()}."
        )

        val networkGames =
            gameRedisService.getByType(
                gameType
            )

        plugin.debug(
            "SIGN",
            "Redis returned ${networkGames.size} game(s) for '$gameType'."
        )

        networkGames.forEach { game ->

            val stateDisplay = findStateDisplay(
                gameConfig = gameConfig,
                state = game.state
            )

            plugin.debug(
                "SIGN",
                "Game '${game.id}': " +
                        "type='${game.type}', " +
                        "state='${game.state}', " +
                        "players=${game.players}/${game.maxPlayers}, " +
                        "stateConfig=${stateDisplay != null}, " +
                        "showState=${stateDisplay?.showState}, " +
                        "allowJoin=${stateDisplay?.allowJoin}, " +
                        "priority=${stateDisplay?.priority}, " +
                        "relativeMaterial='${stateDisplay?.relativeMaterial}'."
            )
        }

        val displayableGames = networkGames
            .asSequence()
            .filter { game ->

                val displayable = isDisplayable(
                    game = game,
                    gameConfig = gameConfig
                )

                plugin.debug(
                    "SIGN",
                    "Displayability check for game '${game.id}' " +
                            "state='${game.state}': $displayable."
                )

                displayable
            }
            .sortedWith(
                compareByDescending<GameInstance> {
                    getPriority(
                        game = it,
                        gameConfig = gameConfig
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
                        displayableGames.joinToString {
                            "${it.id}[state=${it.state},priority=${getPriority(it, gameConfig)},players=${it.players}]"
                        }
                    }
        )

        val usedGameIds =
            mutableSetOf<String>()

        /*
         * Reserve valid existing assignments
         */

        signs.forEach { signConfig ->

            val location = signConfig
                .location
                .toLocation()

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

            if (gameId == null) {

                plugin.debug(
                    "SIGN",
                    "Sign '$key' currently has no assignment."
                )

                return@forEach
            }

            plugin.debug(
                "SIGN",
                "Sign '$key' currently assigned to '$gameId'."
            )

            val game =
                gameRedisService.get(
                    gameId
                )

            if (game == null) {

                plugin.debug(
                    "SIGN",
                    "Assigned game '$gameId' for sign '$key' " +
                            "no longer exists in Redis."
                )

                return@forEach
            }

            val displayable = isDisplayable(
                game = game,
                gameConfig = gameConfig
            )

            plugin.debug(
                "SIGN",
                "Existing assignment '$gameId' on '$key': " +
                        "typeMatch=${game.type.equals(gameType, true)}, " +
                        "displayable=$displayable, " +
                        "alreadyUsed=${game.id in usedGameIds}."
            )

            if (
                game.type.equals(
                    gameType,
                    ignoreCase = true
                ) &&
                displayable &&
                game.id !in usedGameIds
            ) {

                usedGameIds +=
                    game.id

                plugin.debug(
                    "SIGN",
                    "Reserved existing game '${game.id}' for sign '$key'."
                )
            }
        }

        plugin.debug(
            "SIGN",
            "Reserved game ids for '$gameType': " +
                    if (usedGameIds.isEmpty()) {
                        "none."
                    } else {
                        usedGameIds.joinToString()
                    }
        )

        /*
         * Refresh signs
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

        val key =
            signKey(
                location
            )

        plugin.debug(
            "SIGN",
            "Refreshing sign '$key' for type '$gameType'."
        )

        val assignedGameId =
            assignments[key]

        plugin.debug(
            "SIGN",
            "Sign '$key' existing assignment=" +
                    "'${assignedGameId ?: "NONE"}'."
        )

        if (assignedGameId != null) {

            val assignedGame =
                gameRedisService.get(
                    assignedGameId
                )

            plugin.debug(
                "SIGN",
                "Fetched existing assigned game '$assignedGameId': " +
                        if (assignedGame == null) {
                            "not found."
                        } else {
                            "state='${assignedGame.state}', " +
                                    "type='${assignedGame.type}', " +
                                    "players=${assignedGame.players}/${assignedGame.maxPlayers}."
                        }
            )

            if (
                assignedGame != null &&
                assignedGame.type.equals(
                    gameType,
                    ignoreCase = true
                ) &&
                isDisplayable(
                    game = assignedGame,
                    gameConfig = gameConfig
                )
            ) {

                usedGameIds +=
                    assignedGame.id

                plugin.debug(
                    "SIGN",
                    "Keeping assignment '${assignedGame.id}' on sign '$key'."
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
                "Unallocated game '$assignedGameId' from sign '$key'."
            )
        }

        plugin.debug(
            "SIGN",
            "Searching replacement for '$key'. " +
                    "Used games=${usedGameIds.joinToString()}, " +
                    "candidates=${displayableGames.joinToString { it.id }}."
        )

        val replacement = displayableGames
            .firstOrNull {
                it.id !in usedGameIds
            }

        if (replacement != null) {

            assignments[key] =
                replacement.id

            usedGameIds +=
                replacement.id

            val display = findStateDisplay(
                gameConfig = gameConfig,
                state = replacement.state
            )

            plugin.debug(
                "SIGN",
                "Assigned replacement game '${replacement.id}' " +
                        "state='${replacement.state}', " +
                        "relativeMaterial='${display?.relativeMaterial}' " +
                        "to sign '$key'."
            )

            updateSign(
                location = location,
                gameType = gameType,
                game = replacement
            )

            return
        }

        assignments.remove(
            key
        )

        plugin.debug(
            "SIGN",
            "No replacement game available for '$key'. " +
                    "Using searching display with relativeMaterial=" +
                    "'${gameConfig.searchingForGames.relativeMaterial}'."
        )

        updateSearchingSign(
            location = location,
            gameType = gameType,
            gameConfig = gameConfig
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

        val key =
            signKey(
                location
            )

        plugin.debug(
            "SIGN",
            "updateSign called for '$key': " +
                    "gameType='$gameType', " +
                    "game='${game?.id ?: "NONE"}', " +
                    "state='${game?.state ?: "NONE"}'."
        )

        val gameConfig = findGameDisplayConfig(
            gameType
        ) ?: run {

            plugin.debug(
                "SIGN",
                "Unable to update sign '$key': " +
                        "display configuration for '$gameType' not found."
            )

            return
        }

        if (game == null) {

            plugin.debug(
                "SIGN",
                "No game supplied for '$key'; switching to searching display."
            )

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

        val stateDisplay = findStateDisplay(
            gameConfig = gameConfig,
            state = game.state
        )

        plugin.debug(
            "SIGN",
            "State lookup for game '${game.id}', " +
                    "state='${game.state}': " +
                    if (stateDisplay == null) {
                        "NOT FOUND."
                    } else {
                        "showState=${stateDisplay.showState}, " +
                                "allowJoin=${stateDisplay.allowJoin}, " +
                                "priority=${stateDisplay.priority}, " +
                                "relativeMaterial='${stateDisplay.relativeMaterial}'."
                    }
        )

        if (stateDisplay == null) {

            assignments.remove(
                key
            )

            plugin.debug(
                "SIGN",
                "Game '${game.id}' entered unconfigured " +
                        "state '${game.state}'. " +
                        "Sign '$key' unallocated."
            )

            updateSearchingSign(
                location = location,
                gameType = gameType,
                gameConfig = gameConfig
            )

            return
        }

        if (!stateDisplay.showState) {

            assignments.remove(
                key
            )

            plugin.debug(
                "SIGN",
                "Game '${game.id}' entered state " +
                        "'${game.state}' with showState=false. " +
                        "Sign '$key' unallocated; searching display will be rendered."
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
            "Rendering active game '${game.id}' on '$key' " +
                    "with state='${game.state}' and " +
                    "relativeMaterial='${stateDisplay.relativeMaterial}'."
        )

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

        val key =
            signKey(
                location
            )

        assignments.remove(
            key
        )

        plugin.debug(
            "SIGN",
            "Rendering searching display on '$key': " +
                    "relativeMaterial='${gameConfig.searchingForGames.relativeMaterial}'."
        )

        render(
            location = location,
            gameType = gameType,
            game = null,
            display = gameConfig.searchingForGames
        )
    }

    /*
     * Render
     */

    private fun render(
        location: Location,
        gameType: String,
        game: GameInstance?,
        display: GameDisplaysConfig.SignDisplay
    ) {

        val key =
            signKey(
                location
            )

        plugin.debug(
            "SIGN",
            "Scheduling render for '$key': " +
                    "game='${game?.id ?: "SEARCHING"}', " +
                    "state='${game?.state ?: "SEARCHING"}', " +
                    "relativeMaterial='${display.relativeMaterial}'."
        )

        Bukkit.getRegionScheduler().execute(
            plugin,
            location
        ) {

            plugin.debug(
                "SIGN",
                "Executing render for '$key' on region thread."
            )

            val block =
                location.block

            plugin.debug(
                "SIGN",
                "Block at '$key' is '${block.type}' with " +
                        "blockData='${block.blockData.javaClass.simpleName}'."
            )

            val sign = block.state as? Sign
                ?: run {

                    plugin.debug(
                        "SIGN",
                        "Configured GameLink sign at '$key' no longer exists. " +
                                "Block type='${block.type}'."
                    )

                    assignments.remove(
                        key
                    )

                    return@execute
                }

            plugin.debug(
                "SIGN",
                "Resolved sign block at '$key'. " +
                        "Waxed=${sign.isWaxed}."
            )

            val relativeLocation =
                getRelativeBlockLocation(
                    sign = sign
                )

            if (relativeLocation != null) {

                plugin.debug(
                    "SIGN",
                    "Relative block for '$key' resolved to " +
                            "'${formatLocation(relativeLocation)}'. " +
                            "Requested material='${display.relativeMaterial}'."
                )

                applyRelativeMaterial(
                    location = relativeLocation,
                    display = display
                )

            } else {

                plugin.debug(
                    "SIGN",
                    "No relative block location could be determined for '$key'."
                )
            }

            val side =
                sign.getSide(
                    Side.FRONT
                )

            val lines = display
                .lines
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

            plugin.debug(
                "SIGN",
                "Rendering lines for '$key': " +
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

            applySignOptions(
                sign = sign,
                display = display
            )

            val updated = sign.update(
                true,
                false
            )

            plugin.debug(
                "SIGN",
                "Sign update completed for '$key'. update()=$updated."
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

        plugin.debug(
            "SIGN",
            "Resolving relative block for '${formatLocation(block.location)}'. " +
                    "BlockData='${blockData.javaClass.name}'."
        )

        val face = when (blockData) {

            is Directional -> {

                plugin.debug(
                    "SIGN",
                    "Sign uses Directional data. " +
                            "facing='${blockData.facing}', " +
                            "relativeFace='${blockData.facing.oppositeFace}'."
                )

                blockData.facing.oppositeFace
            }

            is Rotatable -> {

                plugin.debug(
                    "SIGN",
                    "Sign uses Rotatable data. " +
                            "rotation='${blockData.rotation}', " +
                            "relativeFace='${blockData.rotation.oppositeFace}'."
                )

                blockData.rotation.oppositeFace
            }

            else -> {

                plugin.debug(
                    "SIGN",
                    "Unable to determine relative block for " +
                            "'${formatLocation(block.location)}': " +
                            "unsupported BlockData '${blockData.javaClass.name}'."
                )

                return null
            }
        }

        val relativeLocation = block.location
            .clone()
            .add(
                face.modX.toDouble(),
                face.modY.toDouble(),
                face.modZ.toDouble()
            )

        plugin.debug(
            "SIGN",
            "Calculated relative block from " +
                    "'${formatLocation(block.location)}' to " +
                    "'${formatLocation(relativeLocation)}' using face '$face'."
        )

        return relativeLocation
    }

    /*
     * Apply relative material
     */

    private fun applyRelativeMaterial(
        location: Location,
        display: GameDisplaysConfig.SignDisplay
    ) {

        plugin.debug(
            "SIGN",
            "Resolving relative material '${display.relativeMaterial}' " +
                    "for '${formatLocation(location)}'."
        )

        val material = Material.matchMaterial(
            display.relativeMaterial
        ) ?: run {

            plugin.debug(
                "SIGN",
                "Invalid relative material '${display.relativeMaterial}' " +
                        "for '${formatLocation(location)}'."
            )

            return
        }

        if (!material.isBlock) {

            plugin.debug(
                "SIGN",
                "Relative material '$material' is not a block. " +
                        "Location='${formatLocation(location)}'."
            )

            return
        }

        plugin.debug(
            "SIGN",
            "Resolved relative material '${display.relativeMaterial}' " +
                    "to Bukkit material '$material'."
        )

        Bukkit.getRegionScheduler().execute(
            plugin,
            location
        ) {

            val block =
                location.block

            val previous =
                block.type

            plugin.debug(
                "SIGN",
                "Relative block '${formatLocation(location)}' " +
                        "currently='$previous', requested='$material'."
            )

            if (previous == material) {

                plugin.debug(
                    "SIGN",
                    "Relative block '${formatLocation(location)}' " +
                            "already uses '$material'; no update required."
                )

                return@execute
            }

            block.setType(
                material,
                false
            )

            plugin.debug(
                "SIGN",
                "Updated relative block '${formatLocation(location)}' " +
                        "from '$previous' to '${block.type}'."
            )
        }
    }

    /*
     * Displayable
     */

    fun isDisplayable(
        game: GameInstance
    ): Boolean {

        plugin.debug(
            "SIGN",
            "Checking displayability for game '${game.id}', " +
                    "type='${game.type}', state='${game.state}'."
        )

        val gameConfig = findGameDisplayConfig(
            game.type
        ) ?: run {

            plugin.debug(
                "SIGN",
                "Game '${game.id}' is not displayable: " +
                        "game type '${game.type}' has no display configuration."
            )

            return false
        }

        val result = isDisplayable(
            game = game,
            gameConfig = gameConfig
        )

        plugin.debug(
            "SIGN",
            "Displayability result for game '${game.id}': $result."
        )

        return result
    }

    private fun isDisplayable(
        game: GameInstance,
        gameConfig: GameDisplaysConfig.GameTypeDisplay
    ): Boolean {

        val display = findStateDisplay(
            gameConfig = gameConfig,
            state = game.state
        ) ?: run {

            plugin.debug(
                "SIGN",
                "Game '${game.id}' is not displayable: " +
                        "state '${game.state}' has no SignDisplay."
            )

            return false
        }

        plugin.debug(
            "SIGN",
            "Game '${game.id}' state '${game.state}' display config: " +
                    "showState=${display.showState}, " +
                    "allowJoin=${display.allowJoin}, " +
                    "priority=${display.priority}, " +
                    "relativeMaterial='${display.relativeMaterial}'."
        )

        return display.showState
    }

    /*
     * Priority
     */

    private fun getPriority(
        game: GameInstance,
        gameConfig: GameDisplaysConfig.GameTypeDisplay
    ): Int {

        val display = findStateDisplay(
            gameConfig = gameConfig,
            state = game.state
        )

        val priority =
            display?.priority
                ?: Int.MIN_VALUE

        plugin.debug(
            "SIGN",
            "Priority for game '${game.id}' state='${game.state}' " +
                    "resolved to $priority."
        )

        return priority
    }

    /*
     * Game display configuration
     */

    private fun findGameDisplayConfig(
        gameType: String
    ): GameDisplaysConfig.GameTypeDisplay? {

        val entry = plugin
            .gameDisplaysConfig
            .games
            .entries
            .firstOrNull {
                it.key.equals(
                    gameType,
                    ignoreCase = true
                )
            }

        plugin.debug(
            "SIGN",
            "Game display config lookup for '$gameType': " +
                    if (entry == null) {
                        "NOT FOUND. Available=${plugin.gameDisplaysConfig.games.keys.joinToString()}."
                    } else {
                        "FOUND as key='${entry.key}'."
                    }
        )

        return entry
            ?.value
    }

    /*
     * State display configuration
     */

    private fun findStateDisplay(
        gameConfig: GameDisplaysConfig.GameTypeDisplay,
        state: String
    ): GameDisplaysConfig.SignDisplay? {

        val entry = gameConfig
            .states
            .entries
            .firstOrNull {
                it.key.equals(
                    state,
                    ignoreCase = true
                )
            }

        plugin.debug(
            "SIGN",
            "State display lookup for '$state': " +
                    if (entry == null) {
                        "NOT FOUND. Available=${gameConfig.states.keys.joinToString()}."
                    } else {
                        "FOUND as '${entry.key}' " +
                                "[showState=${entry.value.showState}, " +
                                "allowJoin=${entry.value.allowJoin}, " +
                                "priority=${entry.value.priority}, " +
                                "relativeMaterial='${entry.value.relativeMaterial}']."
                    }
        )

        return entry
            ?.value
    }

    /*
     * Get assigned game id
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
            "Assignment lookup for '$key': '${gameId ?: "NONE"}'."
        )

        return gameId
    }

    /*
     * Get assigned game
     */

    fun getAssignedGame(
        location: Location
    ): GameInstance? {

        val key =
            signKey(
                location
            )

        val gameId = assignments[
            key
        ] ?: run {

            plugin.debug(
                "SIGN",
                "No assigned game for sign '$key'."
            )

            return null
        }

        plugin.debug(
            "SIGN",
            "Fetching assigned game '$gameId' for sign '$key'."
        )

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
                "Removed stale assignment '$gameId' from sign '$key'."
            )

            return null
        }

        plugin.debug(
            "SIGN",
            "Assigned game '$gameId' found for '$key': " +
                    "state='${game.state}', " +
                    "players=${game.players}/${game.maxPlayers}."
        )

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

        val removed =
            assignments.remove(
                key
            )

        plugin.debug(
            "SIGN",
            if (removed != null) {
                "Cleared game '$removed' from sign '$key'."
            } else {
                "No assignment existed for sign '$key' to clear."
            }
        )
    }

    /*
     * Clear assignments
     */

    fun clearAssignments() {

        val amount =
            assignments.size

        plugin.debug(
            "SIGN",
            "Clearing all sign assignments. Current count=$amount."
        )

        assignments.clear()

        plugin.debug(
            "SIGN",
            "Cleared $amount sign assignment(s)."
        )
    }

    /*
     * Cleanup removed signs
     */

    private fun cleanupRemovedSigns() {

        plugin.debug(
            "SIGN",
            "Checking ${assignments.size} assignment(s) for removed sign configs."
        )

        val configuredKeys = plugin
            .gameSignsConfig
            .signs
            .mapNotNull {

                val location =
                    it.location.toLocation()

                if (location == null) {

                    plugin.debug(
                        "SIGN",
                        "Unable to resolve configured sign while cleaning: " +
                                "'${it.location.world}:" +
                                "${it.location.x}," +
                                "${it.location.y}," +
                                "${it.location.z}'."
                    )
                }

                location
                    ?.let { resolved ->
                        signKey(
                            resolved
                        )
                    }
            }
            .toSet()

        plugin.debug(
            "SIGN",
            "Configured sign keys: ${configuredKeys.joinToString()}."
        )

        val staleKeys = assignments
            .keys
            .filter {
                it !in configuredKeys
            }

        staleKeys.forEach {

            val removed =
                assignments.remove(
                    it
                )

            plugin.debug(
                "SIGN",
                "Removed stale sign assignment '$it' -> '$removed'."
            )
        }

        if (staleKeys.isEmpty()) {

            plugin.debug(
                "SIGN",
                "No stale sign assignments found."
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

        val replaced = text
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

        plugin.debug(
            "SIGN",
            "Replaced sign placeholders: '$text' -> '$replaced' " +
                    "for game='${game?.id ?: "SEARCHING"}'."
        )

        return replaced
    }

    /*
     * Apply sign options
     *
     * Applies all configured physical sign options
     * to the selected sign side.
     */
    private fun applySignOptions(
        sign: Sign,
        display: GameDisplaysConfig.SignDisplay
    ) {

        val options =
            display.signOptions

        /*
         * Resolve side.
         */

        val sideType = when (
            options.side.uppercase()
        ) {

            "BACK" -> Side.BACK

            else -> Side.FRONT
        }

        val side =
            sign.getSide(
                sideType
            )

        plugin.debug(
            "SIGN",
            "Applying sign options to " +
                    "'${formatLocation(sign.location)}': " +
                    "side='$sideType', " +
                    "glow=${options.glow}, " +
                    "color='${options.color}', " +
                    "waxed=${options.waxed}."
        )

        /*
         * Glow
         */

        side.isGlowingText = options.glow

        /*
         * Dye color
         */

        val color = runCatching {
            DyeColor.valueOf(
                options.color.uppercase()
            )
        }.getOrNull()

        if (color != null) {

            side.color =
                color

            plugin.debug(
                "SIGN",
                "Applied sign color '$color' to " +
                        "'${formatLocation(sign.location)}'."
            )

        } else {

            plugin.debug(
                "SIGN",
                "Invalid sign color '${options.color}' at " +
                        "'${formatLocation(sign.location)}'."
            )
        }

        /*
         * Waxed
         */

        sign.isWaxed =
            options.waxed
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