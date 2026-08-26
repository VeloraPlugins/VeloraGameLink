package online.veloraplugins.gamelink.api.game

data class GameInstance(
    val id: String,
    val type: String,
    val serverId: String,
    val state: String,
    val players: Int,
    val maxPlayers: Int,
    val map: String? = null
) {

    val isFull: Boolean
        get() = players >= maxPlayers

    val hasSpace: Boolean
        get() = !isFull
}