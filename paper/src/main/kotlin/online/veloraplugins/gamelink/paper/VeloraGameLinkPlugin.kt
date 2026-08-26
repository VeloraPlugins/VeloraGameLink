package online.veloraplugins.gamelink.paper

import online.velora.framework.eventbus.manager.EventBusManager
import online.velora.framework.language.MessagesConfig
import online.velora.framework.language.MessagesService
import online.velora.framework.redis.RedisManager
import online.velora.framework.redis.config.RedisConfig
import online.veloraplugins.engine.VeloraPlugin
import online.veloraplugins.engine.hooks.papi.PlaceholderAPIHook
import online.veloraplugins.gamelink.api.VeloraGameLinkProvider
import online.veloraplugins.gamelink.api.events.GameRegisteredEvent
import online.veloraplugins.gamelink.api.events.GameRemovedEvent
import online.veloraplugins.gamelink.api.events.GameUpdatedEvent
import online.veloraplugins.gamelink.api.game.ServerType
import online.veloraplugins.gamelink.paper.commands.GameLinkCommands
import online.veloraplugins.gamelink.paper.commands.GameManagementCommands
import online.veloraplugins.gamelink.paper.configurations.GameDisplaysConfig
import online.veloraplugins.gamelink.paper.configurations.GameLinkConfig
import online.veloraplugins.gamelink.paper.configurations.GameSignsConfig
import online.veloraplugins.gamelink.paper.configurations.GamesMenuConfig
import online.veloraplugins.gamelink.paper.eventbus.RedisEventTransport
import online.veloraplugins.gamelink.paper.listeners.GameSignListener
import online.veloraplugins.gamelink.paper.message.GameLinkMessage
import online.veloraplugins.gamelink.paper.placeholder.PlaceholderRegistry
import online.veloraplugins.gamelink.paper.services.GameInstanceService
import online.veloraplugins.gamelink.paper.services.GameJoinService
import online.veloraplugins.gamelink.paper.services.GameLinkService
import online.veloraplugins.gamelink.paper.services.GameRedisService
import online.veloraplugins.gamelink.paper.services.GameSelectorService
import online.veloraplugins.gamelink.paper.services.GameSignService
import online.veloraplugins.gamelink.paper.tasks.GameHeartbeatTask
import org.bukkit.Material

class VeloraGameLinkPlugin : VeloraPlugin() {

    companion object {

        private const val EVENT_BUS_CHANNEL =
            "velora:gamelink:eventbus"

        const val PROXY_CHANNEL =
            "BungeeCord"
    }

    /*
     * Configuration
     */

    lateinit var pluginConfig: GameLinkConfig
        private set

    lateinit var gameDisplaysConfig: GameDisplaysConfig
        private set

    lateinit var gameSignsConfig: GameSignsConfig
        private set

    lateinit var gamesMenuConfig: GamesMenuConfig
        private set

    private lateinit var redisConfig: RedisConfig

    /*
     * Messages
     */

    override lateinit var messages: MessagesService<GameLinkMessage>
        private set

    /*
     * Framework
     */

    lateinit var redisManager: RedisManager
        private set

    lateinit var eventBusManager: EventBusManager
        private set

    /*
     * Services
     */

    lateinit var gameInstanceService: GameInstanceService
        private set

    lateinit var gameRedisService: GameRedisService
        private set

    lateinit var gameSelectorService: GameSelectorService
        private set

    lateinit var gameSignService: GameSignService
        private set

    lateinit var gameJoinService: GameJoinService
        private set

    lateinit var gameLinkService: GameLinkService
        private set

    /*
     * Tasks
     */

    private var gameHeartbeatTask: GameHeartbeatTask? = null

    /*
     * Load
     */

    override suspend fun onPluginLoad() {

        loadConfigurations()
        validateConfiguration()

        loadMessages()

        loadRedis()
        loadEventBus()
    }

    /*
     * Enable
     */

    override suspend fun onPluginEnable() {

        createCommandManager(
            "gamelink"
        )

        loadPluginMessaging()
        loadServices()
        loadListeners()
        loadCommands()
        loadTasks()
        loadApi()

        if (pluginConfig.server.type == ServerType.LOBBY) {

            gameSelectorService.refresh()
            gameSignService.refreshAll()
        }

        registerPlaceholderAPI()
        loadHooks()

        info(
            "VeloraGameLink enabled as " +
                    "${pluginConfig.server.type} server " +
                    "'${pluginConfig.server.id}'."
        )
    }

