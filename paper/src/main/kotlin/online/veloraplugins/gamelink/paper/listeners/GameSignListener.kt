package online.veloraplugins.gamelink.paper.listeners

import online.veloraplugins.gamelink.api.game.GameJoinResult
import online.veloraplugins.gamelink.paper.VeloraGameLinkPlugin
import online.veloraplugins.gamelink.api.game.ServerType
import org.bukkit.block.Sign
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent

class GameSignListener(
    private val plugin: VeloraGameLinkPlugin
) : Listener {

    @EventHandler
    fun onInteract(
        event: PlayerInteractEvent
    ) {

        if (
            event.action != Action.RIGHT_CLICK_BLOCK &&
            event.action != Action.LEFT_CLICK_BLOCK
        ) {
            return
        }

        val block = event.clickedBlock
            ?: return

        if (block.state !is Sign) {
            return
        }

        val game = plugin
            .gameSignService
            .getAssignedGame(
                block.location
            )
            ?: return

        event.isCancelled = true

        plugin.debug(
            "SIGN",
            "Player '${event.player.name}' clicked sign " +
                    "for game '${game.id}'."
        )

        val result = plugin.gameJoinService.join(
            player = event.player,
            game = game
        )

        if (result != GameJoinResult.SUCCESS) {

            plugin.debug(
                "SIGN",
                "Join failed for game '${game.id}' with result '$result'. Refreshing signs."
            )

            plugin.gameSignService.refreshGameType(
                game.type
            )
        }
    }
}