# VeloraGameLink

VeloraGameLink is a Paper plugin for linking lobby servers and game servers across the Velora network.

It is designed for setups where a single Minecraft server may host multiple game instances at the same time.

VeloraGameLink provides:

- Network-wide game instance synchronization through Redis
- Realtime game updates through the Velora EventBus
- Lobby game signs
- Joinable game selection
- QuickJoin support
- Game management commands
- PlaceholderAPI support
- A public API for external game plugins
- Join intents so destination game servers know which exact game instance a player should enter

---

## Server Types

VeloraGameLink supports two server types:

### `GAME`

A `GAME` server hosts one or more game instances.

Examples:

```text
games-01
├─ bedwars-01
├─ bedwars-02
└─ bedwars-03
```

Game plugins register their instances through the VeloraGameLink API.

VeloraGameLink publishes the instance state to Redis and keeps it alive using heartbeat updates.

### `LOBBY`

A `LOBBY` server consumes game data from Redis.

Lobby servers may provide:

- Game signs
- Games menus
- QuickJoin
- PlaceholderAPI placeholders
- Player transfer to game servers

---

## Game Instances

A game instance is represented by:

```kotlin
data class GameInstance(
    val id: String,
    val type: String,
    val serverId: String,
    val state: String,
    val players: Int,
    val maxPlayers: Int,
    val map: String? = null
)
```

### Fields

| Field | Description |
|---|---|
| `id` | Unique game instance identifier, for example `bedwars-01` |
| `type` | Logical game type, for example `bedwars` |
| `serverId` | Minecraft server hosting the game, for example `games-01` |
| `state` | Custom state supplied by the game plugin |
| `players` | Current player count |
| `maxPlayers` | Maximum player count |
| `map` | Optional map name |

Game states are custom strings.

Examples:

```text
WAITING
STARTING
RUNNING
ENDING
RESETTING
```

VeloraGameLink does not force a game state enum.

---

## Redis Structure

Game instances are stored in Redis.

Example keys:

```text
velora:gamelink:game:<gameId>
velora:gamelink:index:games
velora:gamelink:index:type:<type>
velora:gamelink:index:server:<serverId>
velora:gamelink:join:<playerUuid>
```

Game snapshots receive a TTL.

If a game server crashes and stops sending heartbeat updates, its game instances automatically expire.

---

## Game Display Configuration

Game state behavior is configured in `game-displays.yml`.

Each state can define:

```yaml
states:
  WAITING:
    show-state: true
    allow-join: true
    priority: 10
    material: OAK_SIGN
    lines:
      - "<gold><bold>BedWars</bold>"
      - "<yellow>{players}/{max_players}</yellow>"
      - "<green>Waiting</green>"
      - "<white>Click to join</white>"

  STARTING:
    show-state: true
    allow-join: true
    priority: 20
    material: SPRUCE_SIGN
    lines:
      - "<gold><bold>BedWars</bold>"
      - "<yellow>{players}/{max_players}</yellow>"
      - "<gold>Starting</gold>"
      - "<white>Click to join</white>"

  RUNNING:
    show-state: false
    allow-join: false
    priority: 0
    material: DARK_OAK_SIGN
    lines:
      - "<gold><bold>BedWars</bold>"
      - "<yellow>{players}/{max_players}</yellow>"
      - "<red>In Game</red>"
      - "<gray>{map}</gray>"
```

### `show-state`

Controls whether a game in this state may occupy a lobby sign.

```yaml
show-state: true
```

The game may stay allocated to a sign.

```yaml
show-state: false
```

The sign assignment is removed immediately.

The sign can then be allocated to another game.

If no replacement exists, the `searching-for-games` display is shown.

### `allow-join`

Controls whether players may join games in this state.

```yaml
allow-join: true
```

The game may be selected by QuickJoin, menus and sign clicks.

```yaml
allow-join: false
```

The game cannot be joined.

`show-state` and `allow-join` are intentionally separate.

A game may therefore be visible but not joinable.

### `priority`

Controls game selection order.

Higher values are preferred first.

Example:

```text
STARTING = 20
WAITING  = 10
RUNNING  = 0
```

When multiple games have the same priority, the game with the highest player count is preferred.

---

## Public API

Access the API through:

```kotlin
val gameLink = VeloraGameLinkProvider.get()
```

You can safely check availability first:

```kotlin
if (!VeloraGameLinkProvider.isAvailable()) {
    return
}

val gameLink = VeloraGameLinkProvider.get()
```

---

## Registering a Game