    /*
     * Configuration
     */

    private fun loadConfigurations() {

        pluginConfig = loadConfig(
            "settings.yml",
            GameLinkConfig()
        )

        gameDisplaysConfig = loadConfig(
            "game-displays.yml",
            GameDisplaysConfig()
        )

        gameSignsConfig = loadConfig(
            "game-signs.yml",
            GameSignsConfig()
        )

        gamesMenuConfig = loadConfig(
            "games-menu.yml",
            GamesMenuConfig()
        )

        redisConfig = loadConfig(
            "redis.yml",
            RedisConfig()
        )
    }

    private fun validateConfiguration() {

        /*
         * Server
         */

        require(
            pluginConfig.server.id.isNotBlank()
        ) {
            "server.id cannot be blank."
        }

        /*
         * Synchronization
         */

        require(
            pluginConfig.synchronization.heartbeatInterval > 0L
        ) {
            "synchronization.heartbeatInterval must be greater than 0."
        }

        require(
            pluginConfig.synchronization.instanceTimeout > 0L
        ) {
            "synchronization.instanceTimeout must be greater than 0."
        }

        require(
            pluginConfig.synchronization.instanceTimeout >
                    pluginConfig.synchronization.heartbeatInterval
        ) {
            "synchronization.instanceTimeout must be greater than heartbeatInterval."
        }

        /*
         * Game displays
         */

        gameDisplaysConfig.games.forEach { (gameType, displayConfig) ->

            require(
                gameType.isNotBlank()
            ) {
                "Game display type cannot be blank."
            }

            require(
                displayConfig.states.isNotEmpty()
            ) {
                "Game '$gameType' must define at least one state."
            }

            /*
             * Searching display
             */

            validateSignDisplay(
                gameType = gameType,
                state = "searching-for-games",
                display = displayConfig.searchingForGames
            )

            /*
             * States
             */

            displayConfig.states.forEach { (state, signDisplay) ->

                require(
                    state.isNotBlank()
                ) {
                    "Game '$gameType' contains a blank state."
                }

                validateSignDisplay(
                    gameType = gameType,
                    state = state,
                    display = signDisplay
                )

                if (signDisplay.allowJoin && !signDisplay.showState) {

                    debug(
                        "CONFIG",
                        "Game '$gameType' state '$state' has " +
                                "allowJoin=true while showState=false. " +
                                "QuickJoin may still select this state, " +
                                "but it will not occupy a physical sign."
                    )
                }
            }
        }

        /*
         * Game signs
         */

        gameSignsConfig.signs.forEachIndexed { index, sign ->

            require(
                sign.gameType.isNotBlank()
            ) {
                "Game sign #$index has a blank game type."
            }

            require(
                findGameDisplayConfig(
                    sign.gameType
                ) != null
            ) {
                "Game sign #$index references unknown game type '${sign.gameType}'."
            }

            require(
                sign.location.world.isNotBlank()
            ) {
                "Game sign #$index has a blank world."
            }
        }
    }

    private fun validateSignDisplay(
        gameType: String,
        state: String,
        display: GameDisplaysConfig.SignDisplay
    ) {

        require(
            display.lines.size == 4
        ) {
            "Sign display '$gameType/$state' must contain exactly 4 lines."
        }

        require(
            display.material.isNotBlank()
        ) {
            "Sign display '$gameType/$state' has a blank material."
        }

        val material = Material.matchMaterial(
            display.material
        )

        require(
            material != null
        ) {
            "Sign display '$gameType/$state' contains invalid material '${display.material}'."
        }

        require(
            isSignMaterial(material)
        ) {
            "Material '${display.material}' for '$gameType/$state' is not a sign material."
        }
    }

    private suspend fun registerPlaceholderAPI() {

        val placeholderAPIHook = PlaceholderAPIHook(
            plugin = this,
            namespace = "gamelink"
        )

        PlaceholderRegistry.register(
            plugin = this,
            hook = placeholderAPIHook
        )

        registerHook(
            placeholderAPIHook
        )
    }

    /*
     * Messages
     */

