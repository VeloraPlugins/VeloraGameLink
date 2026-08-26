package online.veloraplugins.gamelink.paper.services

import online.velora.framework.eventbus.manager.EventBusManager
import online.veloraplugins.gamelink.api.VeloraGameLinkApi
import online.veloraplugins.gamelink.api.events.GameRegisteredEvent
import online.veloraplugins.gamelink.api.events.GameRemovedEvent
import online.veloraplugins.gamelink.api.events.GameUpdatedEvent
import online.veloraplugins.gamelink.api.game.GameInstance
import java.util.UUID

class GameLinkService(
    private val gameInstanceService: GameInstanceService,
    private val gameRedisService: GameRedisService,
    private val gameSelectorService: GameSelectorService,
    private val eventBusManager: EventBusManager
) : VeloraGameLinkApi {

    /*
     * Registration
     */

    override fun registerGame(
        id: String,
        type: String,
        state: String,
        players: Int,
        maxPlayers: Int,
        map: String?
    ): GameInstance {

        val managed = gameInstanceService.register(
            id = id,
            type = type,
            state = state,
            players = players,
            maxPlayers = maxPlayers,
            map = map
        )

        val game = managed.snapshot()

        gameRedisService.save(
            game
        )

        eventBusManager.publish(
            GameRegisteredEvent(
                game
            )
        )

        return game
    }

    /*
     * Unregister
     */

    override fun unregisterGame(
        id: String
    ): Boolean {

        val managed = gameInstanceService.unregister(
            id
        ) ?: return false

        val game = managed.snapshot()

        gameRedisService.remove(
            game
        )

        eventBusManager.publish(
            GameRemovedEvent(
                gameId = game.id,
                serverId = game.serverId
            )
        )

        return true
    }

    /*
     * State
     */

    override fun updateState(
        id: String,
        state: String
    ): Boolean {

        val updated = gameInstanceService.updateState(
            id = id,
            state = state
        )

        if (!updated) {
            return false
        }

        publishUpdate(
            id
        )

        return true
    }

    /*
     * Players
     */

    override fun updatePlayers(
        id: String,
        players: Int
    ): Boolean {

        val updated = gameInstanceService.updatePlayers(
            id = id,
            players = players
        )

        if (!updated) {
            return false
        }

        publishUpdate(
            id
        )

        return true
    }

    /*
     * Map
     */

    override fun updateMap(
        id: String,
        map: String?
    ): Boolean {

        val updated = gameInstanceService.updateMap(
            id = id,
            map = map
        )

        if (!updated) {
            return false
        }

        publishUpdate(
            id
        )

        return true
    }

    /*
     * Full update
     */

    override fun updateGame(
        id: String,
        state: String?,
        players: Int?,
        maxPlayers: Int?,
        map: String?
    ): Boolean {

        val game = gameInstanceService.getManaged(
            id
        ) ?: return false

        state?.let {

            require(
                it.isNotBlank()
            ) {
                "Game state cannot be blank."
            }

            game.state = it
        }

        maxPlayers?.let {

            require(
                it > 0
            ) {
                "Max players must be greater than 0."
            }

            game.maxPlayers = it
        }

        players?.let {

            require(
                it in 0..game.maxPlayers
            ) {
                "Players must be between 0 and ${game.maxPlayers}."
            }

            game.players = it
        }

        /*
         * map is nullable, which creates ambiguity:
         *
         * null can mean:
         * - don't update the map
         * - explicitly remove the map
         *
         * With this API signature we treat null as
         * 'do not update'.
         */

        if (map != null) {
            game.map = map
        }

        publishUpdate(
            id
        )

        return true
    }

    /*
     * Get game
     *
     * Network-wide lookup.
     */

    override fun getGame(
        id: String
    ): GameInstance? {

        return gameRedisService.get(
            id
        )
    }

    /*
     * Get games
     *
     * Network-wide.
     */

    override fun getGames(): Collection<GameInstance> {

        return gameRedisService.getAll()
    }

    /*
     * Get games by type
     *
     * Network-wide.
     */

    override fun getGames(
        type: String
    ): Collection<GameInstance> {

        return gameRedisService.getByType(
            type
        )
    }

    /*
     * Best game
     */

    override fun findBestGame(
        type: String
    ): GameInstance? {

        return gameSelectorService.findBestGame(
            type
        )
    }

    /*
     * Available games
     */

    override fun getAvailableGames(
        type: String
    ): Collection<GameInstance> {

        return gameSelectorService.getAvailableGames(
            type
        )
    }

    /*
     * Registered
     *
     * Local registration check.
     */

    override fun isRegistered(
        id: String
    ): Boolean {

        return gameInstanceService.getManaged(
            id
        ) != null
    }

    /*
     * Local games
     */

    override fun getLocalGames(): Collection<GameInstance> {

        return gameInstanceService.getAll()
    }

    /*
     * Create join intent
     *
     * Stores the target game id for a player before
     * the player is transferred to the destination
     * GAME server.
     *
     * The intent is stored temporarily in Redis and
     * allows the destination server to determine
     * which local game instance should receive
     * the incoming player.
     */

    override fun createJoinIntent(
        playerId: UUID,
        gameId: String
    ) {

        gameRedisService.createJoinIntent(
            playerId = playerId,
            gameId = gameId
        )
    }

    /*
     * Get join intent
     *
     * Returns the target game id currently stored
     * for the player.
     *
     * The join intent remains stored after this call.
     */

    override fun getJoinIntent(
        playerId: UUID
    ): String? {

        return gameRedisService.getJoinIntent(
            playerId
        )
    }

    /*
     * Consume join intent
     *
     * Returns the target game id and immediately
     * removes the join intent from Redis.
     *
     * This should normally be used when a player
     * arrives on the destination GAME server.
     */

    override fun consumeJoinIntent(
        playerId: UUID
    ): String? {

        return gameRedisService.consumeJoinIntent(
            playerId
        )
    }

    /*
     * Remove join intent
     *
     * Removes a pending join intent without returning
     * the target game id.
     *
     * This can be used when a transfer is cancelled
     * or an intent should explicitly be invalidated.
     */

    override fun removeJoinIntent(
        playerId: UUID
    ): Boolean {

        return gameRedisService.removeJoinIntent(
            playerId
        )
    }

    /*
     * Has join intent
     *
     * Returns whether a join intent key currently
     * exists for the player.
     *
     * This does not validate the remaining TTL.
     */

    override fun hasJoinIntent(
        playerId: UUID
    ): Boolean {

        return gameRedisService.hasJoinIntent(
            playerId
        )
    }

    /*
     * Has valid join intent
     *
     * Returns whether the player currently has
     * a join intent with a positive remaining TTL.
     */

    override fun hasValidJoinIntent(
        playerId: UUID
    ): Boolean {

        return gameRedisService.hasValidJoinIntent(
            playerId
        )
    }

    /*
     * Get join intent TTL
     *
     * Returns the remaining join intent lifetime
     * in seconds.
     *
     * Redis special values:
     *
     *   -1 = key exists without expiration
     *   -2 = key does not exist
     */

    override fun getJoinIntentTtl(
        playerId: UUID
    ): Long {

        return gameRedisService.getJoinIntentTtl(
            playerId
        )
    }

    /*
     * Publish update
     *
     * Saves the latest local game snapshot to Redis
     * and publishes a GameUpdatedEvent to the
     * GameLink event bus.
     */

    private fun publishUpdate(
        id: String
    ) {

        val game = gameInstanceService.get(
            id
        ) ?: return

        gameRedisService.save(
            game
        )

        eventBusManager.publish(
            GameUpdatedEvent(
                game
            )
        )
    }
}