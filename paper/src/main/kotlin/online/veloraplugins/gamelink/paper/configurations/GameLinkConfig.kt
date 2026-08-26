package online.veloraplugins.gamelink.paper.configurations

import eu.okaeri.configs.OkaeriConfig
import eu.okaeri.configs.annotation.Comment
import eu.okaeri.configs.annotation.Header
import online.veloraplugins.gamelink.api.game.ServerType

@Header(
    "========================================",
    "       VeloraGameLink Settings",
    "========================================",
    "",
    "Permissions:",
    "",
    "Commands:",
    "  gamelink.command",
    "  gamelink.command.reload",
    "  gamelink.command.debug",
    "",
    "========================================",
)
class GameLinkConfig : OkaeriConfig() {

    @Comment(
        "Enable debug logging."
    )
    var debug = false

    @Comment(
        "Settings for this server instance."
    )
    var server = ServerSettings()

    @Comment(
        "Settings for game synchronization."
    )
    var synchronization = SynchronizationSettings()

    class ServerSettings : OkaeriConfig() {

        @Comment(
            "Unique identifier of this Minecraft server instance.",
            "This server may host multiple game instances."
        )
        var id = "games-01"

        @Comment(
            "Type of this server.",
            "Available values: GAME, LOBBY."
        )
        var type = ServerType.GAME
    }

    class SynchronizationSettings : OkaeriConfig() {

        @Comment(
            "How long a pending player join intent remains",
            "available before it automatically expires.",
            "",
            "This should be long enough for the proxy to",
            "transfer the player to the target game server.",
            "",
            "Value is in seconds."
        )
        var joinIntentTimeout = 15L

        @Comment(
            "How often local game instances are published.",
            "Value is in seconds."
        )
        var heartbeatInterval = 5L

        @Comment(
            "How long a game instance may go without an update",
            "before it is considered offline.",
            "Value is in seconds."
        )
        var instanceTimeout = 15L
    }
}