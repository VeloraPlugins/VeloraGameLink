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
    "  - Display rules use conditions instead of",
    "    hardcoded state mappings.",
    "  - Conditions are evaluated from top to bottom.",
    "  - The first matching condition is used.",
    "  - Sign locations are configured separately.",
    "  - Physical sign materials are never changed.",
    "  - The block behind a sign may be changed.",
    "",
    "========================================",
)
class GameDisplaysConfig : OkaeriConfig() {

    @Comment(
        "Configuration per game type.",
        "",
        "The map key must match the game type used when",
        "registering a game instance."
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
        }
    )

    class GameTypeDisplay : OkaeriConfig() {

        /*
         * Display item
         */

        @Comment(
            "Display item used for this game type",
            "inside GUI menus.",
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

            name =
                "<#00B0FF><bold>BedWars</bold>"

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

        /*
         * Searching display
         */

        @Comment(
            "Fallback physical sign display.",
            "",
            "This display is used when no condition",
            "matches or when no game instance can be",
            "assigned to the sign."
        )
        var searchingForGames = SignDisplay().apply {
            allowJoin = false
            showState = false
            priority = 0

            relativeMaterial =
                "GRAY_CONCRETE"

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

        /*
         * Conditions
         */

        @Comment(
            "Conditional display rules.",
            "",
            "Rules are evaluated from top to bottom.",
            "The first matching condition is used.",
            "",
            "Available placeholders:",
            "  {game}",
            "  {game_id}",
            "  {state}",
            "  {players}",
            "  {max_players}",
            "  {server}",
            "  {map}",
            "",
            "Supported operators:",
            "  ==",
            "  !=",
            "  >",
            "  >=",
            "  <",
            "  <=",
            "  &&",
            "  ||",
            "",
            "Examples:",
            "  {state} == WAITING",
            "  {players} >= {max_players}",
            "  {state} == STARTING && {players} < {max_players}",
            "",
            "Important:",
            "  More specific conditions should be placed",
            "  before more general conditions."
        )
        var conditions = listOf(

            /*
             * FULL
             *
             * This must be before WAITING and STARTING
             * so a full game cannot still be joinable.
             */

            ConditionDisplay().apply {
                condition =
                    "{players} >= {max_players}"

                allowJoin = false
                showState = true
                priority = -5

                relativeMaterial =
                    "PURPLE_CONCRETE"

                signOptions.glow = true
                signOptions.color = "PURPLE"
                signOptions.waxed = true
                signOptions.side = "FRONT"

                lines = listOf(
                    "<gold><bold>BedWars</bold>",
                    "<light_purple>{players}/{max_players}</light_purple>",
                    "<light_purple><bold>FULL</bold></light_purple>",
                    "<gray>Try another game</gray>"
                )
            },

            /*
             * WAITING
             */

            ConditionDisplay().apply {
                condition =
                    "{state} == WAITING"

                allowJoin = true
                showState = true
                priority = 10

                relativeMaterial =
                    "LIME_CONCRETE"

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

            /*
             * STARTING
             */

            ConditionDisplay().apply {
                condition =
                    "{state} == STARTING && {players} < {max_players}"

                allowJoin = true
                showState = true
                priority = 20

                relativeMaterial =
                    "YELLOW_CONCRETE"

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

            /*
             * RUNNING
             */

            ConditionDisplay().apply {
                condition =
                    "{state} == RUNNING"

                allowJoin = false
                showState = false
                priority = 0

                relativeMaterial =
                    "RED_CONCRETE"

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

            /*
             * ENDING
             */

            ConditionDisplay().apply {
                condition =
                    "{state} == ENDING"

                allowJoin = false
                showState = false
                priority = -10

                relativeMaterial =
                    "ORANGE_CONCRETE"

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
            "inside GUI menus."
        )
        var material =
            "PAPER"

        @Comment(
            "Display name of the GUI item.",
            "",
            "MiniMessage formatting is supported."
        )
        var name =
            "<white>{game}</white>"

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
     * Conditional display
     */

    class ConditionDisplay : OkaeriConfig() {

        @Comment(
            "Expression that must evaluate to true",
            "before this display configuration is used.",
            "",
            "Examples:",
            "  {state} == WAITING",
            "  {players} >= {max_players}",
            "  {state} != RUNNING && {players} < {max_players}"
        )
        var condition =
            "{state} == WAITING"

        @Comment(
            "Whether players may join a game matching",
            "this condition."
        )
        var allowJoin =
            false

        @Comment(
            "Whether a game matching this condition",
            "may occupy a physical GameLink sign."
        )
        var showState =
            true

        @Comment(
            "Selection priority.",
            "",
            "Higher values are preferred first."
        )
        var priority =
            0

        @Comment(
            "Material used for the block directly behind",
            "the physical GameLink sign."
        )
        var relativeMaterial =
            "GRAY_CONCRETE"

        @Comment(
            "Physical sign appearance and behaviour."
        )
        var signOptions =
            SignOptions()

        @Comment(
            "The four lines shown on the physical sign.",
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
            "  {map}"
        )
        var lines = listOf(
            "",
            "",
            "",
            ""
        )
    }

    /*
     * Searching display
     */

    class SignDisplay : OkaeriConfig() {

        var allowJoin =
            false

        var showState =
            false

        var priority =
            0

        var relativeMaterial =
            "GRAY_CONCRETE"

        var signOptions =
            SignOptions()

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
            "Whether the sign text should glow."
        )
        var glow =
            true

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
            "  BLACK"
        )
        var color =
            "WHITE"

        @Comment(
            "Whether the physical sign should be waxed."
        )
        var waxed =
            true

        @Comment(
            "Which side of the physical sign is managed.",
            "",
            "Supported values:",
            "  FRONT",
            "  BACK"
        )
        var side =
            "FRONT"
    }
}