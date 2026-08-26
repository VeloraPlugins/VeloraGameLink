package online.veloraplugins.gamelink.paper.configurations

import eu.okaeri.configs.OkaeriConfig
import eu.okaeri.configs.annotation.Comment
import eu.okaeri.configs.annotation.Header
import online.veloraplugins.engine.configuration.types.BlockLocationConfig

@Header(
    "========================================",
    "     VeloraGameLink Sign Locations",
    "========================================",
    "",
    "Configure physical game sign locations.",
    "",
    "Sign appearance is configured in",
    "game-displays.yml.",
    "",
    "========================================",
)
class GameSignsConfig : OkaeriConfig() {

    @Comment(
        "Configured game signs."
    )
    var signs = emptyList<GameSign>()

    class GameSign : OkaeriConfig() {

        @Comment(
            "Game type displayed by this sign."
        )
        var gameType = "bedwars"

        @Comment(
            "Block location of the sign."
        )
        var location = BlockLocationConfig()
    }
}