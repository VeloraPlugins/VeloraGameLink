package online.veloraplugins.gamelink.paper.configurations

import eu.okaeri.configs.OkaeriConfig
import eu.okaeri.configs.annotation.Comment
import eu.okaeri.configs.annotation.Header

@Header(
    "========================================",
    "      VeloraGameLink Game Displays",
    "========================================",
    "",
    "Configure game behaviour, menu displays",
    "and physical sign displays per game type.",
    "",
    "This configuration is used by:",
    "  - Physical game signs",
    "  - Games menus",
    "  - QuickJoin",
    "  - Other game selectors",
    "",
    "Important:",
    "  - Game states are custom strings supplied by",
    "    the game plugin.",
    "  - Sign locations are configured separately.",
    "  - Physical sign materials are never changed.",
    "  - GUI items use their own display item.",
    "  - A relative block behind the sign may be",
    "    changed per game state.",
    "",
    "========================================",
)
class GameDisplaysConfig : OkaeriConfig() {

    @Comment(
        "Configuration per game type.",
        "",
        "The map key must match the game type used when",
        "registering a game instance.",
        "",
        "Example:",
        "  registerGame(type = \"bedwars\", ...)",
        "",
        "must match:",
        "  games:",
        "    bedwars:"
    )
    var games = linkedMapOf(
        "bedwars" to GameTypeDisplay().apply {
            displayItem.material = "RED_BED"
            displayItem.name = "<red><bold>BedWars</bold>"

            displayItem.lore = listOf(
                "<gray>Players: <white>{players}/{max_players}</white>",
                "<gray>State: <white>{state}</white>",
                "<gray>Map: <white>{map}</white>",
                "",
                "<green>▶ Click to join"
            )

            searchingForGames.relativeMaterial =
                "GRAY_CONCRETE"

            states["WAITING"]?.relativeMaterial =
                "LIME_CONCRETE"

            states["STARTING"]?.relativeMaterial =
                "YELLOW_CONCRETE"

            states["RUNNING"]?.relativeMaterial =
                "RED_CONCRETE"

            states["ENDING"]?.relativeMaterial =
                "ORANGE_CONCRETE"
        },

        "skywars" to GameTypeDisplay().apply {
            displayItem.material = "FEATHER"
            displayItem.name = "<aqua><bold>SkyWars</bold>"

            displayItem.lore = listOf(
                "<gray>Players: <white>{players}/{max_players}</white>",
                "<gray>State: <white>{state}</white>",
                "<gray>Map: <white>{map}</white>",
                "",
                "<green>▶ Click to join"
            )

            searchingForGames.relativeMaterial =
                "GRAY_CONCRETE"

            states["WAITING"]?.relativeMaterial =
                "LIGHT_BLUE_CONCRETE"

            states["STARTING"]?.relativeMaterial =
                "YELLOW_CONCRETE"

            states["RUNNING"]?.relativeMaterial =
                "BLUE_CONCRETE"

            states["ENDING"]?.relativeMaterial =
                "ORANGE_CONCRETE"
        },

        "survivalgames" to GameTypeDisplay().apply {
            displayItem.material = "IRON_SWORD"
            displayItem.name = "<gold><bold>Survival Games</bold>"

            displayItem.lore = listOf(
                "<gray>Players: <white>{players}/{max_players}</white>",
                "<gray>State: <white>{state}</white>",
                "<gray>Map: <white>{map}</white>",
                "",
                "<green>▶ Click to join"
            )

            searchingForGames.relativeMaterial =
                "GRAY_CONCRETE"

            states["WAITING"]?.relativeMaterial =
                "LIME_CONCRETE"

            states["STARTING"]?.relativeMaterial =
                "YELLOW_CONCRETE"

            states["RUNNING"]?.relativeMaterial =
                "RED_CONCRETE"

            states["ENDING"]?.relativeMaterial =
                "ORANGE_CONCRETE"
        },

        "duels" to GameTypeDisplay().apply {
            displayItem.material = "DIAMOND_SWORD"
            displayItem.name = "<yellow><bold>Duels</bold>"

            displayItem.lore = listOf(
                "<gray>Players: <white>{players}/{max_players}</white>",
                "<gray>State: <white>{state}</white>",
                "<gray>Map: <white>{map}</white>",
                "",
                "<green>▶ Click to join"
            )

            searchingForGames.relativeMaterial =
                "GRAY_CONCRETE"

            states["WAITING"]?.relativeMaterial =
                "LIME_CONCRETE"

            states["STARTING"]?.relativeMaterial =
                "YELLOW_CONCRETE"

            states["RUNNING"]?.relativeMaterial =
                "RED_CONCRETE"

            states["ENDING"]?.relativeMaterial =
                "ORANGE_CONCRETE"
        }
    )

