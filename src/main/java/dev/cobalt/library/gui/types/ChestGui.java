package dev.cobalt.library.gui.types;

import dev.cobalt.library.gui.GuiRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.function.Consumer;

/**
 * Standard chest GUI with 1-6 rows
 */
public class ChestGui extends GuiRegistry.BaseGui {

    private final int rows;
    private Sound clickSound;
    private float clickVolume = 1.0f;
    private float clickPitch = 1.0f;

    public ChestGui(Plugin plugin, Player player, String title, int rows) {
        super(plugin, player, title);
        this.rows = Math.max(1, Math.min(6, rows)); // Clamp between 1-6
        createInventory();
    }

    @Override
    protected void createInventory() {
        inventory = Bukkit.createInventory(null, rows * 9, title);

        // Re-add all items
        items.forEach((slot, item) -> {
            if (slot < inventory.getSize()) {
                inventory.setItem(slot, item.getItem());
            }
        });
    }

    @Override
    protected void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        // Only handle clicks in the GUI (not player inventory)
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }

        // Play click sound
        if (clickSound != null) {
            player.playSound(player.getLocation(), clickSound, clickVolume, clickPitch);
        }

        // Global handlers
        for (Consumer<InventoryClickEvent> handler : globalClickHandlers) {
            handler.accept(event);
        }

        // Slot-specific handler
        GuiRegistry.ClickableItem item = items.get(slot);
        if (item != null) {
            item.click(player);
        }
    }

    @Override
    protected void handleDrag(InventoryDragEvent event) {
        // Cancel drags in GUI
        for (int slot : event.getRawSlots()) {
            if (slot < inventory.getSize()) {
                event.setCancelled(true);
                return;
            }
        }
    }

    /**
     * Set click sound effect
     */
    public ChestGui setClickSound(Sound sound, float volume, float pitch) {
        this.clickSound = sound;
        this.clickVolume = volume;
        this.clickPitch = pitch;
        return this;
    }

    /**
     * Fill border with item
     */
    public ChestGui fillBorder(ItemStack item) {
        int size = inventory.getSize();

        // Top and bottom rows
        for (int i = 0; i < 9; i++) {
            setItem(i, item);
            if (rows > 1) {
                setItem(size - 9 + i, item);
            }
        }

        // Left and right columns (excluding corners already filled)
        for (int row = 1; row < rows - 1; row++) {
            setItem(row * 9, item);
            setItem(row * 9 + 8, item);
        }

        return this;
    }

    /**
     * Fill empty slots with item
     */
    public ChestGui fillEmpty(ItemStack item) {
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                setItem(i, item);
            }
        }
        return this;
    }

    /**
     * Fill specific row with item
     */
    public ChestGui fillRow(int row, ItemStack item) {
        if (row < 0 || row >= rows) return this;

        int start = row * 9;
        for (int i = 0; i < 9; i++) {
            setItem(start + i, item);
        }
        return this;
    }

    /**
     * Fill specific column with item
     */
    public ChestGui fillColumn(int column, ItemStack item) {
        if (column < 0 || column >= 9) return this;

        for (int row = 0; row < rows; row++) {
            setItem(row * 9 + column, item);
        }
        return this;
    }

    /**
     * Fill rectangular area
     */
    public ChestGui fillRectangle(int startSlot, int endSlot, ItemStack item) {
        int startRow = startSlot / 9;
        int startCol = startSlot % 9;
        int endRow = endSlot / 9;
        int endCol = endSlot % 9;

        for (int row = startRow; row <= endRow; row++) {
            for (int col = startCol; col <= endCol; col++) {
                int slot = row * 9 + col;
                if (slot < inventory.getSize()) {
                    setItem(slot, item);
                }
            }
        }
        return this;
    }

    /**
     * Set multiple items in a pattern
     */
    public ChestGui setPattern(String[] pattern, ItemStack item) {
        if (pattern.length > rows) return this;

        for (int row = 0; row < pattern.length; row++) {
            String rowPattern = pattern[row];
            for (int col = 0; col < Math.min(9, rowPattern.length()); col++) {
                if (rowPattern.charAt(col) == 'X') {
                    setItem(row * 9 + col, item);
                }
            }
        }
        return this;
    }

    public int getRows() {
        return rows;
    }
}
