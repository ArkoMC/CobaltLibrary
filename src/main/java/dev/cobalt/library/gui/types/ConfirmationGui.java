package dev.cobalt.library.gui.types;

import dev.cobalt.library.gui.GuiRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;

public class ConfirmationGui extends GuiRegistry.BaseGui {

    private final Runnable onConfirm;
    private final Runnable onCancel;

    public ConfirmationGui(Plugin plugin, Player player, String title, Runnable onConfirm, Runnable onCancel) {
        super(plugin, player, title);
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
        createInventory();
        setupButtons();
    }

    @Override
    protected void createInventory() {
        inventory = Bukkit.createInventory(null, 27, title);
    }

    private void setupButtons() {
        // Confirm button (green)
        ItemStack confirmItem = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta confirmMeta = confirmItem.getItemMeta();
        if (confirmMeta != null) {
            confirmMeta.setDisplayName("§a§lCONFIRM");
            confirmMeta.setLore(Arrays.asList("§7Click to confirm"));
            confirmItem.setItemMeta(confirmMeta);
        }

        // Fill left side with confirm
        for (int i = 10; i <= 16; i++) {
            if (i != 13) {
                setItem(i, confirmItem, p -> {
                    if (onConfirm != null) onConfirm.run();
                    close();
                });
            }
        }

        // Cancel button (red)
        ItemStack cancelItem = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta cancelMeta = cancelItem.getItemMeta();
        if (cancelMeta != null) {
            cancelMeta.setDisplayName("§c§lCANCEL");
            cancelMeta.setLore(Arrays.asList("§7Click to cancel"));
            cancelItem.setItemMeta(cancelMeta);
        }

        // Fill right side with cancel
        for (int i = 10; i <= 16; i++) {
            if (i != 13) {
                setItem(i + 9, cancelItem, p -> {
                    if (onCancel != null) onCancel.run();
                    close();
                });
            }
        }

        // Question mark in middle
        ItemStack questionItem = new ItemStack(Material.PAPER);
        ItemMeta questionMeta = questionItem.getItemMeta();
        if (questionMeta != null) {
            questionMeta.setDisplayName("§e§lAre you sure?");
            questionItem.setItemMeta(questionMeta);
        }
        setItem(13, questionItem);
    }

    @Override
    protected void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) return;

        GuiRegistry.ClickableItem item = items.get(slot);
        if (item != null) {
            item.click((Player) event.getWhoClicked());
        }
    }

    @Override
    protected void handleDrag(InventoryDragEvent event) {
        event.setCancelled(true);
    }
}