    class GameTypeDisplay : OkaeriConfig() {

        @Comment(
            "Display item used for this game type in GUI menus.",
            "",
            "This item is independent from physical signs.",
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
        var displayItem = DisplayItem().apply {
            material = "RED_BED"
            name = "<#00B0FF><bold>BedWars</bold>"

            lore = listOf(
                "<gray>Players: <white>{players}/{max_players}</white>",
                "<gray>State: <white>{state}</white>",
                "<gray>Map: <white>{map}</white>",
                "",
                "<dark_gray>Server: {server}</dark_gray>",
                "",
                "<green>▶ Click to join"
            )
        }

        @Comment(
            "Fallback physical sign display.",
            "",
            "Shown when no visible game instance can",
            "currently be assigned to this sign.",
            "",
            "The existing sign block is preserved.",
            "Only the sign text, options and relative",
            "state block are updated."
        )
        var searchingForGames = SignDisplay().apply {
            showState = false
            allowJoin = false
            priority = 0
            relativeMaterial = "GRAY_CONCRETE"

            signOptions.glow = true
            signOptions.color = "GRAY"
            signOptions.waxed = true
            signOptions.side = "FRONT"

            lines = listOf(
                "<gold><bold>BedWars</bold>",
                "",
                "<yellow>Searching...</yellow>",
                "<gray>For games</gray>"
            )
        }

        @Comment(
            "Configuration per custom game state.",
            "",
            "Each map key represents a state supplied by",
            "the external game plugin.",
            "",
            "The state configuration controls:",
            "  - Whether players may join",
            "  - Whether the game may occupy a sign",
            "  - Selection priority",
            "  - Sign text",
            "  - Relative block material",
            "  - Sign appearance and behaviour options",
            "",
            "Unknown states are considered non-joinable",
            "and cannot occupy a physical GameLink sign."
        )
        var states = linkedMapOf(
            "WAITING" to SignDisplay().apply {
                showState = true
                allowJoin = true
                priority = 10
                relativeMaterial = "LIME_CONCRETE"

                signOptions.glow = true
                signOptions.color = "LIME"
                signOptions.waxed = true
                signOptions.side = "FRONT"

                lines = listOf(
                    "<gold><bold>BedWars</bold>",
                    "<yellow>{players}/{max_players}</yellow>",
                    "<green>Waiting</green>",
                    "<white>Click to join</white>"
                )
            },

            "STARTING" to SignDisplay().apply {
                showState = true
                allowJoin = true
                priority = 20
                relativeMaterial = "YELLOW_CONCRETE"

                signOptions.glow = true
                signOptions.color = "YELLOW"
                signOptions.waxed = true
                signOptions.side = "FRONT"

                lines = listOf(
                    "<gold><bold>BedWars</bold>",
                    "<yellow>{players}/{max_players}</yellow>",
                    "<gold>Starting</gold>",
                    "<white>Click to join</white>"
                )
            },

            "RUNNING" to SignDisplay().apply {
                showState = false
                allowJoin = false
                priority = 0
                relativeMaterial = "RED_CONCRETE"

                signOptions.glow = true
                signOptions.color = "RED"
                signOptions.waxed = true
                signOptions.side = "FRONT"

                lines = listOf(
                    "<gold><bold>BedWars</bold>",
                    "<yellow>{players}/{max_players}</yellow>",
                    "<red>In Game</red>",
                    "<gray>{map}</gray>"
                )
            },

            "ENDING" to SignDisplay().apply {
                showState = false
                allowJoin = false
                priority = -10
                relativeMaterial = "ORANGE_CONCRETE"

                signOptions.glow = true
                signOptions.color = "ORANGE"
                signOptions.waxed = true
                signOptions.side = "FRONT"

                lines = listOf(
                    "<gold><bold>BedWars</bold>",
                    "<yellow>{players}/{max_players}</yellow>",
                    "<red>Ending</red>",
                    "<gray>Please wait</gray>"
                )
            }
        )
    }

    /*
     * Display item
     */

    class DisplayItem : OkaeriConfig() {

