package online.veloraplugins.gamelink.paper.services

import online.velora.framework.utils.condition.ConditionParser
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

        return gameRedisService
            .getByType(
                type
            )
            .asSequence()
            .filter {
                isJoinable(
                    it
                )
            }
            .sortedWith(
                compareByDescending<GameInstance> {
                    plugin
                        .gameConditionResolver
                        .getPriority(
                            it
                        )
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

        return plugin
            .gameConditionResolver
            .isJoinable(
                game
            )
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