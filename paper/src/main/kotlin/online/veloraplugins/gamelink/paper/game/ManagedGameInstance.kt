package online.veloraplugins.gamelink.paper.game

import online.veloraplugins.gamelink.api.game.GameInstance

class ManagedGameInstance(
    val id: String,
    val type: String,
    val serverId: String,
    var state: String,
    var players: Int,
    var maxPlayers: Int,
    var map: String? = null
) {

    fun snapshot(): GameInstance {
        return GameInstance(
            id = id,
            type = type,
            serverId = serverId,
            state = state,
            players = players,
            maxPlayers = maxPlayers,
            map = map
        )
    }
}