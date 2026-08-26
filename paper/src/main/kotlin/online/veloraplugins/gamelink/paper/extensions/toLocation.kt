package online.veloraplugins.gamelink.paper.extensions

import online.veloraplugins.engine.configuration.types.BlockLocationConfig
import org.bukkit.Bukkit
import org.bukkit.Location

fun BlockLocationConfig.toLocation(): Location? {

    val world = Bukkit.getWorld(
        world
    ) ?: return null

    return Location(
        world,
        x.toDouble(),
        y.toDouble(),
        z.toDouble()
    )
}