        @Comment(
            "Minecraft material used for this game type",
            "inside GUI menus.",
            "",
            "Examples:",
            "  RED_BED",
            "  DIAMOND_SWORD",
            "  FEATHER",
            "  ENDER_PEARL",
            "  PAPER"
        )
        var material = "PAPER"

        @Comment(
            "Display name of the GUI item.",
            "",
            "MiniMessage formatting is supported.",
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
        var name = "<white>{game}</white>"

        @Comment(
            "Lore displayed on the GUI item.",
            "",
            "MiniMessage formatting is supported.",
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
            "<gray>Players: <white>{players}/{max_players}</white>",
            "<gray>State: <white>{state}</white>",
            "<gray>Map: <white>{map}</white>",
            "",
            "<green>▶ Click to join"
        )
    }

    /*
     * Sign display
     */

    class SignDisplay : OkaeriConfig() {

        @Comment(
            "Whether players may join game instances",
            "currently using this state.",
            "",
            "Used by:",
            "  - Game sign clicks",
            "  - Games menus",
            "  - QuickJoin",
            "",
            "This setting is independent from show-state."
        )
        var allowJoin = false

        @Comment(
            "Whether games in this state may occupy",
            "physical GameLink signs.",
            "",
            "If disabled:",
            "  - The game is removed from its current sign.",
            "  - The sign becomes available for another game.",
            "  - If no replacement is available, the",
            "    searching-for-games display is shown."
        )
        var showState = true

        @Comment(
            "Selection priority for game instances",
            "currently using this state.",
            "",
            "Higher values are preferred first.",
            "",
            "When two games have the same priority,",
            "the game with the highest player count",
            "is preferred.",
            "",
            "Priority does not make a game joinable."
        )
        var priority = 0

        @Comment(
            "Material used for the block directly behind",
            "the physical GameLink sign.",
            "",
            "VeloraGameLink automatically calculates the",
            "correct relative block from the sign facing.",
            "",
            "The material must be a valid block material.",
            "",
            "Examples:",
            "  GRAY_CONCRETE",
            "  LIME_CONCRETE",
            "  YELLOW_CONCRETE",
            "  RED_CONCRETE",
            "  ORANGE_CONCRETE"
        )
        var relativeMaterial = "GRAY_CONCRETE"

        @Comment(
            "Physical sign behaviour and appearance options.",
            "",
            "These options are applied whenever the sign",
            "is rendered."
        )
        var signOptions = SignOptions()

        @Comment(
            "The four lines displayed on the physical sign.",
            "",
            "Minecraft signs contain exactly four lines.",
            "",
            "MiniMessage formatting is supported.",
            "",
            "Available placeholders:",
            "  {game}",
            "    Game type.",
            "",
            "  {game_id}",
            "    Unique game instance id.",
            "",
            "  {players}",
            "    Current player count.",
            "",
            "  {max_players}",
            "    Maximum player count.",
            "",
            "  {state}",
            "    Current custom game state.",
            "",
            "  {server}",
            "    Server hosting the game.",
            "",
            "  {map}",
            "    Current map name."
        )
        var lines = listOf(
            "",
            "",
            "",
            ""
        )
    }

    /*
     * Sign options
     */

    class SignOptions : OkaeriConfig() {

        @Comment(
            "Whether the sign text should glow.",
            "",
            "Glow is applied to the configured sign side."
        )
        var glow = true

        @Comment(
            "Vanilla dye color applied to the sign text.",
            "",
            "Supported values:",
            "  WHITE",
            "  ORANGE",
            "  MAGENTA",
            "  LIGHT_BLUE",
            "  YELLOW",
            "  LIME",
            "  PINK",
            "  GRAY",
            "  LIGHT_GRAY",
            "  CYAN",
            "  PURPLE",
            "  BLUE",
            "  BROWN",
            "  GREEN",
            "  RED",
            "  BLACK",
            "",
            "This is the vanilla sign dye color.",
            "MiniMessage colors inside the configured",
            "lines are handled separately."
        )
        var color = "WHITE"

        @Comment(
            "Whether the physical sign should be waxed.",
            "",
            "Waxed signs cannot normally be edited",
            "by players."
        )
        var waxed = true

        @Comment(
            "Which side of the physical sign is managed",
            "by VeloraGameLink.",
            "",
            "Supported values:",
            "  FRONT",
            "  BACK",
            "",
            "The configured lines, glow and dye color",
            "are applied to this side."
        )
        var side = "FRONT"
    }
}