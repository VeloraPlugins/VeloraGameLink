package online.veloraplugins.gamelink.paper.message

import online.velora.framework.language.MessageKey

enum class GameLinkMessage(
    override val key: String,
    override val default: String
) : MessageKey {

    HELP(
        "help",
        """
    {prefix}<gradient:#00B0FF:#2979FF><b>VeloraGameLink</b></gradient>
    
    <gray>Commands:</gray>
    
    <yellow>/gamelink reload</yellow> <dark_gray>-</dark_gray> <white>Reload the plugin.</white>
    <yellow>/gamelink debug</yellow> <dark_gray>-</dark_gray> <white>Show debug status.</white>
    <yellow>/gamelink games</yellow> <dark_gray>-</dark_gray> <white>Show registered game instances.</white>
    <yellow>/gamelink menu [gameType]</yellow> <dark_gray>-</dark_gray> <white>Open the games menu.</white>
    <yellow>/gamelink quickjoin <gameType></yellow> <dark_gray>-</dark_gray> <white>Join the best available game.</white>
    <yellow>/gamelink manage</yellow> <dark_gray>-</dark_gray> <white>Show management commands.</white>
    """.trimIndent()
    ),

    MANAGE_HELP(
        "manage_help",
        """
    {prefix}<gradient:#00B0FF:#2979FF><b>Game Management</b></gradient>
    
    <gray>Commands:</gray>
    
    <yellow>/gamelink manage register <id> <type> <state> <players> <maxPlayers> [map]</yellow>
    <dark_gray>-</dark_gray> <white>Register a game instance.</white>
    
    <yellow>/gamelink manage unregister <id></yellow>
    <dark_gray>-</dark_gray> <white>Unregister a game instance.</white>
    
    <yellow>/gamelink manage state <id> <state></yellow>
    <dark_gray>-</dark_gray> <white>Update a game state.</white>
    
    <yellow>/gamelink manage players <id> <players></yellow>
    <dark_gray>-</dark_gray> <white>Update a game player count.</white>
    
    <yellow>/gamelink manage map <id> <map></yellow>
    <dark_gray>-</dark_gray> <white>Update a game map.</white>
    
    <yellow>/gamelink manage clearmap <id></yellow>
    <dark_gray>-</dark_gray> <white>Clear a game map.</white>
    
    <yellow>/gamelink manage info <id></yellow>
    <dark_gray>-</dark_gray> <white>Show information about a game instance.</white>
    
    <yellow>/gamelink manage addsign <gameType></yellow>
    <dark_gray>-</dark_gray> <white>Register the sign you are looking at.</white>
    
    <yellow>/gamelink manage removesign</yellow>
    <dark_gray>-</dark_gray> <white>Remove the registered sign you are looking at.</white>
    """.trimIndent()
    ),

    RELOAD_SUCCESS(
        "reload_success",
        "{prefix}<green>VeloraGameLink configuration reloaded."
    ),

    DEBUG_STATUS(
        "debug_status",
        "{prefix}<gray>Debug: <white>{status}</white>"
    ),

    NO_PERMISSION(
        "no_permission",
        "{prefix}<red>You do not have permission to do that."
    ),

    ONLY_PLAYERS(
        "only_players",
        "{prefix}<red>This command can only be used by players."
    ),

    GAME_NOT_FOUND(
        "game_not_found",
        "{prefix}<red>Game instance <white>{game}</white> could not be found."
    ),

    GAME_FULL(
        "game_full",
        "{prefix}<red>Game <white>{game}</white> is full."
    ),

    GAME_NOT_JOINABLE(
        "game_not_joinable",
        "{prefix}<red>Game <white>{game}</white> cannot be joined in state <white>{state}</white>."
    ),

    GAME_ALREADY_REGISTERED(
        "game_already_registered",
        "{prefix}<red>Game instance <white>{game}</white> is already registered."
    ),

    GAME_STATE_UPDATED(
        "game_state_updated",
        "{prefix}<green>Updated game <white>{game}</white> state to <white>{state}</white>."
    ),

    GAME_PLAYERS_UPDATED(
        "game_players_updated",
        "{prefix}<green>Updated game <white>{game}</white> player count to <white>{players}</white>."
    ),

    GAME_MAP_UPDATED(
        "game_map_updated",
        "{prefix}<green>Updated game <white>{game}</white> map to <white>{map}</white>."
    ),

    GAME_MAP_CLEARED(
        "game_map_cleared",
        "{prefix}<green>Cleared the map for game <white>{game}</white>."
    ),

    GAME_OPERATION_FAILED(
        "game_operation_failed",
        "{prefix}<red>Unable to update game <white>{game}</white>: <gray>{error}</gray>"
    ),

    GAME_INFO(
        "game_info",
        """
    {prefix}<gradient:#00B0FF:#2979FF><b>{id}</b></gradient>
    <gray>Type: <white>{type}</white>
    <gray>Server: <white>{server}</white>
    <gray>State: <white>{state}</white>
    <gray>Players: <white>{players}/{max_players}</white>
    <gray>Map: <white>{map}</white>
    """.trimIndent()
    ),

    SIGN_NOT_FOUND(
        "sign_not_found",
        "{prefix}<red>You must look at a sign."
    ),

    SIGN_ALREADY_EXISTS(
        "sign_already_exists",
        "{prefix}<red>This sign is already registered."
    ),

    GAME_TYPE_NOT_FOUND(
        "game_type_not_found",
        "{prefix}<red>Game type <white>{type}</white> is not configured."
    ),

    SIGN_ADDED(
        "sign_added",
        "{prefix}<green>Added sign for <white>{type}</white> at <white>{world} {x} {y} {z}</white>."
    ),

    SIGN_NOT_REGISTERED(
        "sign_not_registered",
        "{prefix}<red>This sign is not registered."
    ),

    SIGN_REMOVED(
        "sign_removed",
        "{prefix}<green>Removed <white>{type}</white> sign at <white>{world} {x} {y} {z}</white>."
    ),

    JOINING_GAME(
        "joining_game",
        "{prefix}<green>Joining <white>{game}</white>..."
    ),

    JOIN_ALREADY_PENDING(
        "join_already_pending",
        "{prefix}<red>You already have a pending join request for <white>{game}</white>."
    ),

    NO_AVAILABLE_GAME(
        "no_available_game",
        "{prefix}<red>No available <white>{type}</white> game could be found."
    ),

    GAME_REGISTERED(
        "game_registered",
        "{prefix}<green>Registered game <white>{game}</white>."
    ),

    GAME_UNREGISTERED(
        "game_unregistered",
        "{prefix}<green>Unregistered game <white>{game}</white>."
    ),

    GAMES_HEADER(
        "games_header",
        """
        {prefix}<gradient:#00B0FF:#2979FF><b>Game Instances</b></gradient>
        
        <gray>Registered games:</gray>
        """.trimIndent()
    ),

    GAMES_ENTRY(
        "games_entry",
        "<dark_gray>-</dark_gray> <yellow>{id}</yellow> <gray>[{type}]</gray> <white>{players}/{max_players}</white> <dark_gray>-</dark_gray> <gray>{state}</gray> <dark_gray>@</dark_gray> <white>{server}</white>"
    ),

    GAMES_EMPTY(
        "games_empty",
        "{prefix}<gray>No game instances are currently available."
    ),

    REDIS_UNAVAILABLE(
        "redis_unavailable",
        "{prefix}<red>Redis is currently unavailable."
    ),

    INVALID_SERVER_TYPE(
        "invalid_server_type",
        "{prefix}<red>This action is not available on a <white>{type}</white> server."
    )
}