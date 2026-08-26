package online.veloraplugins.gamelink.paper.commands

import com.github.shynixn.mccoroutine.bukkit.scope
import online.veloraplugins.engine.message.audience
import online.veloraplugins.gamelink.paper.VeloraGameLinkPlugin
import online.veloraplugins.gamelink.paper.message.GameLinkMessage
import org.bukkit.command.CommandSender
import org.incendo.cloud.description.CommandDescription
import org.incendo.cloud.kotlin.coroutines.extension.suspendingHandler
import org.incendo.cloud.parser.standard.IntegerParser
import org.incendo.cloud.parser.standard.StringParser

class GameManagementCommands(
    private val plugin: VeloraGameLinkPlugin
) {

    init {

        val permission =
            "gamelink.command.manage"

        val root = plugin
            .commandManager
            .root()
            .literal("manage")
            .permission(
                permission
            )

        /*
         * /gamelink game register
         * <id> <type> <state> <players> <maxPlayers> [map]
         */

        plugin.commandManager.manager.command(

            root.literal("register")
                .permission("$permission.register")
                .required(
                    "id",
                    StringParser.stringParser()
                )
                .required(
                    "type",
                    StringParser.stringParser()
                )
                .required(
                    "state",
                    StringParser.stringParser()
                )
                .required(
                    "players",
                    IntegerParser.integerParser(
                        0
                    )
                )
                .required(
                    "maxPlayers",
                    IntegerParser.integerParser(
                        1
                    )
                )
                .optional(
                    "map",
                    StringParser.stringParser()
                )
                .commandDescription(
                    CommandDescription.commandDescription(
                        "Register a game instance."
                    )
                )
                .suspendingHandler(plugin.scope) { context ->

                    val id = context.get<String>(
                        "id"
                    )

                    val type = context.get<String>(
                        "type"
                    )

                    val state = context.get<String>(
                        "state"
                    )

                    val players = context.get<Int>(
                        "players"
                    )

                    val maxPlayers = context.get<Int>(
                        "maxPlayers"
                    )

                    val map = context.getOrDefault<String?>(
                        "map",
                        null
                    )

                    if (plugin.gameLinkService.isRegistered(
                            id
                        )
                    ) {

                        plugin.messages.send(
                            context.sender().audience(),
                            GameLinkMessage.GAME_ALREADY_REGISTERED,
                            "game" to id
                        )

                        return@suspendingHandler
                    }

                    runCatching {

                        plugin.gameLinkService.registerGame(
                            id = id,
                            type = type,
                            state = state,
                            players = players,
                            maxPlayers = maxPlayers,
                            map = map
                        )

                    }.onSuccess {

                        plugin.messages.send(
                            context.sender().audience(),
                            GameLinkMessage.GAME_REGISTERED,
                            "game" to id
                        )

                    }.onFailure { throwable ->

                        plugin.debug(
                            "COMMAND",
                            "Unable to register game '$id'.",
                            throwable
                        )

                        plugin.messages.send(
                            context.sender().audience(),
                            GameLinkMessage.GAME_OPERATION_FAILED,
                            "game" to id,
                            "error" to (
                                throwable.message
                                    ?: "Unknown error"
                                )
                        )
                    }
                }
        )

        /*
         * /gamelink game unregister <id>
         */

        plugin.commandManager.manager.command(

            root.literal("unregister")
                .permission("$permission.unregister")
                .required(
                    "id",
                    StringParser.stringParser()
                )
                .commandDescription(
                    CommandDescription.commandDescription(
                        "Unregister a local game instance."
                    )
                )
                .suspendingHandler(plugin.scope) { context ->

                    val id = context.get<String>(
                        "id"
                    )

                    if (!plugin.gameLinkService.unregisterGame(
                            id
                        )
                    ) {

                        plugin.messages.send(
                            context.sender().audience(),
                            GameLinkMessage.GAME_NOT_FOUND,
                            "game" to id
                        )

                        return@suspendingHandler
                    }

                    plugin.messages.send(
                        context.sender().audience(),
                        GameLinkMessage.GAME_UNREGISTERED,
                        "game" to id
                    )
                }
        )

        /*
         * /gamelink game state <id> <state>
         */

        plugin.commandManager.manager.command(

            root.literal("state")
                .permission("$permission.state")
                .required(
                    "id",
                    StringParser.stringParser()
                )
                .required(
                    "state",
                    StringParser.stringParser()
                )
                .commandDescription(
                    CommandDescription.commandDescription(
                        "Update a game's state."
                    )
                )
                .suspendingHandler(plugin.scope) { context ->

                    val id = context.get<String>(
                        "id"
                    )

                    val state = context.get<String>(
                        "state"
                    )

                    if (!plugin.gameLinkService.updateState(
                            id = id,
                            state = state
                        )
                    ) {

                        sendGameNotFound(
                            context = context,
                            id = id
                        )

                        return@suspendingHandler
                    }

                    plugin.messages.send(
                        context.sender().audience(),
                        GameLinkMessage.GAME_STATE_UPDATED,
                        "game" to id,
                        "state" to state
                    )
                }
        )

        /*
         * /gamelink game players <id> <players>
         */

        plugin.commandManager.manager.command(

            root.literal("players")
                .permission("$permission.players")
                .required(
                    "id",
                    StringParser.stringParser()
                )
                .required(
                    "players",
                    IntegerParser.integerParser(
                        0
                    )
                )
                .commandDescription(
                    CommandDescription.commandDescription(
                        "Update a game's player count."
                    )
                )
                .suspendingHandler(plugin.scope) { context ->

                    val id = context.get<String>(
                        "id"
                    )

                    val players = context.get<Int>(
                        "players"
                    )

                    runCatching {

                        plugin.gameLinkService.updatePlayers(
                            id = id,
                            players = players
                        )

                    }.onSuccess { updated ->

                        if (!updated) {

                            sendGameNotFound(
                                context = context,
                                id = id
                            )

                            return@onSuccess
                        }

                        plugin.messages.send(
                            context.sender().audience(),
                            GameLinkMessage.GAME_PLAYERS_UPDATED,
                            "game" to id,
                            "players" to players.toString()
                        )

                    }.onFailure { throwable ->

                        plugin.debug(
                            "COMMAND",
                            "Unable to update players for game '$id'.",
                            throwable
                        )

                        plugin.messages.send(
                            context.sender().audience(),
                            GameLinkMessage.GAME_OPERATION_FAILED,
                            "game" to id,
                            "error" to (
                                throwable.message
                                    ?: "Unknown error"
                                )
                        )
                    }
                }
        )

        /*
         * /gamelink game map <id> <map>
         */

        plugin.commandManager.manager.command(

            root.literal("map")
                .permission("$permission.map")
                .required(
                    "id",
                    StringParser.stringParser()
                )
                .required(
                    "map",
                    StringParser.stringParser()
                )
                .commandDescription(
                    CommandDescription.commandDescription(
                        "Update a game's map."
                    )
                )
                .suspendingHandler(plugin.scope) { context ->

                    val id = context.get<String>(
                        "id"
                    )

                    val map = context.get<String>(
                        "map"
                    )

                    if (!plugin.gameLinkService.updateMap(
                            id = id,
                            map = map
                        )
                    ) {

                        sendGameNotFound(
                            context = context,
                            id = id
                        )

                        return@suspendingHandler
                    }

                    plugin.messages.send(
                        context.sender().audience(),
                        GameLinkMessage.GAME_MAP_UPDATED,
                        "game" to id,
                        "map" to map
                    )
                }
        )

        /*
         * /gamelink game clearmap <id>
         */

        plugin.commandManager.manager.command(

            root.literal("clearmap")
                .permission("$permission.map")
                .required(
                    "id",
                    StringParser.stringParser()
                )
                .commandDescription(
                    CommandDescription.commandDescription(
                        "Clear a game's map."
                    )
                )
                .suspendingHandler(plugin.scope) { context ->

                    val id = context.get<String>(
                        "id"
                    )

                    if (!plugin.gameLinkService.updateMap(
                            id = id,
                            map = null
                        )
                    ) {

                        sendGameNotFound(
                            context = context,
                            id = id
                        )

                        return@suspendingHandler
                    }

                    plugin.messages.send(
                        context.sender().audience(),
                        GameLinkMessage.GAME_MAP_CLEARED,
                        "game" to id
                    )
                }
        )

        /*
         * /gamelink game info <id>
         */

        plugin.commandManager.manager.command(

            root.literal("info")
                .permission("$permission.info")
                .required(
                    "id",
                    StringParser.stringParser()
                )
                .commandDescription(
                    CommandDescription.commandDescription(
                        "Show information about a game instance."
                    )
                )
                .suspendingHandler(plugin.scope) { context ->

                    val id = context.get<String>(
                        "id"
                    )

                    val game = plugin
                        .gameLinkService
                        .getGame(
                            id
                        )
                        ?: run {

                            sendGameNotFound(
                                context = context,
                                id = id
                            )

                            return@suspendingHandler
                        }

                    plugin.messages.send(
                        context.sender().audience(),
                        GameLinkMessage.GAME_INFO,
                        "id" to game.id,
                        "type" to game.type,
                        "server" to game.serverId,
                        "state" to game.state,
                        "players" to game.players.toString(),
                        "max_players" to game.maxPlayers.toString(),
                        "map" to (game.map ?: "None")
                    )
                }
        )
    }

    /*
     * Game not found
     */

    private fun sendGameNotFound(
        context: org.incendo.cloud.context.CommandContext<CommandSender>,
        id: String
    ) {

        plugin.messages.send(
            context.sender().audience(),
            GameLinkMessage.GAME_NOT_FOUND,
            "game" to id
        )
    }
}