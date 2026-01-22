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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class ScrollableGui extends GuiRegistry.BaseGui {

    private final int rows;
    private final List<GuiRegistry.ClickableItem> scrollItems = new ArrayList<>();
    private int scrollPosition = 0;
    private int scrollUpSlot = 1;
    private int scrollDownSlot = 7;

    public ScrollableGui(Plugin plugin, Player player, String title, int rows) {
        super(plugin, player, title);
        this.rows = Math.max(2, Math.min(6, rows));
        createInventory();
    }

    @Override
    protected void createInventory() {
        inventory = Bukkit.createInventory(null, rows * 9, title);
    }

    public ScrollableGui addScrollItem(ItemStack item, Consumer<Player> onClick) {
        scrollItems.add(new GuiRegistry.ClickableItem(item, onClick));
        return this;
    }

    public ScrollableGui setScrollUpSlot(int slot) {
        this.scrollUpSlot = slot;
        return this;
    }

    public ScrollableGui setScrollDownSlot(int slot) {
        this.scrollDownSlot = slot;
        return this;
    }

    public ScrollableGui scrollUp() {
        if (scrollPosition > 0) {
            scrollPosition--;
            render();
        }
        return this;
    }

    public ScrollableGui scrollDown() {
        int maxScroll = Math.max(0, scrollItems.size() - ((rows - 1) * 9));
        if (scrollPosition < maxScroll) {
            scrollPosition++;
            render();
        }
        return this;
    }

    private void render() {
        inventory.clear();

        // Render scroll items
        int contentRows = rows - 1;
        int contentSize = contentRows * 9;

        for (int i = 0; i < contentSize; i++) {
            int itemIndex = scrollPosition + i;
            if (itemIndex < scrollItems.size()) {
                GuiRegistry.ClickableItem item = scrollItems.get(itemIndex);
                items.put(i + 9, item);
                inventory.setItem(i + 9, item.getItem());
            }
        }

        // Scroll buttons
        ItemStack scrollUpItem = new ItemStack(Material.ARROW);
        ItemMeta upMeta = scrollUpItem.getItemMeta();
        if (upMeta != null) {
            upMeta.setDisplayName("§aRul op");
            scrollUpItem.setItemMeta(upMeta);
        }
        setItem(scrollUpSlot, scrollUpItem, p -> scrollUp());

        ItemStack scrollDownItem = new ItemStack(Material.ARROW);
        ItemMeta downMeta = scrollDownItem.getItemMeta();
        if (downMeta != null) {
            downMeta.setDisplayName("§aRul ned");
            scrollDownItem.setItemMeta(downMeta);
        }
        setItem(scrollDownSlot, scrollDownItem, p -> scrollDown());
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

    @Override
    public void open() {
        render();
        super.open();
    }
}
