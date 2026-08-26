package online.veloraplugins.gamelink.api

import online.veloraplugins.gamelink.api.game.GameInstance
import java.util.UUID

interface VeloraGameLinkApi {

    /*
     * Registration
     *
     * Register and unregister local game instances.
     *
     * Registered games are owned by the current
     * Minecraft server instance and synchronized
     * through VeloraGameLink.
     */

    /*
     * Register a new local game instance.
     *
     * @param id
     * Unique identifier of the game instance.
     *
     * Example:
     *   bedwars-01
     *
     * The id must be unique across the GameLink network.
     *
     * @param type
     * Logical game type.
     *
     * Example:
     *   bedwars
     *   skywars
     *
     * This value is used for selection, displays
     * and QuickJoin.
     *
     * @param state
     * Current custom state of the game.
     *
     * Example:
     *   WAITING
     *   STARTING
     *   RUNNING
     *
     * States are defined by the external game plugin.
     *
     * @param players
     * Current number of players in the game.
     *
     * Must be between 0 and maxPlayers.
     *
     * @param maxPlayers
     * Maximum number of players allowed in the game.
     *
     * Must be greater than zero.
     *
     * @param map
     * Optional map name used by this game instance.
     *
     * Example:
     *   Lighthouse
     *
     * @return
     * Snapshot of the newly registered game instance.
     */
    fun registerGame(
        id: String,
        type: String,
        state: String,
        players: Int,
        maxPlayers: Int,
        map: String? = null
    ): GameInstance

    /*
     * Unregister a local game instance.
     *
     * @param id
     * Unique id of the local game instance.
     *
     * @return
     * true when the game was registered and removed,
     * false when no local game with this id existed.
     */
    fun unregisterGame(
        id: String
    ): Boolean

    /*
     * Updates
     *
     * Update properties of locally registered
     * game instances.
     */

    /*
     * Update the custom state of a game instance.
     *
     * @param id
     * Unique id of the local game instance.
     *
     * @param state
     * New custom game state.
     *
     * Example:
     *   WAITING
     *   STARTING
     *   RUNNING
     *
     * @return
     * true when the game was updated,
     * false when the game was not registered locally.
     */
    fun updateState(
        id: String,
        state: String
    ): Boolean

    /*
     * Update the current player count.
     *
     * @param id
     * Unique id of the local game instance.
     *
     * @param players
     * New player count.
     *
     * Must be between 0 and the game's maxPlayers value.
     *
     * @return
     * true when the game was updated,
     * false when the game was not registered locally.
     */
    fun updatePlayers(
        id: String,
        players: Int
    ): Boolean

    /*
     * Update the current map.
     *
     * @param id
     * Unique id of the local game instance.
     *
     * @param map
     * New map name.
     *
     * Null clears the current map value.
     *
     * @return
     * true when the game was updated,
     * false when the game was not registered locally.
     */
    fun updateMap(
        id: String,
        map: String?
    ): Boolean

    /*
     * Update multiple game properties in one operation.
     *
     * @param id
     * Unique id of the local game instance.
     *
     * @param state
     * Optional new custom state.
     *
     * Null means the state is not changed.
     *
     * @param players
     * Optional new player count.
     *
     * Null means the player count is not changed.
     *
     * @param maxPlayers
     * Optional new maximum player count.
     *
     * Null means maxPlayers is not changed.
     *
     * @param map
     * Optional map value.
     *
     * Note:
     * With this method, null currently means
     * "do not update the map".
     *
     * Use updateMap(id, null) when the map
     * needs to be explicitly cleared.
     *
     * @return
     * true when the game existed and was updated,
     * false when no local game with this id existed.
     */
    fun updateGame(
        id: String,
        state: String? = null,
        players: Int? = null,
        maxPlayers: Int? = null,
        map: String? = null
    ): Boolean

    /*
     * Lookup
     *
     * Read synchronized game instances from
     * the GameLink network.
     */

    /*
     * Find a game instance by its unique id.
     *
     * @param id
     * Unique game instance id.
     *
     * @return
     * Current synchronized GameInstance snapshot,
     * or null when the game does not exist.
     */
    fun getGame(
        id: String
    ): GameInstance?

    /*
     * Return every currently known game instance.
     *
     * This may include games hosted by other
     * Minecraft servers.
     *
     * @return
     * Collection containing all currently available
     * synchronized game instances.
     */
    fun getGames(): Collection<GameInstance>

