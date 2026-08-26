package online.veloraplugins.gamelink.paper.configurations

import eu.okaeri.configs.OkaeriConfig
import eu.okaeri.configs.annotation.Comment
import eu.okaeri.configs.annotation.Header

@Header(
    "========================================",
    "        VeloraGameLink Games Menu",
    "========================================",
    "",
    "Configure the appearance of the games menu.",
    "",
    "This menu can show:",
    "  - All available game instances",
    "  - Only one specific game type",
    "",
    "MiniMessage formatting is supported.",
    "",
    "========================================"
)
class GamesMenuConfig : OkaeriConfig() {

    /*
     * General
     */

    @Comment(
        "General menu settings."
    )
    var menu = MenuSettings()

    class MenuSettings : OkaeriConfig() {

        @Comment(
            "Amount of inventory rows.",
            "",
            "Valid values:",
            "  1 - 6",
            "",
            "Recommended:",
            "  6"
        )
        var rows = 6

        @Comment(
            "Menu title when all game types are shown.",
            "",
            "MiniMessage formatting is supported."
        )
        var title = "<gradient:#00B0FF:#2979FF><bold>Games</bold></gradient>"

        @Comment(
            "Menu title when only one game type is shown.",
            "",
            "Available placeholders:",
            "  {game}",
            "",
            "Example:",
            "  {game} = Bedwars"
        )
        var gameTitle = "<gradient:#00B0FF:#2979FF><bold>{game}</bold></gradient>"
    }

    /*
     * Empty item
     */

    @Comment(
        "Item shown when no joinable games are available."
    )
    var emptyItem = EmptyItem()

    class EmptyItem : OkaeriConfig() {

        var material = "BARRIER"

        var name = "<red><bold>NO GAMES AVAILABLE</bold>"

        @Comment(
            "Lore when all game types are being shown."
        )
        var lore = listOf(
            "<gray>There are currently no games available."
        )

        @Comment(
            "Lore when the menu is filtered by game type.",
            "",
            "Available placeholders:",
            "  {game}"
        )
        var gameLore = listOf(
            "<gray>There are currently no {game} games available."
        )
    }

    /*
     * Border
     */

    @Comment(
        "Border filler configuration."
    )
    var border = BorderItem()

    class BorderItem : OkaeriConfig() {

        var enabled = true

        var material = "GRAY_STAINED_GLASS_PANE"

        var name = ""
    }

    /*
     * Previous
     */

    @Comment(
        "Previous page button."
    )
    var previous = NavigationItem().apply {
        slot = 48
        material = "ARROW"
        name = "<yellow><bold>PREVIOUS</bold>"
        lore = listOf(
            "<gray>Go to the previous page."
        )
    }

    /*
     * Refresh
     */

    @Comment(
        "Refresh button."
    )
    var refresh = NavigationItem().apply {
        slot = 49
        material = "SUNFLOWER"
        name = "<green><bold>REFRESH</bold>"
        lore = listOf(
            "<gray>Refresh available games.",
            "",
            "<yellow>▶ Click to refresh"
        )
    }

    /*
     * Next
     */

    @Comment(
        "Next page button."
    )
    var next = NavigationItem().apply {
        slot = 50
        material = "ARROW"
        name = "<yellow><bold>NEXT</bold>"
        lore = listOf(
            "<gray>Go to the next page."
        )
    }

    /*
     * Close
     */

    @Comment(
        "Close menu button."
    )
    var close = NavigationItem().apply {
        slot = 53
        material = "BARRIER"
        name = "<red><bold>CLOSE</bold>"
        lore = listOf(
            "<gray>Close this menu."
        )
    }

    class NavigationItem : OkaeriConfig() {

        @Comment(
            "Inventory slot of this button.",
            "",
            "Slots are zero-based.",
            "",
            "For a 6 row inventory:",
            "  First slot = 0",
            "  Last slot  = 53"
        )
        var slot = 0

        var material = "PAPER"

        var name = ""

        var lore = emptyList<String>()
    }
}