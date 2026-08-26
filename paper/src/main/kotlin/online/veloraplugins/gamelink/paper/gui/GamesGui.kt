package online.veloraplugins.gamelink.paper.gui

import online.velora.framework.adventure.ComponentUtil
import online.veloraplugins.engine.extensions.parse
import online.veloraplugins.engine.libs.triumph.builder.item.PaperItemBuilder
import online.veloraplugins.engine.libs.triumph.guis.Gui
import online.veloraplugins.engine.libs.triumph.guis.PaginatedGui
import online.veloraplugins.engine.libs.xseries.XMaterial
import online.veloraplugins.gamelink.api.game.GameInstance
import online.veloraplugins.gamelink.paper.VeloraGameLinkPlugin
import online.veloraplugins.gamelink.paper.configurations.GameDisplaysConfig
import online.veloraplugins.gamelink.paper.configurations.GamesMenuConfig
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class GamesGui(
    private val plugin: VeloraGameLinkPlugin,
    private val gameType: String? = null
) {

    /*
     * Open
     */

    fun open(
        player: Player
    ) {

        val config = plugin.gamesMenuConfig

        val gui = Gui.paginated()
            .rows(
                config.menu.rows
            )
            .title(
                ComponentUtil.parse(
                    getTitle()
                )
            )
            .create()

        gui.disableAllInteractions()

        /*
         * Border
         */

        if (config.border.enabled) {

            gui.filler.fillBorder(
                PaperItemBuilder.from(
                    getMaterial(
                        config.border.material,
                        XMaterial.GRAY_STAINED_GLASS_PANE
                    )
                )
                    .name(
                        ComponentUtil.parse(
                            config.border.name
                        )
                    )
                    .asGuiItem()
            )
        }

        /*
         * Games
         */

        val games = getGames()

        games.forEach { game ->

            gui.addItem(
                createGameItem(
                    gui = gui,
                    player = player,
                    game = game
                )
            )
        }

        /*
         * Empty
         */

        if (games.isEmpty()) {

            val emptyConfig =
                config.emptyItem

            val lore = if (gameType == null) {

                emptyConfig.lore

            } else {

                emptyConfig.gameLore
                    .map {
                        replaceMenuPlaceholders(
                            text = it
                        )
                    }
            }

            gui.setItem(
                22,
                PaperItemBuilder.from(
                    getMaterial(
                        emptyConfig.material,
                        XMaterial.BARRIER
                    )
                )
                    .name(
                        ComponentUtil.parse(
                            replaceMenuPlaceholders(
                                emptyConfig.name
                            )
                        )
                    )
                    .lore(
                        lore.map {
                            ComponentUtil.parse(
                                replaceMenuPlaceholders(
                                    it
                                )
                            )
                        }
                    )
                    .asGuiItem()
            )
        }

        /*
         * Previous page
         */

        setNavigationItem(
            gui = gui,
            player = player,
            config = config.previous
        ) {
            gui.previous()
        }

        /*
         * Refresh
         */

        setNavigationItem(
            gui = gui,
            player = player,
            config = config.refresh
        ) {

            plugin.gameSelectorService.refresh()

            gui.close(
                player
            )

            open(
                player
            )
        }

        /*
         * Next page
         */

        setNavigationItem(
            gui = gui,
            player = player,
            config = config.next
        ) {
            gui.next()
        }

        /*
         * Close
         */

        setNavigationItem(
            gui = gui,
            player = player,
            config = config.close
        ) {

            gui.close(
                player
            )
        }

        /*
         * Open
         */

        gui.open(
            player
        )
    }

    /*
     * Games
     */

    private fun getGames(): List<GameInstance> {

        val games = if (gameType == null) {

            plugin
                .gameRedisService
                .getAll()

        } else {

            plugin
                .gameRedisService
                .getByType(
                    gameType
                )
        }

        return games
            .asSequence()
            .filter {
                plugin.gameSelectorService.isJoinable(
                    it
                )
            }
            .sortedWith(
                compareBy<GameInstance> {
                    it.type.lowercase()
                }.thenByDescending {
                    getPriority(
                        it
                    )
                }.thenByDescending {
                    it.players
                }.thenBy {
                    it.id.lowercase()
                }
            )
            .toList()
    }

    /*
     * Game item
     */

    private fun createGameItem(
        gui: PaginatedGui,
        player: Player,
        game: GameInstance
    ) = PaperItemBuilder.from(
        getGameMaterial(
            game
        )
    )
        .name(
            ComponentUtil.parse(
                replaceGamePlaceholders(
                    text = plugin.gamesMenuConfig.gameItem.name,
                    game = game
                )
            )
        )
        .lore(
            plugin
                .gamesMenuConfig
                .gameItem
                .lore
                .map {
                    ComponentUtil.parse(
                        replaceGamePlaceholders(
                            text = it,
                            game = game
                        )
                    )
                }
        )
        .asGuiItem {

            /*
             * Close using Triumph GUI.
             */

            gui.close(
                player
            )

            /*
             * GameJoinService re-validates the latest
             * game state from Redis before transfer.
             */

            plugin.gameJoinService.join(
                player = player,
                game = game
            )
        }

    /*
     * Navigation
     */

    private fun setNavigationItem(
        gui: PaginatedGui,
        player: Player,
        config: GamesMenuConfig.NavigationItem,
        action: () -> Unit
    ) {

        gui.setItem(
            config.slot,
            PaperItemBuilder.from(
                getMaterial(
                    config.material,
                    XMaterial.PAPER
                )
            )
                .name(
                    ComponentUtil.parse(
                        replaceMenuPlaceholders(
                            config.name
                        )
                    )
                )
                .lore(
                    config.lore.map {
                        ComponentUtil.parse(
                            replaceMenuPlaceholders(
                                it
                            )
                        )
                    }
                )
                .asGuiItem {
                    action()
                }
        )
    }

    /*
     * Game material
     */

    private fun getGameMaterial(
        game: GameInstance
    ): ItemStack {

        val config =
            plugin.gamesMenuConfig.gameItem

        /*
         * Use state material first.
         */

        if (config.useStateMaterial) {

            val stateMaterial = findStateDisplay(
                game
            )
                ?.material
                ?.let {
                    XMaterial.matchXMaterial(
                        it
                    ).orElse(
                        null
                    )
                }

            if (stateMaterial != null) {

                return ItemStack(
                    stateMaterial.parse
                )
            }
        }

        /*
         * Fallback material.
         */

        return getMaterial(
            config.material,
            XMaterial.PAPER
        )
    }

    /*
     * Priority
     */

    private fun getPriority(
        game: GameInstance
    ): Int {

        return findStateDisplay(
            game
        )
            ?.priority
            ?: Int.MIN_VALUE
    }

    /*
     * State display
     */

    private fun findStateDisplay(
        game: GameInstance
    ): GameDisplaysConfig.SignDisplay? {

        val gameConfig = plugin
            .gameDisplaysConfig
            .games
            .entries
            .firstOrNull {
                it.key.equals(
                    game.type,
                    ignoreCase = true
                )
            }
            ?.value
            ?: return null

        return gameConfig
            .states
            .entries
            .firstOrNull {
                it.key.equals(
                    game.state,
                    ignoreCase = true
                )
            }
            ?.value
    }

    /*
     * Title
     */

    private fun getTitle(): String {

        val config =
            plugin.gamesMenuConfig.menu

        return if (gameType == null) {

            config.title

        } else {

            config.gameTitle
                .replace(
                    "{game}",
                    formatGameType(
                        gameType
                    )
                )
        }
    }

    /*
     * Game placeholders
     */

    private fun replaceGamePlaceholders(
        text: String,
        game: GameInstance
    ): String {

        return text
            .replace(
                "{game}",
                formatGameType(
                    game.type
                )
            )
            .replace(
                "{game_id}",
                game.id
            )
            .replace(
                "{players}",
                game.players.toString()
            )
            .replace(
                "{max_players}",
                game.maxPlayers.toString()
            )
            .replace(
                "{state}",
                game.state
            )
            .replace(
                "{server}",
                game.serverId
            )
            .replace(
                "{map}",
                game.map ?: "Unknown"
            )
            .replace(
                "{priority}",
                getPriority(
                    game
                ).toString()
            )
    }

    /*
     * Menu placeholders
     */

    private fun replaceMenuPlaceholders(
        text: String
    ): String {

        return text
            .replace(
                "{game}",
                gameType?.let {
                    formatGameType(
                        it
                    )
                } ?: "Games"
            )
    }

    /*
     * Material
     */

    private fun getMaterial(
        material: String,
        fallback: XMaterial
    ): ItemStack {

        val resolved = XMaterial
            .matchXMaterial(
                material
            )
            .orElse(
                fallback
            )
            .parse

        return ItemStack(
            resolved
        )
    }

    /*
     * Format
     */

    private fun formatGameType(
        gameType: String
    ): String {

        return gameType
            .lowercase()
            .split(
                "_",
                "-"
            )
            .joinToString(
                " "
            ) {
                it.replaceFirstChar(
                    Char::titlecase
                )
            }
    }
}