```kotlin
val game = VeloraGameLinkProvider.get().registerGame(
    id = "bedwars-01",
    type = "bedwars",
    state = "WAITING",
    players = 0,
    maxPlayers = 16,
    map = "Lighthouse"
)
```

The current server id is injected by VeloraGameLink.

The resulting game is synchronized to Redis and published through the EventBus.

---

## Updating a Game

### State

```kotlin
VeloraGameLinkProvider.get().updateState(
    id = "bedwars-01",
    state = "STARTING"
)
```

### Players

```kotlin
VeloraGameLinkProvider.get().updatePlayers(
    id = "bedwars-01",
    players = 8
)
```

### Map

```kotlin
VeloraGameLinkProvider.get().updateMap(
    id = "bedwars-01",
    map = "Temple"
)
```

Clear the map with:

```kotlin
VeloraGameLinkProvider.get().updateMap(
    id = "bedwars-01",
    map = null
)
```

### Multiple values

```kotlin
VeloraGameLinkProvider.get().updateGame(
    id = "bedwars-01",
    state = "STARTING",
    players = 8,
    maxPlayers = 16,
    map = "Temple"
)
```

---

## Unregistering a Game

```kotlin
VeloraGameLinkProvider.get().unregisterGame(
    "bedwars-01"
)
```

The game is removed from local registration, Redis indexes and the network state.

---

## Looking Up Games

### By id

```kotlin
val game = VeloraGameLinkProvider.get().getGame(
    "bedwars-01"
)
```

### All network games

```kotlin
val games = VeloraGameLinkProvider.get().getGames()
```

### By type

```kotlin
val games = VeloraGameLinkProvider.get().getGames(
    "bedwars"
)
```

### Local games only

```kotlin
val localGames = VeloraGameLinkProvider.get().getLocalGames()
```

---

## Game Selection

### Best game

```kotlin
val game = VeloraGameLinkProvider.get().findBestGame(
    "bedwars"
)
```

Only joinable games are considered.

Selection uses:

1. `allowJoin`
2. available player slots
3. state `priority`
4. player count

### Available games

```kotlin
val games = VeloraGameLinkProvider.get().getAvailableGames(
    "bedwars"
)
```

---

## Join Intents

One Minecraft server may host multiple game instances.

Sending a player only to:

```text
games-01
```

is therefore not enough.

The destination server also needs to know whether the player wanted:

```text
bedwars-01
bedwars-02
bedwars-03
```

VeloraGameLink solves this using a temporary Redis join intent.

### Flow

```text
LOBBY
  |
  | createJoinIntent(playerUuid, gameId)
  v
Redis
  |
  | player -> gameId
  v
Proxy transfer
  |
  v
GAME server
  |
  | consumeJoinIntent(playerUuid)
  v
Game plugin adds player to the requested instance
```

### Create

```kotlin
VeloraGameLinkProvider.get().createJoinIntent(
    playerId = player.uniqueId,
    gameId = "bedwars-01"
)
```

### Read without removing

```kotlin
val gameId = VeloraGameLinkProvider.get().getJoinIntent(
    player.uniqueId
)
```

### Consume

Game plugins should normally consume the intent when a player arrives:

```kotlin
val gameId = VeloraGameLinkProvider.get().consumeJoinIntent(
    player.uniqueId
) ?: return
```

Then resolve the local game:

```kotlin
if (!VeloraGameLinkProvider.get().isRegistered(gameId)) {
    return
}
```

After `consumeJoinIntent`, the intent is removed.

### Other intent helpers

```kotlin
VeloraGameLinkProvider.get().hasJoinIntent(
    player.uniqueId
)

VeloraGameLinkProvider.get().hasValidJoinIntent(
    player.uniqueId
)

VeloraGameLinkProvider.get().getJoinIntentTtl(
    player.uniqueId
)

VeloraGameLinkProvider.get().removeJoinIntent(
    player.uniqueId
)
```

---

## Example Game Plugin Integration

A game plugin can register an instance during startup:

```kotlin
private lateinit var game: GameInstance

fun registerGame() {

    game = VeloraGameLinkProvider.get().registerGame(
        id = "bedwars-01",
        type = "bedwars",
        state = "WAITING",
        players = 0,
        maxPlayers = 16,
        map = "Lighthouse"
    )
}
```

Update it during the game lifecycle:

```kotlin
fun startCountdown() {

    VeloraGameLinkProvider.get().updateState(
        game.id,
        "STARTING"
    )
}
```

