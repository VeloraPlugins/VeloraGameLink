package online.veloraplugins.gamelink.paper.services

import online.veloraplugins.gamelink.api.game.GameInstance
import online.veloraplugins.gamelink.paper.VeloraGameLinkPlugin

class GameSelectorService(
    private val plugin: VeloraGameLinkPlugin,
    private val gameRedisService: GameRedisService
) {

    /*
     * Find best game
     */

    fun findBestGame(
        type: String
    ): GameInstance? {

        return getAvailableGames(type)
            .firstOrNull()
    }

    /*
     * Get available games
     */

    fun getAvailableGames(
        type: String
    ): List<GameInstance> {

        val displayConfig = plugin
            .gameDisplaysConfig
            .games[type]
            ?: return emptyList()

        return gameRedisService
            .getByType(type)
            .asSequence()
            .filter {
                isJoinable(it)
            }
            .sortedWith(
                compareByDescending<GameInstance> { game ->

                    displayConfig.states
                        .entries
                        .firstOrNull {
                            it.key.equals(
                                game.state,
                                ignoreCase = true
                            )
                        }
                        ?.value
                        ?.priority
                        ?: Int.MIN_VALUE

                }.thenByDescending {
                    it.players
                }
            )
            .toList()
    }

    /*
     * Find specific game
     */

    fun findGame(
        gameId: String
    ): GameInstance? {

        val game = gameRedisService.get(
            gameId
        ) ?: return null

        if (!isJoinable(game)) {
            return null
        }

        return game
    }

    /*
     * Joinable
     */

    fun isJoinable(
        game: GameInstance
    ): Boolean {

        if (game.isFull) {
            return false
        }

        val displayConfig = plugin
            .gameDisplaysConfig
            .games
            .entries
            .firstOrNull {
                it.key.equals(
                    game.type,
                    ignoreCase = true
                )
            }
            ?.value
            ?: return false

        val stateDisplay = displayConfig
            .states
            .entries
            .firstOrNull {
                it.key.equals(
                    game.state,
                    ignoreCase = true
                )
            }
            ?.value
            ?: return false

        return stateDisplay.allowJoin
    }

    /*
     * Refresh
     */

    fun refresh() {

        val games = gameRedisService.getAll()

        plugin.gameSignService.refreshAll()

        plugin.debug(
            "SELECTOR",
            "Refreshed ${games.size} network game instance(s)."
        )
    }

    /*
     * Reload
     */

    fun reload() {

        plugin.debug(
            "SELECTOR",
            "Reloaded game selector service."
        )
    }
}