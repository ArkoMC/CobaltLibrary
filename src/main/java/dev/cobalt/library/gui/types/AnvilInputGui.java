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

import java.util.function.Consumer;

public class AnvilInputGui extends GuiRegistry.BaseGui {

    private final String defaultText;
    private final Consumer<String> onInput;

    public AnvilInputGui(Plugin plugin, Player player, String defaultText, Consumer<String> onInput) {
        super(plugin, player, "Skriv her!");
        this.defaultText = defaultText;
        this.onInput = onInput;
        createInventory();
        setupInput();
    }

    @Override
    protected void createInventory() {
        // Create anvil inventory
        inventory = Bukkit.createInventory(null, org.bukkit.event.inventory.InventoryType.ANVIL, title);
    }

    private void setupInput() {
        // Set default item with text
        ItemStack inputItem = new ItemStack(Material.PAPER);
        ItemMeta meta = inputItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(defaultText);
            inputItem.setItemMeta(meta);
        }
        inventory.setItem(0, inputItem);
    }

    @Override
    protected void handleClick(InventoryClickEvent event) {
        if (event.getRawSlot() == 2) { // Result slot
            event.setCancelled(true);

            ItemStack result = event.getCurrentItem();
            if (result != null && result.hasItemMeta() && result.getItemMeta().hasDisplayName()) {
                String input = result.getItemMeta().getDisplayName();

                if (onInput != null) {
                    onInput.accept(input);
                }

                close();
            }
        }
    }

    @Override
    protected void handleDrag(InventoryDragEvent event) {
        // Allow dragging in anvil
    }
}