    private fun loadMessages() {

        val messagesConfig = loadConfig(
            "messages.yml",
            MessagesConfig(
                "<gradient:#00B0FF:#2979FF><b>VeloraGameLink</b></gradient> <gray>»</gray> "
            )
        )

        messages = MessagesService(
            messagesConfig,
            GameLinkMessage.entries
        )
    }

    /*
     * Redis
     */

    private fun loadRedis() {

        redisManager = RedisManager(
            config = redisConfig,
            info = {
                debug(
                    "REDIS",
                    it
                )
            },
            warn = {
                warn(
                    "[Redis] $it"
                )
            },
            error = { message, throwable ->

                if (throwable != null) {

                    error(
                        "[Redis] $message",
                        throwable
                    )

                } else {

                    error(
                        "[Redis] $message"
                    )
                }
            }
        )

        redisManager.start()

        if (redisManager.isConnected()) {

            debug(
                "REDIS",
                "Redis connected."
            )

        } else {

            warn(
                "Redis is not connected."
            )
        }
    }

    /*
     * Event Bus
     */

    private fun loadEventBus() {

        eventBusManager = EventBusManager(
            transport = RedisEventTransport(
                redisManager = redisManager,
                channel = EVENT_BUS_CHANNEL
            ),
            logger = {
                debug(
                    "EVENTBUS",
                    it
                )
            },
            errorLogger = { message, throwable ->

                if (throwable != null) {

                    debug(
                        "EVENTBUS",
                        message,
                        throwable
                    )

                } else {

                    debug(
                        "EVENTBUS",
                        message
                    )
                }
            }
        )

        eventBusManager.initialize()

        debug(
            "EVENTBUS",
            "Event bus initialized."
        )
    }

    /*
     * Plugin Messaging
     */

    private fun loadPluginMessaging() {

        server.messenger.registerOutgoingPluginChannel(
            this,
            PROXY_CHANNEL
        )

        debug(
            "MESSAGING",
            "Registered outgoing proxy channel '$PROXY_CHANNEL'."
        )
    }

    private fun unloadPluginMessaging() {

        server.messenger.unregisterOutgoingPluginChannel(
            this,
            PROXY_CHANNEL
        )

        debug(
            "MESSAGING",
            "Unregistered outgoing proxy channel '$PROXY_CHANNEL'."
        )
    }

    /*
     * Services
     */

    private fun loadServices() {

        /*
         * Local game management
         */

        gameInstanceService = GameInstanceService(
            this
        )

        /*
         * Redis persistence
         */

        gameRedisService = GameRedisService(
            plugin = this,
            redisManager = redisManager,
            gameInstanceService = gameInstanceService
        )

        gameRedisService.start()

        /*
         * Network game selection
         */

        gameSelectorService = GameSelectorService(
            plugin = this,
            gameRedisService = gameRedisService
        )

        /*
         * Physical lobby signs
         */

        gameSignService = GameSignService(
            plugin = this,
            gameRedisService = gameRedisService
        )

        /*
         * Joining and QuickJoin
         */

        gameJoinService = GameJoinService(
            plugin = this,
            gameRedisService = gameRedisService,
            gameSelectorService = gameSelectorService
        )

        /*
         * Public API implementation
         */

        gameLinkService = GameLinkService(
            gameInstanceService = gameInstanceService,
            gameRedisService = gameRedisService,
            gameSelectorService = gameSelectorService,
            eventBusManager = eventBusManager
        )

        debug(
            "SERVICE",
            "Loaded GameLink services."
        )
    }

    /*
     * API
     */

    private fun loadApi() {

        VeloraGameLinkProvider.register(
            gameLinkService
        )

        debug(
            "API",
            "Registered VeloraGameLink API."
        )
    }

    /*
     * Listeners
     */

    private fun loadListeners() {

        loadBukkitListeners()
        loadEventBusListeners()

        debug(
            "LISTENER",
            "Registered listeners."
        )
    }

    private fun loadBukkitListeners() {

        if (pluginConfig.server.type != ServerType.LOBBY) {

            debug(
                "LISTENER",
                "Skipping lobby listeners on ${pluginConfig.server.type} server."
            )

            return
        }

        registerListeners(
            GameSignListener(
                this
            )
        )

        debug(
            "LISTENER",
            "Registered lobby Bukkit listeners."
        )
    }

