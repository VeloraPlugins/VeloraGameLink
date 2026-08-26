package online.veloraplugins.gamelink.paper.placeholder

import online.veloraplugins.engine.hooks.papi.PlaceholderAPIHook
import online.veloraplugins.gamelink.paper.VeloraGameLinkPlugin
import java.util.UUID

object PlaceholderRegistry {

    /*
     * Register all GameLink placeholders
     */

    fun register(
        plugin: VeloraGameLinkPlugin,
        hook: PlaceholderAPIHook
    ) {

        registerGamePlaceholders(
            plugin = plugin,
            hook = hook
        )

        registerJoinPlaceholders(
            plugin = plugin,
            hook = hook
        )
    }

    /*
     * Game placeholders
     */

    private fun registerGamePlaceholders(
        plugin: VeloraGameLinkPlugin,
        hook: PlaceholderAPIHook
    ) {

        /*
         * Total games
         *
         * %gamelink_games%
         */

        hook.register(
            "games"
        ) {

            plugin.gameRedisService
                .getAll()
                .size
                .toString()
        }

        /*
         * Games by type
         *
         * %gamelink_games_bedwars%
         */

        hook.registerWithArgs(
            "games"
        ) { _, args ->

            val gameType = args
                .firstOrNull()
                ?: return@registerWithArgs "0"

            plugin.gameRedisService
                .getByType(
                    gameType
                )
                .size
                .toString()
        }

        /*
         * Available games by type
         *
         * %gamelink_available_bedwars%
         */

        hook.registerWithArgs(
            "available"
        ) { _, args ->

            val gameType = args
                .firstOrNull()
                ?: return@registerWithArgs "0"

            plugin.gameSelectorService
                .getAvailableGames(
                    gameType
                )
                .size
                .toString()
        }

        /*
         * Best game id
         *
         * %gamelink_best_bedwars%
         */

        hook.registerWithArgs(
            "best"
        ) { _, args ->

            val gameType = args
                .firstOrNull()
                ?: return@registerWithArgs ""

            plugin.gameSelectorService
                .findBestGame(
                    gameType
                )
                ?.id
                ?: ""
        }

        /*
         * Best game players
         *
         * %gamelink_best_players_bedwars%
         */

        hook.registerWithArgs(
            "best_players"
        ) { _, args ->

            val gameType = args
                .firstOrNull()
                ?: return@registerWithArgs "0"

            plugin.gameSelectorService
                .findBestGame(
                    gameType
                )
                ?.players
                ?.toString()
                ?: "0"
        }

        /*
         * Best game max players
         *
         * %gamelink_best_max_players_bedwars%
         */

        hook.registerWithArgs(
            "best_max_players"
        ) { _, args ->

            val gameType = args
                .firstOrNull()
                ?: return@registerWithArgs "0"

            plugin.gameSelectorService
                .findBestGame(
                    gameType
                )
                ?.maxPlayers
                ?.toString()
                ?: "0"
        }

        /*
         * Best game state
         *
         * %gamelink_best_state_bedwars%
         */

        hook.registerWithArgs(
            "best_state"
        ) { _, args ->

            val gameType = args
                .firstOrNull()
                ?: return@registerWithArgs ""

            plugin.gameSelectorService
                .findBestGame(
                    gameType
                )
                ?.state
                ?: ""
        }

        /*
         * Best game map
         *
         * %gamelink_best_map_bedwars%
         */

        hook.registerWithArgs(
            "best_map"
        ) { _, args ->

            val gameType = args
                .firstOrNull()
                ?: return@registerWithArgs ""

            plugin.gameSelectorService
                .findBestGame(
                    gameType
                )
                ?.map
                ?: ""
        }

        /*
         * Best game server
         *
         * %gamelink_best_server_bedwars%
         */

        hook.registerWithArgs(
            "best_server"
        ) { _, args ->

            val gameType = args
                .firstOrNull()
                ?: return@registerWithArgs ""

            plugin.gameSelectorService
                .findBestGame(
                    gameType
                )
                ?.serverId
                ?: ""
        }
    }

    /*
     * Join placeholders
     */

    private fun registerJoinPlaceholders(
        plugin: VeloraGameLinkPlugin,
        hook: PlaceholderAPIHook
    ) {

        /*
         * Player join intent
         *
         * %gamelink_join_intent%
         */

        hook.register(
            "join_intent"
        ) { player ->

            plugin.gameRedisService
                .getJoinIntent(
                    player.uniqueId
                )
                ?: ""
        }

        /*
         * Player has valid join intent
         *
         * %gamelink_has_join_intent%
         */

        hook.register(
            "has_join_intent"
        ) { player ->

            plugin.gameRedisService
                .hasValidJoinIntent(
                    player.uniqueId
                )
                .toString()
        }

        /*
         * Player join intent TTL
         *
         * %gamelink_join_intent_ttl%
         */

        hook.register(
            "join_intent_ttl"
        ) { player ->

            plugin.gameRedisService
                .getJoinIntentTtl(
                    player.uniqueId
                )
                .toString()
        }

        /*
 * Join intent by player UUID
 *
 * %gamelink_intent_<playerUUID>%
 *
 * Returns the target game instance id stored
 * for the supplied player UUID.
 *
 * Useful for Skript or other integrations where
 * a different player's intent needs to be read
 * without relying on the current PAPI player context.
 */
        hook.registerWithArgs(
            "intent"
        ) { _, args ->

            val playerId = args
                .firstOrNull()
                ?.let {
                    runCatching {
                        UUID.fromString(
                            it
                        )
                    }.getOrNull()
                }
                ?: return@registerWithArgs ""

            plugin.gameRedisService
                .getJoinIntent(
                    playerId
                )
                ?: ""
        }
    }
}