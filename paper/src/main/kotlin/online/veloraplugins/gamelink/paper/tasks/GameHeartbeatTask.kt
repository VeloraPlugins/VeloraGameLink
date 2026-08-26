package online.veloraplugins.gamelink.paper.tasks

import com.github.shynixn.mccoroutine.bukkit.scope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import online.veloraplugins.engine.extensions.safeCancel
import online.veloraplugins.gamelink.api.game.ServerType
import online.veloraplugins.gamelink.paper.VeloraGameLinkPlugin
import kotlin.time.Duration.Companion.seconds

class GameHeartbeatTask(
    private val plugin: VeloraGameLinkPlugin
) {

    private var job: Job? = null

    fun start() {

        if (job != null) {
            return
        }

        val interval = plugin
            .pluginConfig
            .synchronization
            .heartbeatInterval
            .seconds

        job = plugin.scope.launch {

            while (isActive) {

                when (plugin.pluginConfig.server.type) {

                    ServerType.GAME -> {
                        plugin.gameRedisService.heartbeat()
                    }

                    ServerType.LOBBY -> {
                        plugin.gameSelectorService.refresh()
                    }
                }

                delay(interval)
            }
        }

        plugin.debug(
            "TASK",
            "Started synchronization task every $interval for ${plugin.pluginConfig.server.type}."
        )
    }

    fun stop() {

        job?.safeCancel()
        job = null

        plugin.debug(
            "TASK",
            "Stopped synchronization task."
        )
    }
}