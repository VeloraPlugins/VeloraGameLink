package online.veloraplugins.gamelink.api.events

import online.velora.framework.eventbus.event.Event

class GameRemovedEvent(
    val gameId: String,
    val serverId: String
) : Event()