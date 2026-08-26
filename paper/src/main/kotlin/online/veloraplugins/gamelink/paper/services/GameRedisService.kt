package online.veloraplugins.gamelink.paper.services

import online.velora.framework.redis.RedisManager
import online.veloraplugins.gamelink.api.game.GameInstance
import online.veloraplugins.gamelink.paper.VeloraGameLinkPlugin
import java.util.UUID

class GameRedisService(
    private val plugin: VeloraGameLinkPlugin,
    private val redisManager: RedisManager,
    private val gameInstanceService: GameInstanceService
) {

    companion object {

        private const val PREFIX =
            "velora:gamelink"

        private const val GAME_INDEX =
            "$PREFIX:index:games"

        private fun gameKey(
            gameId: String
        ): String {
            return "$PREFIX:game:$gameId"
        }

        private fun typeIndexKey(
            type: String
        ): String {
            return "$PREFIX:index:type:${type.lowercase()}"
        }

        private fun serverIndexKey(
            serverId: String
        ): String {
            return "$PREFIX:index:server:$serverId"
        }

        private fun joinKey(
            playerId: UUID
        ): String {
            return "$PREFIX:join:$playerId"
        }
    }

    private var running = false

    /*
     * Lifecycle
     */

    fun start() {

        if (running) {
            return
        }

        if (!redisManager.isConnected()) {
            plugin.warn(
                "GameRedisService could not start because Redis is not connected."
            )
            return
        }

        running = true

        plugin.debug(
            "REDIS",
            "GameRedisService started."
        )
    }

    fun shutdown() {

        if (!running) {
            return
        }

        if (redisManager.isConnected()) {
            gameInstanceService
                .getAll()
                .forEach {
                    remove(it)
                }
        }

        running = false

        plugin.debug(
            "REDIS",
            "GameRedisService stopped."
        )
    }

    /*
     * Save
     */

    fun save(
        game: GameInstance
    ) {

        if (!isAvailable()) {
            return
        }

        val key = gameKey(
            game.id
        )

        redisManager.hMSet(
            key,
            mapOf(
                "id" to game.id,
                "type" to game.type,
                "serverId" to game.serverId,
                "state" to game.state,
                "players" to game.players.toString(),
                "maxPlayers" to game.maxPlayers.toString(),
                "map" to (game.map ?: "")
            )
        )

        redisManager.expire(
            key,
            plugin.pluginConfig
                .synchronization
                .instanceTimeout
        )

        redisManager.sAdd(
            GAME_INDEX,
            game.id
        )

        redisManager.sAdd(
            typeIndexKey(game.type),
            game.id
        )

        redisManager.sAdd(
            serverIndexKey(game.serverId),
            game.id
        )

        plugin.debug(
            "REDIS",
            "Saved game '${game.id}' to Redis."
        )
    }

    /*
     * Get
     */

    fun get(
        gameId: String
    ): GameInstance? {

        if (!isAvailable()) {
            return null
        }

        val values = redisManager.hGetAll(
            gameKey(gameId)
        )

        if (values.isEmpty()) {
            return null
        }

        return decode(
            values
        )
    }

    /*
     * Get all
     */

    fun getAll(): List<GameInstance> {

        if (!isAvailable()) {
            return emptyList()
        }

        return redisManager
            .sMembers(GAME_INDEX)
            .mapNotNull { gameId ->

                val game = get(
                    gameId
                )

                if (game == null) {
                    cleanupStaleGameId(
                        gameId
                    )
                }

                game
            }
    }

    /*
     * Get by type
     */

    fun getByType(
        type: String
    ): List<GameInstance> {

        if (!isAvailable()) {
            return emptyList()
        }

        val indexKey = typeIndexKey(
            type
        )

        return redisManager
            .sMembers(indexKey)
            .mapNotNull { gameId ->

                val game = get(
                    gameId
                )

                if (game == null) {
                    redisManager.sRem(
                        indexKey,
                        gameId
                    )

                    redisManager.sRem(
                        GAME_INDEX,
                        gameId
                    )
                }

                game
            }
    }

    /*
     * Get by server
     */

    fun getByServer(
        serverId: String
    ): List<GameInstance> {

        if (!isAvailable()) {
            return emptyList()
        }

        val indexKey = serverIndexKey(
            serverId
        )

        return redisManager
            .sMembers(indexKey)
            .mapNotNull { gameId ->

                val game = get(
                    gameId
                )

                if (game == null) {
                    redisManager.sRem(
                        indexKey,
                        gameId
                    )

                    redisManager.sRem(
                        GAME_INDEX,
                        gameId
                    )
                }

                game
            }
    }

    /*
     * Remove
     */

    fun remove(
        game: GameInstance
    ) {

        if (!redisManager.isConnected()) {
            return
        }

        redisManager.del(
            gameKey(game.id)
        )

        redisManager.sRem(
            GAME_INDEX,
            game.id
        )

        redisManager.sRem(
            typeIndexKey(game.type),
            game.id
        )

        redisManager.sRem(
            serverIndexKey(game.serverId),
            game.id
        )

        plugin.debug(
            "REDIS",
            "Removed game '${game.id}' from Redis."
        )
    }

    /*
     * Heartbeat
     */

    fun heartbeat() {

        if (!isAvailable()) {
            return
        }

        val games = gameInstanceService
            .getAll()

        games.forEach {
            save(it)
        }

        plugin.debug(
            "REDIS",
            "Heartbeat refreshed ${games.size} local game instance(s)."
        )
    }

    /*
     * Stale cleanup
     */

    private fun cleanupStaleGameId(
        gameId: String
    ) {

        redisManager.sRem(
            GAME_INDEX,
            gameId
        )

        /*
         * We no longer know the type/server from the expired hash.
         * Entries in those indexes are cleaned lazily by getByType()
         * and getByServer().
         */

        plugin.debug(
            "REDIS",
            "Cleaned stale game index entry '$gameId'."
        )
    }

    /*
     * Decode
     */

    private fun decode(
        values: Map<String, String>
    ): GameInstance? {

        return runCatching {

            GameInstance(
                id = values.getValue(
                    "id"
                ),
                type = values.getValue(
                    "type"
                ),
                serverId = values.getValue(
                    "serverId"
                ),
                state = values.getValue(
                    "state"
                ),
                players = values
                    .getValue(
                        "players"
                    )
                    .toInt(),
                maxPlayers = values
                    .getValue(
                        "maxPlayers"
                    )
                    .toInt(),
                map = values["map"]
                    ?.takeIf {
                        it.isNotBlank()
                    }
            )
        }.onFailure {

            plugin.debug(
                "REDIS",
                "Failed to decode game instance.",
                it
            )

        }.getOrNull()
    }

    /*
     * Join intent
     *
     * A join intent is created before a player is
     * transferred from a lobby to a game server.
     *
     * The key is stored temporarily in Redis:
     *
     *   velora:gamelink:join:<player-uuid>
     *
     * The value contains the target game instance id.
     *
     * This allows the destination Minecraft server to
     * determine which local game instance the player
     * should be assigned to after connecting.
     */

    fun createJoinIntent(
        playerId: UUID,
        gameId: String
    ) {

        if (!isAvailable()) {

            plugin.debug(
                "REDIS",
                "Unable to create join intent for player '$playerId': " +
                        "Redis is unavailable."
            )

            return
        }

        require(
            gameId.isNotBlank()
        ) {
            "Game id cannot be blank."
        }

        redisManager.setEx(
            joinKey(
                playerId
            ),
            plugin.pluginConfig.synchronization.joinIntentTimeout,
            gameId
        )

        plugin.debug(
            "REDIS",
            "Created join intent for player '$playerId' -> game '$gameId'."
        )
    }

    /*
     * Get join intent
     *
     * Returns the game instance id currently assigned
     * to the player's join intent.
     *
     * This does NOT remove the intent from Redis.
     *
     * Use this when the intent only needs to be inspected.
     * When handling the actual player arrival on a game
     * server, consumeJoinIntent() should normally be used.
     */

    fun getJoinIntent(
        playerId: UUID
    ): String? {

        if (!isAvailable()) {

            plugin.debug(
                "REDIS",
                "Unable to get join intent for player '$playerId': " +
                        "Redis is unavailable."
            )

            return null
        }

        val gameId = redisManager.get(
            joinKey(
                playerId
            )
        )

        if (gameId == null) {

            plugin.debug(
                "REDIS",
                "No join intent found for player '$playerId'."
            )

            return null
        }

        plugin.debug(
            "REDIS",
            "Found join intent for player '$playerId' -> game '$gameId'."
        )

        return gameId
    }

    /*
     * Consume join intent
     *
     * Retrieves the target game instance id and removes
     * the join intent immediately afterwards.
     *
     * This should normally be called when the player
     * arrives on the target GAME server.
     *
     * Removing the key prevents the same intent from
     * being processed multiple times.
     */

    fun consumeJoinIntent(
        playerId: UUID
    ): String? {

        if (!isAvailable()) {

            plugin.debug(
                "REDIS",
                "Unable to consume join intent for player '$playerId': " +
                        "Redis is unavailable."
            )

            return null
        }

        val key = joinKey(
            playerId
        )

        val gameId = redisManager.get(
            key
        ) ?: run {

            plugin.debug(
                "REDIS",
                "No join intent found for player '$playerId' to consume."
            )

            return null
        }

        redisManager.del(
            key
        )

        plugin.debug(
            "REDIS",
            "Consumed join intent for player '$playerId' -> game '$gameId'."
        )

        return gameId
    }

    /*
     * Remove join intent
     *
     * Removes a player's join intent without consuming
     * or returning its game instance id.
     *
     * This can be used when a transfer is cancelled or
     * when an intent needs to be explicitly invalidated.
     */

    fun removeJoinIntent(
        playerId: UUID
    ): Boolean {

        if (!isAvailable()) {

            plugin.debug(
                "REDIS",
                "Unable to remove join intent for player '$playerId': " +
                        "Redis is unavailable."
            )

            return false
        }

        val key = joinKey(
            playerId
        )

        if (!redisManager.exists(
                key
            )
        ) {

            plugin.debug(
                "REDIS",
                "Unable to remove join intent for player '$playerId': " +
                        "intent does not exist."
            )

            return false
        }

        redisManager.del(
            key
        )

        plugin.debug(
            "REDIS",
            "Removed join intent for player '$playerId'."
        )

        return true
    }

    /*
     * Check join intent
     *
     * Returns whether the player currently has an active
     * join intent stored in Redis.
     */

    fun hasJoinIntent(
        playerId: UUID
    ): Boolean {

        if (!isAvailable()) {
            return false
        }

        return redisManager.exists(
            joinKey(
                playerId
            )
        )
    }

    /*
     * Get join intent TTL
     *
     * Returns the remaining lifetime of a player's
     * join intent in seconds.
     *
     * Redis may return special values:
     *
     *   -1 = the key exists but has no expiration
     *   -2 = the key does not exist
     *
     * If Redis is unavailable, -2 is returned.
     */
    fun getJoinIntentTtl(
        playerId: UUID
    ): Long {

        if (!isAvailable()) {
            return -2L
        }

        return redisManager.ttl(
            joinKey(
                playerId
            )
        )
    }

    /*
     * Check valid join intent
     *
     * Returns whether the player currently has a
     * valid, non-expired join intent stored in Redis.
     *
     * A join intent is considered valid when its
     * remaining TTL is greater than 0 seconds.
     *
     * This means:
     *
     *   > 0 = valid join intent
     *    0  = expired or about to expire
     *   -1  = key exists without expiration
     *   -2  = key does not exist
     *
     * Join intents should always have an expiration,
     * so only values greater than 0 are accepted here.
     */
    fun hasValidJoinIntent(
        playerId: UUID
    ): Boolean {

        return getJoinIntentTtl(
            playerId
        ) > 0L
    }

    /*
     * State
     */

    fun isRunning(): Boolean {
        return running
    }

    private fun isAvailable(): Boolean {
        return running &&
                redisManager.isConnected()
    }
}