package online.veloraplugins.gamelink.paper.listeners

import online.veloraplugins.engine.message.audience
import online.veloraplugins.gamelink.paper.VeloraGameLinkPlugin
import online.veloraplugins.gamelink.paper.message.GameLinkMessage
import org.bukkit.block.Sign
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent

class GameSignBreakListener(
    private val plugin: VeloraGameLinkPlugin
) : Listener {

    companion object {

        private const val REMOVE_PERMISSION =
            "gamelink.sign.remove"
    }

    /*
     * Remove GameLink sign
     *
     * A registered GameLink sign can only be removed
     * from the GameLink configuration when:
     *
     *   - The player is sneaking
     *   - The player has the required permission
     *
     * Breaking a registered sign without meeting
     * these requirements is cancelled.
     */

    @EventHandler
    fun onBlockBreak(
        event: BlockBreakEvent
    ) {

        val block =
            event.block

        if (block.state !is Sign) {
            return
        }

        val location =
            block.location

        /*
         * Find registered sign
         */

        val signConfig = plugin
            .gameSignsConfig
            .signs
            .firstOrNull { sign ->

                sign.location.world.equals(
                    location.world.name,
                    ignoreCase = true
                ) &&
                    sign.location.x == location.blockX &&
                    sign.location.y == location.blockY &&
                    sign.location.z == location.blockZ
            }
            ?: return

        val player =
            event.player

        /*
         * Require sneak
         */

        if (!player.isSneaking) {
            event.isCancelled = true
            return
        }

        /*
         * Require permission
         */

        if (!player.hasPermission(
                REMOVE_PERMISSION
            )
        ) {

            event.isCancelled = true

            plugin.messages.send(
                player.audience(),
                GameLinkMessage.NO_PERMISSION
            )

            return
        }

        /*
         * Remove config entry
         */

        plugin.gameSignsConfig.signs =
            plugin.gameSignsConfig
                .signs
                .filterNot {
                    it === signConfig
                }

        plugin.gameSignsConfig.save()

        /*
         * Clear active sign assignment
         */

        plugin.gameSignService.clearAssignment(
            location
        )

        /*
         * Refresh remaining signs of this type
         */

        plugin.gameSignService.refreshGameType(
            signConfig.gameType
        )

        plugin.debug(
            "SIGN",
            "Player '${player.name}' removed GameLink sign " +
                "for '${signConfig.gameType}' at " +
                "${location.world.name}:" +
                "${location.blockX}," +
                "${location.blockY}," +
                "${location.blockZ}."
        )

        plugin.messages.send(
            player.audience(),
            GameLinkMessage.SIGN_REMOVED,
            "type" to signConfig.gameType,
            "world" to location.world.name,
            "x" to location.blockX.toString(),
            "y" to location.blockY.toString(),
            "z" to location.blockZ.toString()
        )
    }
}