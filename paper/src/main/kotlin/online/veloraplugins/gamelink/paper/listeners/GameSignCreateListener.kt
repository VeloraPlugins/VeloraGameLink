package online.veloraplugins.gamelink.paper.listeners

import net.kyori.adventure.text.Component
import online.velora.framework.adventure.ComponentUtil
import online.veloraplugins.gamelink.paper.VeloraGameLinkPlugin
import online.veloraplugins.gamelink.paper.configurations.GameSignsConfig
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.SignChangeEvent

class GameSignCreateListener(
    private val plugin: VeloraGameLinkPlugin
) : Listener {

    /*
     * Create GameLink sign
     *
     * Sign format:
     *
     *   [gamelink]
     *   <gameType>
     *
     * Example:
     *
     *   [gamelink]
     *   bedwars
     */

    @EventHandler
    fun onSignChange(
        event: SignChangeEvent
    ) {

        /*
         * Header
         */

        val header = ComponentUtil.serialize(
            event.line(
                0
            ) ?: Component.empty()
        ).trim()

        if (!header.equals(
                "[gamelink]",
                ignoreCase = true
            )
        ) {
            return
        }

        /*
         * Game type
         */

        val gameType = ComponentUtil.serialize(
            event.line(
                1
            ) ?: Component.empty()
        ).trim()

        if (gameType.isBlank()) {

            plugin.debug(
                "SIGN",
                "Unable to create GameLink sign: game type is blank."
            )

            return
        }

        /*
         * Verify configured game type
         */

        val configuredGameType = plugin
            .gameDisplaysConfig
            .games
            .keys
            .firstOrNull {
                it.equals(
                    gameType,
                    ignoreCase = true
                )
            }
            ?: run {

                plugin.debug(
                    "SIGN",
                    "Unable to create GameLink sign: " +
                            "game type '$gameType' is not configured."
                )

                return
            }

        /*
         * Location
         */

        val location =
            event.block.location

        /*
         * Prevent duplicate registrations
         */

        val alreadyRegistered = plugin
            .gameSignsConfig
            .signs
            .any { sign ->

                sign.location.world.equals(
                    location.world.name,
                    ignoreCase = true
                ) &&
                        sign.location.x == location.blockX &&
                        sign.location.y == location.blockY &&
                        sign.location.z == location.blockZ
            }

        if (alreadyRegistered) {

            plugin.debug(
                "SIGN",
                "GameLink sign at " +
                        "${location.world.name}:" +
                        "${location.blockX}," +
                        "${location.blockY}," +
                        "${location.blockZ} " +
                        "is already registered."
            )

            return
        }

        /*
         * Create config entry
         */

        val signConfig =
            GameSignsConfig.GameSign().apply {

                this.gameType =
                    configuredGameType

                this.location.world =
                    location.world.name

                this.location.x =
                    location.blockX

                this.location.y =
                    location.blockY

                this.location.z =
                    location.blockZ
            }

        /*
         * Save sign
         */

        plugin.gameSignsConfig.signs += signConfig

        plugin.gameSignsConfig.save()

        plugin.debug(
            "SIGN",
            "Registered GameLink sign for '$configuredGameType' at " +
                    "${location.world.name}:" +
                    "${location.blockX}," +
                    "${location.blockY}," +
                    "${location.blockZ}."
        )

        /*
         * Refresh sign
         *
         * SignChangeEvent fires before the sign block
         * has completely applied its new block state.
         *
         * Schedule the refresh on the region owning
         * this sign location.
         */

        Bukkit.getRegionScheduler().execute(
            plugin,
            event.block.location
        ) {

            plugin.gameSignService.refreshGameType(
                configuredGameType
            )
        }
    }
}