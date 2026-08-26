package online.veloraplugins.gamelink.paper.configurations

import eu.okaeri.configs.OkaeriConfig
import eu.okaeri.configs.annotation.Comment
import eu.okaeri.configs.annotation.Header

@Header(
    "========================================",
    "      VeloraGameLink Game Displays",
    "========================================",
    "",
    "Configure game behaviour and sign displays",
    "per game type.",
    "",
    "This configuration is used by:",
    "  - Physical game signs",
    "  - QuickJoin NPCs",
    "",
    "Important:",
    "  - Game states are custom strings supplied by",
    "    the game plugin.",
    "  - Sign locations are configured separately.",
    "  - QuickJoin NPCs do not use sign text or",
    "    sign materials.",
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
        "bedwars" to GameTypeDisplay()
    )

    class GameTypeDisplay : OkaeriConfig() {

        @Comment(
            "Fallback sign display.",
            "",
            "This display is shown when a configured sign",
            "cannot currently be assigned a valid game.",
            "",
            "This can happen when:",
            "  - No game instances are available.",
            "  - All available games are full.",
            "  - A game's current state has no configured",
            "    sign display.",
            "  - A previously assigned game disappeared",
            "    from Redis.",
            "",
            "This display is only used by physical signs.",
            "QuickJoin NPCs do not use it."
        )
        var searchingForGames = SignDisplay().apply {
            material = "OAK_SIGN"

            lines = listOf(
                "<gold><bold>BedWars</bold>",
                "",
                "<yellow>Searching...</yellow>",
                "<gray>For games</gray>"
            )
        }

        @Comment(
            "Sign display configuration per game state.",
            "",
            "Each entry represents one custom state supplied",
            "by the game plugin.",
            "",
            "Example:",
            "  states:",
            "    WAITING:",
            "    STARTING:",
            "    RUNNING:",
            "",
            "When a game's state matches one of these entries,",
            "the corresponding sign display is used.",
            "",
            "If the current game state is not configured here,",
            "the searching-for-games display is shown instead.",
            "",
            "The priority value is used when VeloraGameLink",
            "needs to choose between multiple game instances.",
            "",
            "Higher priority values are preferred first.",
            "When multiple games have the same priority,",
            "the game with the highest player count is preferred.",
            "",
            "QuickJoin NPCs use the priority from these states",
            "when selecting the best joinable game, but they",
            "do not use the sign material or sign lines."
        )
        var states = linkedMapOf(
            "WAITING" to SignDisplay().apply {
                showState = true
                allowJoin = true
                priority = 10
                material = "OAK_SIGN"

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
                material = "SPRUCE_SIGN"

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
                material = "DARK_OAK_SIGN"

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
                material = "BIRCH_SIGN"

                lines = listOf(
                    "<gold><bold>BedWars</bold>",
                    "<yellow>{players}/{max_players}</yellow>",
                    "<red>Ending</red>",
                    "<gray>Please wait</gray>"
                )
            }
        )
    }

    class SignDisplay : OkaeriConfig() {

        @Comment(
            "Whether players are allowed to join game instances",
            "currently using this state.",
            "",
            "This setting is used by:",
            "  - Game sign clicks",
            "  - QuickJoin NPCs",
            "",
            "If disabled, the game may still be displayed",
            "on signs, but players cannot join it."
        )
        var allowJoin = false

        @Comment(
            "Whether game instances in this state should",
            "remain visible on game signs.",
            "",
            "If enabled:",
            "  - The game may stay assigned to a sign.",
            "  - The configured material and lines are shown.",
            "",
            "If disabled:",
            "  - The game is removed from the sign assignment.",
            "  - The sign becomes available for another game.",
            "  - If no replacement game is available, the",
            "    searching-for-games display is shown.",
            "",
            "This setting only affects physical game signs."
        )
        var showState = true

        @Comment(
            "Selection priority for game instances",
            "currently using this state.",
            "",
            "Higher values are preferred first.",
            "",
            "Example:",
            "  STARTING = 20",
            "  WAITING  = 10",
            "  RUNNING  = 0",
            "",
            "If two games have the same priority,",
            "the game with the most players is preferred.",
            "",
            "Priority does not make a game joinable.",
            "Joinability is controlled by 'allow-join'."
        )
        var priority = 0

        @Comment(
            "Minecraft sign material used while a game",
            "is displayed in this state.",
            "",
            "This setting is only used by physical signs.",
            "QuickJoin NPCs ignore this value."
        )
        var material = "OAK_SIGN"

        @Comment(
            "The four lines displayed on the sign.",
            "",
            "Minecraft signs contain exactly four lines.",
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
}