```kotlin
fun startGame() {

    VeloraGameLinkProvider.get().updateState(
        game.id,
        "RUNNING"
    )
}
```

```kotlin
fun updatePlayerCount(
    players: Int
) {

    VeloraGameLinkProvider.get().updatePlayers(
        game.id,
        players
    )
}
```

And remove it when the instance shuts down:

```kotlin
fun unregisterGame() {

    VeloraGameLinkProvider.get().unregisterGame(
        game.id
    )
}
```

---

## Player Arrival Example

A game plugin may handle an incoming player like this:

```kotlin
@EventHandler
fun onJoin(
    event: PlayerJoinEvent
) {

    val gameLink =
        VeloraGameLinkProvider.get()

    val gameId = gameLink.consumeJoinIntent(
        event.player.uniqueId
    ) ?: return

    if (!gameLink.isRegistered(
            gameId
        )
    ) {
        return
    }

    /*
     * Add the player to your own game instance here.
     */
}
```

VeloraGameLink intentionally does not know how BedWars, SkyWars or another game implementation stores and manages players.

That responsibility stays inside the game plugin.

---

## PlaceholderAPI

Namespace:

```text
gamelink
```

Examples:

```text
%gamelink_games%
%gamelink_games_bedwars%
%gamelink_available_bedwars%

%gamelink_best_bedwars%
%gamelink_best_players_bedwars%
%gamelink_best_max_players_bedwars%
%gamelink_best_state_bedwars%
%gamelink_best_map_bedwars%
%gamelink_best_server_bedwars%

%gamelink_join_intent%
%gamelink_has_join_intent%
%gamelink_join_intent_ttl%

%gamelink_intent_<playerUUID>%
```

The UUID-based intent placeholder is useful for integrations such as Skript.

Join-intent placeholders only read intents.

They do not consume them.

---

## Commands

General commands:

```text
/gamelink
/gamelink reload
/gamelink debug
/gamelink games
/gamelink menu
/gamelink menu <gameType>
/gamelink quickjoin <gameType>
```

Management commands:

```text
/gamelink manage register <id> <type> <state> <players> <maxPlayers> [map]
/gamelink manage unregister <id>
/gamelink manage state <id> <state>
/gamelink manage players <id> <players>
/gamelink manage map <id> <map>
/gamelink manage clearmap <id>
/gamelink manage info <id>
```

Game registration and mutation commands should normally be used on `GAME` servers.

---

## Permissions

Examples:

```text
gamelink.command
gamelink.command.reload
gamelink.command.debug
gamelink.command.games
gamelink.command.quickjoin

gamelink.command.menu
gamelink.command.menu.all
gamelink.command.menu.bedwars

gamelink.command.manage
gamelink.command.manage.register
gamelink.command.manage.unregister
gamelink.command.manage.state
gamelink.command.manage.players
gamelink.command.manage.map
gamelink.command.manage.info
```

Game-specific menu permissions are dynamic:

```text
gamelink.command.menu.<gameType>
```

---

## PlaceholderAPI Dependency

PlaceholderAPI is optional.

Example `paper-plugin.yml`:

```yaml
name: VeloraGameLink

main: online.veloraplugins.gamelink.paper.VeloraGameLinkPlugin

loader: online.veloraplugins.gamelink.paper.loader.VeloraGameLinkLoader

version: ${version}

api-version: "1.21"

folia-supported: true

dependencies:
  server:
    PlaceholderAPI:
      load: BEFORE
      required: false
```

---

## Architecture

```text
GameInstanceService
├─ Local mutable game instances

GameRedisService
├─ Redis persistence
├─ TTL
├─ indexes
└─ join intents

GameSelectorService
├─ allowJoin validation
├─ priority sorting
└─ best-game selection

GameSignService
├─ stable sign assignments
├─ showState handling
└─ sign rendering

GameJoinService
├─ final join validation
├─ join intent creation
└─ proxy transfer

GameLinkService
└─ Public VeloraGameLink API

PlaceholderRegistry
└─ PlaceholderAPI registrations
```

---

## Design Goals

VeloraGameLink is intentionally game-agnostic.

It does not contain BedWars, SkyWars or other game-specific logic.

External game plugins are responsible for:

- Defining their own game states
- Registering game instances
- Updating game data
- Handling players after a join intent is consumed
- Managing the actual gameplay lifecycle

VeloraGameLink is responsible for:

- Discovering games across the network
- Synchronizing their state
- Selecting joinable instances
- Displaying games in lobbies
- Routing players to the correct Minecraft server
- Passing the target game instance id to the destination server
