package online.veloraplugins.gamelink.paper.services

import online.veloraplugins.gamelink.api.game.GameInstance
import online.veloraplugins.gamelink.paper.VeloraGameLinkPlugin
import online.veloraplugins.gamelink.paper.game.ManagedGameInstance
import java.util.concurrent.ConcurrentHashMap

class GameInstanceService(
    private val plugin: VeloraGameLinkPlugin
) {

    private val instances =
        ConcurrentHashMap<String, ManagedGameInstance>()

    fun register(
        id: String,
        type: String,
        state: String,
        players: Int,
        maxPlayers: Int,
        map: String? = null
    ): ManagedGameInstance {

        require(id.isNotBlank()) {
            "Game instance id cannot be blank."
        }

        require(type.isNotBlank()) {
            "Game type cannot be blank."
        }

        require(state.isNotBlank()) {
            "Game state cannot be blank."
        }

        require(maxPlayers > 0) {
            "Max players must be greater than 0."
        }

        require(players in 0..maxPlayers) {
            "Players must be between 0 and maxPlayers."
        }

        val instance = ManagedGameInstance(
            id = id,
            type = type,
            serverId = plugin.pluginConfig.server.id,
            state = state,
            players = players,
            maxPlayers = maxPlayers,
            map = map
        )

        check(
            instances.putIfAbsent(
                id,
                instance
            ) == null
        ) {
            "Game instance '$id' is already registered."
        }

        plugin.debug(
            "GAME",
            "Registered game '$id' " +
                    "(type='$type', state='$state', " +
                    "players=$players/$maxPlayers) " +
                    "on server '${instance.serverId}'."
        )

        return instance
    }

    fun unregister(
        id: String
    ): ManagedGameInstance? {

        val instance = instances.remove(
            id
        )

        if (instance == null) {

            plugin.debug(
                "GAME",
                "Unable to unregister game '$id': instance not found."
            )

            return null
        }

        plugin.debug(
            "GAME",
            "Unregistered game '$id' " +
                    "(type='${instance.type}') " +
                    "from server '${instance.serverId}'."
        )

        return instance
    }

    fun getManaged(
        id: String
    ): ManagedGameInstance? {
        return instances[id]
    }

    fun get(
        id: String
    ): GameInstance? {
        return instances[id]
            ?.snapshot()
    }

    fun getAll(): List<GameInstance> {
        return instances.values
            .map {
                it.snapshot()
            }
    }

    fun getByType(
        type: String
    ): List<GameInstance> {

        return instances.values
            .asSequence()
            .filter {
                it.type.equals(
                    type,
                    ignoreCase = true
                )
            }
            .map {
                it.snapshot()
            }
            .toList()
    }

    fun updateState(
        id: String,
        state: String
    ): Boolean {

        require(state.isNotBlank()) {
            "Game state cannot be blank."
        }

        val instance = instances[id]

        if (instance == null) {

            plugin.debug(
                "GAME",
                "Unable to update state for game '$id': instance not found."
            )

            return false
        }

        val previousState = instance.state

        instance.state = state

        plugin.debug(
            "GAME",
            "Updated game '$id' state: '$previousState' -> '$state'."
        )

        return true
    }

    fun updatePlayers(
        id: String,
        players: Int
    ): Boolean {

        val instance = instances[id]

        if (instance == null) {

            plugin.debug(
                "GAME",
                "Unable to update players for game '$id': instance not found."
            )

            return false
        }

        require(players in 0..instance.maxPlayers) {
            "Players must be between 0 and ${instance.maxPlayers}."
        }

        val previousPlayers = instance.players

        instance.players = players

        plugin.debug(
            "GAME",
            "Updated game '$id' players: " +
                    "$previousPlayers/${instance.maxPlayers} -> " +
                    "$players/${instance.maxPlayers}."
        )

        return true
    }

    fun updateMap(
        id: String,
        map: String?
    ): Boolean {

        val instance = instances[id]

        if (instance == null) {

            plugin.debug(
                "GAME",
                "Unable to update map for game '$id': instance not found."
            )

            return false
        }

        val previousMap = instance.map

        instance.map = map

        plugin.debug(
            "GAME",
            "Updated game '$id' map: " +
                    "'${previousMap ?: "none"}' -> '${map ?: "none"}'."
        )

        return true
    }

    fun size(): Int {
        return instances.size
    }

    fun unregisterAll() {

        val count = instances.size

        instances.clear()

        plugin.debug(
            "GAME",
            "Unregistered $count local game instance(s)."
        )
    }
}