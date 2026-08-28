package online.veloraplugins.gamelink.paper.services

import online.velora.framework.utils.condition.ConditionParser
import online.veloraplugins.gamelink.api.game.GameInstance
import online.veloraplugins.gamelink.paper.VeloraGameLinkPlugin
import online.veloraplugins.gamelink.paper.configurations.GameDisplaysConfig

class GameConditionResolver(
    private val plugin: VeloraGameLinkPlugin
) {

    /*
     * Find game type display config
     */

    fun findGameDisplayConfig(
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
            "CONDITION",
            "Game display config lookup for '$gameType': " +
                if (entry == null) {
                    "NOT FOUND. Available=" +
                        "${plugin.gameDisplaysConfig.games.keys.joinToString()}."
                } else {
                    "FOUND as key='${entry.key}'."
                }
        )

        return entry
            ?.value
    }

    /*
     * Find matching display
     *
     * Conditions are evaluated from top to bottom.
     * The first matching condition wins.
     */

    fun findMatchingDisplay(
        game: GameInstance
    ): GameDisplaysConfig.ConditionDisplay? {

        val gameConfig =
            findGameDisplayConfig(
                game.type
            )
                ?: return null

        return findMatchingDisplay(
            gameConfig = gameConfig,
            game = game
        )
    }

    /*
     * Find matching display
     */

    fun findMatchingDisplay(
        gameConfig: GameDisplaysConfig.GameTypeDisplay,
        game: GameInstance
    ): GameDisplaysConfig.ConditionDisplay? {

        plugin.debug(
            "CONDITION",
            "Evaluating ${gameConfig.conditions.size} condition(s) " +
                "for game '${game.id}' " +
                "(type='${game.type}', " +
                "state='${game.state}', " +
                "players=${game.players}/${game.maxPlayers})."
        )

        gameConfig.conditions.forEachIndexed { index, display ->

            val configuredExpression =
                display.condition

            if (configuredExpression.isBlank()) {

                plugin.debug(
                    "CONDITION",
                    "Skipping blank condition #${index + 1} " +
                        "for game '${game.id}'."
                )

                return@forEachIndexed
            }

            val expression =
                replacePlaceholders(
                    expression = configuredExpression,
                    game = game
                )

            plugin.debug(
                "CONDITION",
                "Evaluating condition #${index + 1} " +
                    "for game '${game.id}': " +
                    "'$configuredExpression' -> '$expression'."
            )

            val matches =
                runCatching {

                    ConditionParser.evaluate(
                        expression
                    )

                }.onFailure { throwable ->

                    plugin.debug(
                        "CONDITION",
                        "Unable to evaluate condition " +
                            "'$configuredExpression' " +
                            "for game '${game.id}'.",
                        throwable
                    )

                }.getOrDefault(
                    false
                )

            plugin.debug(
                "CONDITION",
                "Condition #${index + 1} " +
                    "for game '${game.id}' result=$matches."
            )

            if (!matches) {
                return@forEachIndexed
            }

            plugin.debug(
                "CONDITION",
                "Game '${game.id}' matched condition " +
                    "#${index + 1} '$configuredExpression': " +
                    "allowJoin=${display.allowJoin}, " +
                    "showState=${display.showState}, " +
                    "priority=${display.priority}, " +
                    "relativeMaterial='${display.relativeMaterial}'."
            )

            return display
        }

        plugin.debug(
            "CONDITION",
            "Game '${game.id}' matched no configured condition."
        )

        return null
    }

    /*
     * Check condition directly
     */

    fun matches(
        condition: String,
        game: GameInstance
    ): Boolean {

        if (condition.isBlank()) {
            return false
        }

        val expression =
            replacePlaceholders(
                expression = condition,
                game = game
            )

        return runCatching {

            ConditionParser.evaluate(
                expression
            )

        }.onFailure { throwable ->

            plugin.debug(
                "CONDITION",
                "Unable to evaluate condition '$condition' " +
                    "for game '${game.id}'.",
                throwable
            )

        }.getOrDefault(
            false
        )
    }

    /*
     * Replace condition placeholders
     */

    fun replacePlaceholders(
        expression: String,
        game: GameInstance
    ): String {

        val replaced = expression
            .replace(
                "{game}",
                game.type
            )
            .replace(
                "{game_id}",
                game.id
            )
            .replace(
                "{state}",
                game.state
            )
            .replace(
                "{players}",
                game.players.toString()
            )
            .replace(
                "{max_players}",
                game.maxPlayers.toString()
            )
            .replace(
                "{server}",
                game.serverId
            )
            .replace(
                "{map}",
                game.map
                    ?: ""
            )

        plugin.debug(
            "CONDITION",
            "Replaced placeholders for game '${game.id}': " +
                "'$expression' -> '$replaced'."
        )

        return replaced
    }

    /*
     * Joinable
     */

    fun isJoinable(
        game: GameInstance
    ): Boolean {

        val display =
            findMatchingDisplay(
                game
            )
                ?: return false

        return display.allowJoin
    }

    /*
     * Displayable
     */

    fun isDisplayable(
        game: GameInstance
    ): Boolean {

        val display =
            findMatchingDisplay(
                game
            )
                ?: return false

        return display.showState
    }

    /*
     * Priority
     */

    fun getPriority(
        game: GameInstance
    ): Int {

        return findMatchingDisplay(
            game
        )
            ?.priority
            ?: Int.MIN_VALUE
    }
}