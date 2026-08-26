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
     * Game item
     */

    @Comment(
        "Appearance of game instance items.",
        "",
        "The material can either be:",
        "  - A fixed material configured here",
        "  - The material configured for the current game state",
        "",
        "When 'use-state-material' is enabled,",
        "the material from game-displays.yml is preferred.",
        "",
        "Available placeholders:",
        "  {game}",
        "  {game_id}",
        "  {players}",
        "  {max_players}",
        "  {state}",
        "  {server}",
        "  {map}",
        "  {priority}"
    )
    var gameItem = GameItem()

    class GameItem : OkaeriConfig() {

        @Comment(
            "Whether the material from the current state's",
            "SignDisplay should be used.",
            "",
            "If no state material can be resolved,",
            "the fallback material below is used."
        )
        var useStateMaterial = true

        @Comment(
            "Fallback item material."
        )
        var material = "PAPER"

        @Comment(
            "Display name of a game item."
        )
        var name = "<#00B0FF><bold>{game}</bold>"

        @Comment(
            "Lore shown on each game item.",
            "",
            "Available placeholders:",
            "  {game}",
            "  {game_id}",
            "  {players}",
            "  {max_players}",
            "  {state}",
            "  {server}",
            "  {map}",
            "  {priority}"
        )
        var lore = listOf(
            "<gray>Instance: <white>{game_id}</white>",
            "<gray>State: <white>{state}</white>",
            "<gray>Players: <white>{players}/{max_players}</white>",
            "<gray>Map: <white>{map}</white>",
            "",
            "<dark_gray>Server: {server}</dark_gray>",
            "",
            "<green>▶ Click to join"
        )
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