    private fun loadEventBusListeners() {

        eventBusManager.subscribe(
            GameRegisteredEvent::class
        ) { event ->

            debug(
                "EVENTBUS",
                "Game '${event.game.id}' registered on '${event.game.serverId}'."
            )

            refreshLobbyDisplays()
        }

        eventBusManager.subscribe(
            GameUpdatedEvent::class
        ) { event ->

            debug(
                "EVENTBUS",
                "Game '${event.game.id}' updated " +
                        "to state '${event.game.state}'."
            )

            refreshLobbyDisplays()
        }

        eventBusManager.subscribe(
            GameRemovedEvent::class
        ) { event ->

            debug(
                "EVENTBUS",
                "Game '${event.gameId}' removed from '${event.serverId}'."
            )

            refreshLobbyDisplays()
        }

        debug(
            "EVENTBUS",
            "Registered GameLink event bus listeners."
        )
    }

    private fun refreshLobbyDisplays() {

        if (pluginConfig.server.type != ServerType.LOBBY) {
            return
        }

        if (
            !::gameSelectorService.isInitialized ||
            !::gameSignService.isInitialized
        ) {
            return
        }

        gameSelectorService.refresh()
        gameSignService.refreshAll()
    }

    /*
     * Commands
     */

    private fun loadCommands() {

        GameLinkCommands(
            this
        )

        GameManagementCommands(
            this
        )

        debug(
            "COMMAND",
            "Registered commands."
        )
    }

    /*
     * Tasks
     */

    private fun loadTasks() {

        startHeartbeatTask()

        debug(
            "TASK",
            "Started synchronization task."
        )
    }

    private fun startHeartbeatTask() {

        stopHeartbeatTask()

        gameHeartbeatTask = GameHeartbeatTask(
            this
        ).also {
            it.start()
        }
    }

    private fun stopHeartbeatTask() {

        gameHeartbeatTask?.stop()
        gameHeartbeatTask = null
    }

    /*
     * Reload
     */

    override suspend fun onPluginReload() {

        /*
         * VeloraPlugin already reloads registered configs
         * and messages before calling this method.
         */

        validateConfiguration()

        if (::gameSelectorService.isInitialized) {

            gameSelectorService.reload()
        }

        if (pluginConfig.server.type == ServerType.LOBBY) {

            if (::gameSignService.isInitialized) {

                /*
                 * Config locations or display definitions may
                 * have changed, so reset assignments first.
                 */

                gameSignService.clearAssignments()
            }

            refreshLobbyDisplays()
        }

        startHeartbeatTask()

        debug(
            "RELOAD",
            "Reload complete."
        )
    }

    /*
     * Shutdown
     */

    override suspend fun onPluginShutdown() {

        /*
         * Stop synchronization first so nothing can write
         * game information while shutting down.
         */

        stopHeartbeatTask()

        /*
         * Remove API exposure.
         */

        VeloraGameLinkProvider.unregister()

        /*
         * Clear lobby state.
         */

        if (::gameSignService.isInitialized) {

            gameSignService.clearAssignments()
        }

        /*
         * Remove local game instances from Redis before
         * shutting the Redis connection down.
         */

        if (::gameRedisService.isInitialized) {

            gameRedisService.shutdown()
        }

        if (::gameInstanceService.isInitialized) {

            gameInstanceService.unregisterAll()
        }

        /*
         * Event bus uses Redis transport, so shut it down
         * before Redis itself.
         */

        if (::eventBusManager.isInitialized) {

            eventBusManager.shutdown()
        }

        if (::redisManager.isInitialized) {

            redisManager.shutdown()
        }

        unloadPluginMessaging()

        cooldownHandler.clearAll()

        info(
            "VeloraGameLink disabled."
        )
    }

    /*
     * Helpers
     */

    private fun findGameDisplayConfig(
        gameType: String
    ): GameDisplaysConfig.GameTypeDisplay? {

        return gameDisplaysConfig
            .games
            .entries
            .firstOrNull {
                it.key.equals(
                    gameType,
                    ignoreCase = true
                )
            }
            ?.value
    }

    private fun isSignMaterial(
        material: Material
    ): Boolean {

        return material.name.endsWith(
            "_SIGN"
        ) || material.name.endsWith(
            "_WALL_SIGN"
        )
    }

    /*
     * Debug
     */

    override fun isDebugEnabled(): Boolean {

        return ::pluginConfig.isInitialized &&
                pluginConfig.debug
    }
}