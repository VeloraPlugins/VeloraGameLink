package online.veloraplugins.gamelink.api.events

import online.velora.framework.eventbus.event.Event
import online.veloraplugins.gamelink.api.game.GameInstance

class GameUpdatedEvent(
    val game: GameInstance
) : Event()