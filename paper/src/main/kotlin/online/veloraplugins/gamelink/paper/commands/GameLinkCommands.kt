package online.veloraplugins.gamelink.paper.commands

import com.github.shynixn.mccoroutine.bukkit.scope
import online.veloraplugins.engine.message.audience
import online.veloraplugins.gamelink.paper.VeloraGameLinkPlugin
import online.veloraplugins.gamelink.paper.configurations.GameSignsConfig
import online.veloraplugins.gamelink.paper.gui.GamesGui
import online.veloraplugins.gamelink.paper.message.GameLinkMessage
import org.bukkit.block.Sign
import org.bukkit.entity.Player
import org.incendo.cloud.description.CommandDescription
import org.incendo.cloud.kotlin.coroutines.extension.suspendingHandler
import org.incendo.cloud.parser.standard.StringParser

class GameLinkCommands(
    private val plugin: VeloraGameLinkPlugin
) {

    init {

        val permission = "gamelink.command"

        val root = plugin.commandManager
            .root()
            .permission(permission)

        /*
         * /gamelink
         */

        plugin.commandManager.manager.command(

            root.suspendingHandler(plugin.scope) {

                plugin.messages.send(
                    it.sender().audience(),
                    GameLinkMessage.HELP
                )
            }
        )

        /*
         * /gamelink reload
         */

        plugin.commandManager.manager.command(

            root.literal("reload")
                .permission("$permission.reload")
                .commandDescription(
                    CommandDescription.commandDescription(
                        "Reload VeloraGameLink."
                    )
                )
                .suspendingHandler(plugin.scope) {

                    plugin.reloadPlugin()

                    plugin.messages.send(
                        it.sender().audience(),
                        GameLinkMessage.RELOAD_SUCCESS
                    )
                }
        )

        /*
         * /gamelink debug
         */

        plugin.commandManager.manager.command(

            root.literal("debug")
                .permission("$permission.debug")
                .commandDescription(
                    CommandDescription.commandDescription(
                        "Show debug status."
                    )
                )
                .suspendingHandler(plugin.scope) {

                    plugin.messages.send(
                        it.sender().audience(),
                        GameLinkMessage.DEBUG_STATUS,
                        "status" to if (plugin.pluginConfig.debug) {
                            "enabled"
                        } else {
                            "disabled"
                        }
                    )
                }
        )

        /*
         * /gamelink games
         */

        plugin.commandManager.manager.command(

            root.literal("games")
                .permission("$permission.games")
                .commandDescription(
                    CommandDescription.commandDescription(
                        "Show available game instances."
                    )
                )
                .suspendingHandler(plugin.scope) {

                    val games = plugin.gameRedisService.getAll()

                    if (games.isEmpty()) {

                        plugin.messages.send(
                            it.sender().audience(),
                            GameLinkMessage.GAMES_EMPTY
                        )

                        return@suspendingHandler
                    }

                    plugin.messages.send(
                        it.sender().audience(),
                        GameLinkMessage.GAMES_HEADER
                    )

                    games
                        .sortedWith(
                            compareBy(
                                { it.type.lowercase() },
                                { it.id.lowercase() }
                            )
                        )
                        .forEach { game ->

                            plugin.messages.send(
                                it.sender().audience(),
                                GameLinkMessage.GAMES_ENTRY,
                                "id" to game.id,
                                "type" to game.type,
                                "server" to game.serverId,
                                "state" to game.state,
                                "players" to game.players.toString(),
                                "max_players" to game.maxPlayers.toString()
                            )
                        }
                }
        )

        /*
         * /gamelink quickjoin <game>
         */

        plugin.commandManager.manager.command(

            root.literal("quickjoin")
                .permission("$permission.quickjoin")
                .required(
                    "game",
                    StringParser.stringParser()
                )
                .commandDescription(
                    CommandDescription.commandDescription(
                        "Join the best available game instance."
                    )
                )
                .suspendingHandler(plugin.scope) { context ->

                    val sender = context.sender()

                    if (sender !is Player) {

                        plugin.messages.send(
                            sender.audience(),
                            GameLinkMessage.ONLY_PLAYERS
                        )

                        return@suspendingHandler
                    }

                    val gameType = context.get<String>(
                        "game"
                    )

                    plugin.gameJoinService.quickJoin(
                        player = sender,
                        gameType = gameType
                    )
                }
        )

        /*
 * /gamelink menu [game]
 */

        plugin.commandManager.manager.command(

            root.literal("menu")
                .permission("$permission.menu")
                .optional(
                    "game",
                    StringParser.stringParser()
                )
                .commandDescription(
                    CommandDescription.commandDescription(
                        "Open the games menu."
                    )
                )
                .suspendingHandler(plugin.scope) { context ->

                    val sender = context.sender()

                    if (sender !is Player) {

                        plugin.messages.send(
                            sender.audience(),
                            GameLinkMessage.ONLY_PLAYERS
                        )

                        return@suspendingHandler
                    }

                    val gameType = context.getOrDefault<String?>(
                        "game",
                        null
                    )

                    /*
                     * All games menu
                     */

                    if (gameType == null) {

                        if (!sender.hasPermission(
                                "gamelink.command.menu.all"
                            )
                        ) {

                            plugin.messages.send(
                                sender.audience(),
                                GameLinkMessage.NO_PERMISSION
                            )

                            return@suspendingHandler
                        }

                        GamesGui(
                            plugin = plugin
                        ).open(
                            sender
                        )

                        return@suspendingHandler
                    }

                    /*
                     * Specific game type
                     */

                    val normalizedGameType = gameType
                        .lowercase()

                    if (!sender.hasPermission(
                            "gamelink.command.menu.$normalizedGameType"
                        )
                    ) {

                        plugin.messages.send(
                            sender.audience(),
                            GameLinkMessage.NO_PERMISSION
                        )

                        return@suspendingHandler
                    }

                    GamesGui(
                        plugin = plugin,
                        gameType = gameType
                    ).open(
                        sender
                    )
                }
        )
    }
}