package online.veloraplugins.gamelink.paper.services

import com.google.common.io.ByteStreams
import online.veloraplugins.engine.message.audience
import online.veloraplugins.gamelink.api.game.GameInstance
import online.veloraplugins.gamelink.api.game.GameJoinResult
import online.veloraplugins.gamelink.paper.VeloraGameLinkPlugin
import online.veloraplugins.gamelink.paper.message.GameLinkMessage
import org.bukkit.entity.Player

class GameJoinService(
    private val plugin: VeloraGameLinkPlugin,
    private val gameRedisService: GameRedisService,
    private val gameSelectorService: GameSelectorService
) {

    /*
     * Join specific game
     */

    fun join(
        player: Player,
        gameId: String
    ): GameJoinResult {

        val game = gameRedisService.get(
            gameId
        ) ?: run {

            plugin.debug(
                "JOIN",
                "Player '${player.name}' tried to join game '$gameId', " +
                        "but it no longer exists."
            )

            plugin.messages.send(
                player.audience(),
                GameLinkMessage.GAME_NOT_FOUND,
                "game" to gameId
            )

            return GameJoinResult.GAME_NOT_FOUND
        }

        return join(
            player = player,
            game = game
        )
    }

    /*
     * Join game instance
     */

    fun join(
        player: Player,
        game: GameInstance
    ): GameJoinResult {

        /*
         * Always fetch the latest game snapshot from Redis.
         */

        val current = gameRedisService.get(
            game.id
        ) ?: run {

            plugin.debug(
                "JOIN",
                "Game '${game.id}' disappeared before '${player.name}' could join."
            )

            plugin.messages.send(
                player.audience(),
                GameLinkMessage.GAME_NOT_FOUND,
                "game" to game.id
            )

            return GameJoinResult.GAME_NOT_FOUND
        }

        /*
         * Check capacity.
         */

        if (current.isFull) {

            plugin.debug(
                "JOIN",
                "Player '${player.name}' could not join '${current.id}': " +
                        "game is full (${current.players}/${current.maxPlayers})."
            )

            plugin.messages.send(
                player.audience(),
                GameLinkMessage.GAME_FULL,
                "game" to current.id
            )

            return GameJoinResult.GAME_FULL
        }

        /*
         * Check whether the current game state allows joining.
         */

        if (!gameSelectorService.isJoinable(
                current
            )
        ) {

            plugin.debug(
                "JOIN",
                "Player '${player.name}' could not join '${current.id}': " +
                        "state '${current.state}' is not joinable."
            )

            plugin.messages.send(
                player.audience(),
                GameLinkMessage.GAME_NOT_JOINABLE,
                "game" to current.id,
                "state" to current.state
            )

            return GameJoinResult.STATE_NOT_JOINABLE
        }

        /*
         * Create join intent.
         *
         * This must happen BEFORE the player is transferred
         * to the destination Minecraft server.
         *
         * The destination GAME server will consume this
         * intent and use the stored game id to determine
         * which local game instance should receive the player.
         */

        gameRedisService.createJoinIntent(
            playerId = player.uniqueId,
            gameId = current.id
        )

        /*
         * Verify that the intent was successfully created.
         *
         * If Redis is unavailable, or the intent could not
         * be stored, do not transfer the player because the
         * destination server would not know which game
         * instance should handle them.
         */

        if (!gameRedisService.hasValidJoinIntent(
                player.uniqueId
            )
        ) {

            plugin.debug(
                "JOIN",
                "Unable to transfer '${player.name}' to '${current.id}': " +
                        "join intent could not be created."
            )

            plugin.messages.send(
                player.audience(),
                GameLinkMessage.REDIS_UNAVAILABLE
            )

            return GameJoinResult.REDIS_UNAVAILABLE
        }

        /*
         * Player feedback.
         */

        plugin.messages.send(
            player.audience(),
            GameLinkMessage.JOINING_GAME,
            "game" to current.id
        )

        /*
         * Transfer player to the Minecraft server hosting
         * the requested game instance.
         */

        connect(
            player = player,
            serverId = current.serverId
        )

        plugin.debug(
            "JOIN",
            "Player '${player.name}' joining game '${current.id}' " +
                    "(${current.type}/${current.state}) " +
                    "on server '${current.serverId}'."
        )

        return GameJoinResult.SUCCESS
    }

    /*
     * QuickJoin
     */

    fun quickJoin(
        player: Player,
        gameType: String
    ): GameJoinResult {

        val game = gameSelectorService.findBestGame(
            gameType
        ) ?: run {

            plugin.debug(
                "JOIN",
                "No available '$gameType' game found for '${player.name}'."
            )

            plugin.messages.send(
                player.audience(),
                GameLinkMessage.NO_AVAILABLE_GAME,
                    "type" to gameType
            )

            return GameJoinResult.NO_AVAILABLE_GAME
        }

        return join(
            player = player,
            game = game
        )
    }

    /*
     * Check
     */

    fun canJoin(
        game: GameInstance
    ): Boolean {

        return gameSelectorService.isJoinable(
            game
        )
    }

    /*
     * Transfer
     */

    private fun connect(
        player: Player,
        serverId: String
    ) {

        val output = ByteStreams.newDataOutput()

        output.writeUTF(
            "Connect"
        )

        output.writeUTF(
            serverId
        )

        player.sendPluginMessage(
            plugin,
            VeloraGameLinkPlugin.PROXY_CHANNEL,
            output.toByteArray()
        )
    }
}