    /*
     * Return every game instance of a specific type.
     *
     * @param type
     * Logical game type.
     *
     * Example:
     *   bedwars
     *
     * Matching should be case-insensitive.
     *
     * @return
     * Collection containing matching game instances.
     */
    fun getGames(
        type: String
    ): Collection<GameInstance>

    /*
     * Selection
     *
     * Select games according to the configured
     * allowJoin and priority values.
     */

    /*
     * Find the preferred joinable game instance.
     *
     * Games must:
     *   - Exist in the configured game type
     *   - Have a configured state
     *   - Have allowJoin = true
     *   - Have available player slots
     *
     * State priority determines which state is
     * preferred first.
     *
     * Within the same priority, the implementation
     * may prefer the game with the most players.
     *
     * @param type
     * Logical game type to search.
     *
     * Example:
     *   bedwars
     *
     * @return
     * Best matching game instance,
     * or null when none is available.
     */
    fun findBestGame(
        type: String
    ): GameInstance?

    /*
     * Return all currently joinable games
     * of a specific type.
     *
     * @param type
     * Logical game type.
     *
     * @return
     * Joinable game instances ordered according
     * to the configured state priority.
     */
    fun getAvailableGames(
        type: String
    ): Collection<GameInstance>

    /*
     * Local state
     */

    /*
     * Check whether a game instance is registered
     * locally on the current Minecraft server.
     *
     * @param id
     * Unique game instance id.
     *
     * @return
     * true when registered locally,
     * false otherwise.
     */
    fun isRegistered(
        id: String
    ): Boolean

    /*
     * Return all games registered locally by
     * the current Minecraft server.
     *
     * @return
     * Collection of local game snapshots.
     */
    fun getLocalGames(): Collection<GameInstance>

    /*
     * Join intents
     *
     * A join intent identifies which exact game
     * instance an incoming player should enter.
     *
     * This is required because one GAME server
     * may host multiple game instances.
     */

    /*
     * Create or replace a join intent.
     *
     * This should normally be called immediately
     * before transferring a player to the server
     * hosting the target game.
     *
     * @param playerId
     * UUID of the player being transferred.
     *
     * @param gameId
     * Unique id of the target game instance.
     */
    fun createJoinIntent(
        playerId: UUID,
        gameId: String
    )

    /*
     * Read a player's current join intent
     * without removing it.
     *
     * @param playerId
     * UUID of the player.
     *
     * @return
     * Target game instance id,
     * or null when no intent exists.
     */
    fun getJoinIntent(
        playerId: UUID
    ): String?

    /*
     * Read and remove a player's join intent.
     *
     * This should normally be used by the destination
     * GAME server when the player arrives.
     *
     * Consuming the intent prevents it from being
     * processed more than once.
     *
     * @param playerId
     * UUID of the arriving player.
     *
     * @return
     * Target game instance id,
     * or null when no intent exists.
     */
    fun consumeJoinIntent(
        playerId: UUID
    ): String?

    /*
     * Remove a player's join intent without
     * returning its target game id.
     *
     * @param playerId
     * UUID of the player.
     *
     * @return
     * true when an intent existed and was removed,
     * false when no intent existed or Redis
     * was unavailable.
     */
    fun removeJoinIntent(
        playerId: UUID
    ): Boolean

    /*
     * Check whether a join intent exists.
     *
     * This only checks for the Redis key.
     *
     * Use hasValidJoinIntent() when expiration
     * should also be taken into account.
     *
     * @param playerId
     * UUID of the player.
     *
     * @return
     * true when a join intent exists.
     */
    fun hasJoinIntent(
        playerId: UUID
    ): Boolean

    /*
     * Check whether a player has a valid,
     * non-expired join intent.
     *
     * A join intent is valid when its Redis TTL
     * is greater than zero.
     *
     * @param playerId
     * UUID of the player.
     *
     * @return
     * true when the intent exists and has a
     * positive remaining TTL.
     */
    fun hasValidJoinIntent(
        playerId: UUID
    ): Boolean

    /*
     * Return the remaining lifetime of a
     * player's join intent.
     *
     * @param playerId
     * UUID of the player.
     *
     * @return
     * Remaining lifetime in seconds.
     *
     * Redis special values:
     *
     *   -1 = key exists without expiration
     *   -2 = key does not exist
     *
     * The implementation may also return -2
     * when Redis is unavailable.
     */
    fun getJoinIntentTtl(
        playerId: UUID
    ): Long
}