package dev.cobalt.library.gui;

import dev.cobalt.library.event.EventBus;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * GUI Registry for managing inventory-based GUIs
 */
public class GuiRegistry implements Listener {

    private final Plugin plugin;
    private final EventBus eventBus;
    private final Map<UUID, Gui> activeGuis = new ConcurrentHashMap<>();

    public GuiRegistry(Plugin plugin, EventBus eventBus) {
        this.plugin = plugin;
        this.eventBus = eventBus;

        // Register listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Create and open a GUI for a player
     */
    public Gui create(Player player, String title, int rows) {
        Gui gui = new Gui(player, title, rows);
        activeGuis.put(player.getUniqueId(), gui);
        return gui;
    }

    /**
     * Get active GUI for player
     */
    public Gui getGui(Player player) {
        return activeGuis.get(player.getUniqueId());
    }

    /**
     * Close GUI for player
     */
    public void close(Player player) {
        Gui gui = activeGuis.remove(player.getUniqueId());
        if (gui != null) {
            player.closeInventory();
        }
    }

    /**
     * Close all active GUIs
     */
    public void closeAll() {
        activeGuis.values().forEach(gui -> {
            gui.player.closeInventory();
        });
        activeGuis.clear();
    }

    /**
     * Get active GUI count
     */
    public int getActiveGuiCount() {
        return activeGuis.size();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        Gui gui = activeGuis.get(player.getUniqueId());

        if (gui != null && event.getInventory().equals(gui.inventory)) {
            event.setCancelled(true);

            int slot = event.getRawSlot();
            if (slot >= 0 && slot < gui.inventory.getSize()) {
                Consumer<InventoryClickEvent> handler = gui.clickHandlers.get(slot);
                if (handler != null) {
                    handler.accept(event);
                }

                // Publish event
                eventBus.publish("gui.click", new GuiClickEvent(gui, player, slot));
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;

        Player player = (Player) event.getPlayer();
        Gui gui = activeGuis.get(player.getUniqueId());

        if (gui != null && event.getInventory().equals(gui.inventory)) {
            Consumer<InventoryCloseEvent> handler = gui.closeHandler;
            if (handler != null) {
                handler.accept(event);
            }

            activeGuis.remove(player.getUniqueId());

            // Publish event
            eventBus.publish("gui.close", new GuiCloseEvent(gui, player));
        }
    }

    /**
     * GUI wrapper class
     */
    public static class Gui {
        private final Player player;
        private final Inventory inventory;
        private final Map<Integer, Consumer<InventoryClickEvent>> clickHandlers = new ConcurrentHashMap<>();
        private Consumer<InventoryCloseEvent> closeHandler;

        public Gui(Player player, String title, int rows) {
            this.player = player;
            this.inventory = Bukkit.createInventory(null, rows * 9, title);
        }

        /**
         * Set item at slot
         */
        public Gui setItem(int slot, ItemStack item) {
            inventory.setItem(slot, item);
            return this;
        }

        /**
         * Set item with click handler
         */
        public Gui setItem(int slot, ItemStack item, Consumer<InventoryClickEvent> onClick) {
            inventory.setItem(slot, item);
            clickHandlers.put(slot, onClick);
            return this;
        }

        /**
         * Set click handler for slot
         */
        public Gui onClick(int slot, Consumer<InventoryClickEvent> handler) {
            clickHandlers.put(slot, handler);
            return this;
        }

        /**
         * Set close handler
         */
        public Gui onClose(Consumer<InventoryCloseEvent> handler) {
            this.closeHandler = handler;
            return this;
        }

        /**
         * Fill empty slots with item
         */
        public Gui fill(ItemStack item) {
            for (int i = 0; i < inventory.getSize(); i++) {
                if (inventory.getItem(i) == null) {
                    inventory.setItem(i, item);
                }
            }
            return this;
        }

        /**
         * Clear all items
         */
        public Gui clear() {
            inventory.clear();
            clickHandlers.clear();
            return this;
        }

        /**
         * Open GUI for player
         */
        public void open() {
            player.openInventory(inventory);
        }

        /**
         * Get the inventory
         */
        public Inventory getInventory() {
            return inventory;
        }

        /**
         * Get the player
         */
        public Player getPlayer() {
            return player;
        }
    }

    /**
     * GUI click event
     */
    public static class GuiClickEvent {
        private final Gui gui;
        private final Player player;
        private final int slot;

        public GuiClickEvent(Gui gui, Player player, int slot) {
            this.gui = gui;
            this.player = player;
            this.slot = slot;
        }

        public Gui getGui() { return gui; }
        public Player getPlayer() { return player; }
        public int getSlot() { return slot; }
    }

    /**
     * GUI close event
     */
    public static class GuiCloseEvent {
        private final Gui gui;
        private final Player player;

        public GuiCloseEvent(Gui gui, Player player) {
            this.gui = gui;
            this.player = player;
        }

        public Gui getGui() { return gui; }
        public Player getPlayer() { return player; }
    }
}
