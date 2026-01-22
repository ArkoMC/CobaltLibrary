package dev.cobalt.library.gui;

import dev.cobalt.library.event.EventBus;
import dev.cobalt.library.gui.types.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class GuiRegistry implements Listener {

    private final Plugin plugin;
    private final EventBus eventBus;
    private final Map<UUID, BaseGui> activeGuis = new ConcurrentHashMap<>();
    private final Map<String, GuiTemplate> templates = new ConcurrentHashMap<>();

    public GuiRegistry(Plugin plugin, EventBus eventBus) {
        this.plugin = plugin;
        this.eventBus = eventBus;

        // Register listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // ==================== GUI CREATION ====================

    /**
     * Create a standard chest GUI
     */
    public ChestGui createChestGui(Player player, String title, int rows) {
        ChestGui gui = new ChestGui(plugin, player, title, rows);
        activeGuis.put(player.getUniqueId(), gui);
        return gui;
    }

    /**
     * Create a paginated GUI
     */
    public PaginatedGui createPaginatedGui(Player player, String title, int rows) {
        PaginatedGui gui = new PaginatedGui(plugin, player, title, rows);
        activeGuis.put(player.getUniqueId(), gui);
        return gui;
    }

    /**
     * Create a scrollable GUI
     */
    public ScrollableGui createScrollableGui(Player player, String title, int rows) {
        ScrollableGui gui = new ScrollableGui(plugin, player, title, rows);
        activeGuis.put(player.getUniqueId(), gui);
        return gui;
    }

    /**
     * Create a confirmation dialog
     */
    public ConfirmationGui createConfirmation(Player player, String title, Runnable onConfirm, Runnable onCancel) {
        ConfirmationGui gui = new ConfirmationGui(plugin, player, title, onConfirm, onCancel);
        activeGuis.put(player.getUniqueId(), gui);
        return gui;
    }

    /**
     * Create an anvil input GUI
     */
    public AnvilInputGui createAnvilInput(Player player, String defaultText, Consumer<String> onInput) {
        AnvilInputGui gui = new AnvilInputGui(plugin, player, defaultText, onInput);
        activeGuis.put(player.getUniqueId(), gui);
        return gui;
    }

    /**
     * Create GUI from template
     */
    public ChestGui createFromTemplate(Player player, String templateName) {
        GuiTemplate template = templates.get(templateName);
        if (template == null) {
            throw new IllegalArgumentException("Template not found: " + templateName);
        }

        ChestGui gui = createChestGui(player, template.title, template.rows);
        template.apply(gui);
        return gui;
    }

    /**
     * Register a GUI template
     */
    public void registerTemplate(String name, GuiTemplate template) {
        templates.put(name, template);
    }

    // ==================== GUI MANAGEMENT ====================

    /**
     * Get active GUI for player
     */
    public BaseGui getGui(Player player) {
        return activeGuis.get(player.getUniqueId());
    }

    /**
     * Check if player has GUI open
     */
    public boolean hasGui(Player player) {
        return activeGuis.containsKey(player.getUniqueId());
    }

    /**
     * Close GUI for player
     */
    public void close(Player player) {
        BaseGui gui = activeGuis.remove(player.getUniqueId());
        if (gui != null) {
            gui.close();
        }
    }

    /**
     * Close all active GUIs
     */
    public void closeAll() {
        activeGuis.values().forEach(BaseGui::close);
        activeGuis.clear();
    }

    /**
     * Get active GUI count
     */
    public int getActiveGuiCount() {
        return activeGuis.size();
    }

    /**
     * Update all GUIs (useful for global updates)
     */
    public void updateAll() {
        activeGuis.values().forEach(BaseGui::update);
    }

    // ==================== EVENT HANDLERS ====================

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        BaseGui gui = activeGuis.get(player.getUniqueId());
        if (gui != null && gui.isInventory(event.getInventory())) {
            gui.handleClick(event);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        BaseGui gui = activeGuis.get(player.getUniqueId());
        if (gui != null && gui.isInventory(event.getInventory())) {
            gui.handleClose(event);

            // Remove from active GUIs if not prevented
            if (!gui.preventClose) {
                activeGuis.remove(player.getUniqueId());
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        BaseGui gui = activeGuis.get(player.getUniqueId());
        if (gui != null && gui.isInventory(event.getInventory())) {
            gui.handleDrag(event);
        }
    }

    // ==================== BASE GUI CLASS ====================

    /**
     * Base class for all GUI types
     */
    public static abstract class BaseGui {
        protected final Plugin plugin;
        protected final Player player;
        protected Inventory inventory;
        protected final Map<Integer, ClickableItem> items = new ConcurrentHashMap<>();
        protected final List<Consumer<InventoryClickEvent>> globalClickHandlers = new ArrayList<>();
        protected Consumer<InventoryCloseEvent> closeHandler;
        protected boolean preventClose = false;
        protected BukkitTask updateTask;
        protected String title;

        public BaseGui(Plugin plugin, Player player, String title) {
            this.plugin = plugin;
            this.player = player;
            this.title = title;
        }

        /**
         * Set item with click handler
         */
        public BaseGui setItem(int slot, ItemStack item, Consumer<Player> onClick) {
            items.put(slot, new ClickableItem(item, onClick));
            if (inventory != null) {
                inventory.setItem(slot, item);
            }
            return this;
        }

        /**
         * Set item with dynamic supplier
         */
        public BaseGui setDynamicItem(int slot, Supplier<ItemStack> itemSupplier, Consumer<Player> onClick) {
            items.put(slot, new ClickableItem(itemSupplier, onClick));
            if (inventory != null) {
                inventory.setItem(slot, itemSupplier.get());
            }
            return this;
        }

        /**
         * Set item without click handler
         */
        public BaseGui setItem(int slot, ItemStack item) {
            return setItem(slot, item, null);
        }

        /**
         * Add global click handler (runs on any click)
         */
        public BaseGui onGlobalClick(Consumer<InventoryClickEvent> handler) {
            globalClickHandlers.add(handler);
            return this;
        }

        /**
         * Set close handler
         */
        public BaseGui onClose(Consumer<InventoryCloseEvent> handler) {
            this.closeHandler = handler;
            return this;
        }

        /**
         * Prevent closing (reopens GUI)
         */
        public BaseGui preventClose(boolean prevent) {
            this.preventClose = prevent;
            return this;
        }

        /**
         * Start auto-updating all items
         */
        public BaseGui startAutoUpdate(long intervalTicks) {
            stopAutoUpdate();
            updateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::update, 0L, intervalTicks);
            return this;
        }

        /**
         * Start auto-updating specific slot
         */
        public BaseGui startSlotUpdate(int slot, long intervalTicks) {
            Bukkit.getScheduler().runTaskTimer(plugin, () -> updateSlot(slot), 0L, intervalTicks);
            return this;
        }

        /**
         * Stop auto-update
         */
        public BaseGui stopAutoUpdate() {
            if (updateTask != null) {
                updateTask.cancel();
                updateTask = null;
            }
            return this;
        }

        /**
         * Update all dynamic items
         */
        public void update() {
            items.forEach((slot, item) -> {
                if (item.isDynamic() && inventory != null) {
                    inventory.setItem(slot, item.getItem());
                }
            });
        }

        /**
         * Update specific slot
         */
        public void updateSlot(int slot) {
            ClickableItem item = items.get(slot);
            if (item != null && item.isDynamic() && inventory != null) {
                inventory.setItem(slot, item.getItem());
            }
        }

        /**
         * Clear all items
         */
        public BaseGui clear() {
            items.clear();
            if (inventory != null) {
                inventory.clear();
            }
            return this;
        }

        /**
         * Open the GUI
         */
        public void open() {
            if (inventory == null) {
                createInventory();
            }
            player.openInventory(inventory);
        }

        /**
         * Close the GUI
         */
        public void close() {
            stopAutoUpdate();
            player.closeInventory();
        }

        /**
         * Update title (creates new inventory)
         */
        public void setTitle(String newTitle) {
            this.title = newTitle;
            boolean wasOpen = player.getOpenInventory().getTopInventory().equals(inventory);
            createInventory();
            if (wasOpen) {
                open();
            }
        }

        // Abstract methods
        protected abstract void createInventory();
        protected abstract void handleClick(InventoryClickEvent event);
        protected abstract void handleDrag(InventoryDragEvent event);

        protected void handleClose(InventoryCloseEvent event) {
            if (closeHandler != null) {
                closeHandler.accept(event);
            }

            if (preventClose) {
                Bukkit.getScheduler().runTask(plugin, this::open);
            }
        }

        protected boolean isInventory(Inventory inv) {
            return inventory != null && inventory.equals(inv);
        }

        public Player getPlayer() {
            return player;
        }

        public Inventory getInventory() {
            return inventory;
        }
    }

    // ==================== CLICKABLE ITEM ====================

    /**
     * Item with click handler and optional dynamic supplier
     */
    public static class ClickableItem {
        private final ItemStack staticItem;
        private final Supplier<ItemStack> dynamicSupplier;
        private final Consumer<Player> onClick;
        private long lastClick = 0;
        private long cooldown = 0;

        public ClickableItem(ItemStack item, Consumer<Player> onClick) {
            this.staticItem = item;
            this.dynamicSupplier = null;
            this.onClick = onClick;
        }

        public ClickableItem(Supplier<ItemStack> supplier, Consumer<Player> onClick) {
            this.staticItem = null;
            this.dynamicSupplier = supplier;
            this.onClick = onClick;
        }

        public ItemStack getItem() {
            return dynamicSupplier != null ? dynamicSupplier.get() : staticItem;
        }

        public boolean isDynamic() {
            return dynamicSupplier != null;
        }

        public void click(Player player) {
            if (onClick == null) return;

            long now = System.currentTimeMillis();
            if (cooldown > 0 && (now - lastClick) < cooldown) {
                return; // Still on cooldown
            }

            onClick.accept(player);
            lastClick = now;
        }

        public ClickableItem setCooldown(long cooldownMs) {
            this.cooldown = cooldownMs;
            return this;
        }
    }
}
