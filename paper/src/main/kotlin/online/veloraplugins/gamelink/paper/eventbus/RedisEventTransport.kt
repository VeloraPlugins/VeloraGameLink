package online.veloraplugins.gamelink.paper.eventbus

import online.velora.framework.eventbus.transport.EventTransport
import online.velora.framework.redis.RedisManager

class RedisEventTransport(
    private val redisManager: RedisManager,
    private val channel: String,
    private val warn: (String) -> Unit = {}
) : EventTransport {

    override fun publish(
        eventJson: String
    ) {

        if (!redisManager.isConnected()) {
            warn(
                "Unable to publish EventBus event: Redis is not connected."
            )
            return
        }

        redisManager.publish(
            channel,
            eventJson
        )
    }

    override fun subscribe(
        onMessage: (String) -> Unit
    ) {

        if (!redisManager.isConnected()) {
            warn(
                "Unable to subscribe EventBus transport: Redis is not connected."
            )
            return
        }

        redisManager.subscribe(
            channel,
            onMessage
        )
    }
}