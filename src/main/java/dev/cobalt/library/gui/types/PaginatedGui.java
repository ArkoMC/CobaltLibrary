package dev.cobalt.library.gui.types;

import dev.cobalt.library.gui.GuiRegistry;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Paginated GUI for displaying large lists of items
 */
public class PaginatedGui extends ChestGui {

    private final List<GuiRegistry.ClickableItem> pageItems = new ArrayList<>();
    private int currentPage = 0;
    private int[] contentSlots;

    // Navigation items
    private int previousButtonSlot = -1;
    private int nextButtonSlot = -1;
    private int pageInfoSlot = -1;

    private ItemStack previousButton;
    private ItemStack nextButton;
    private ItemStack pageInfoItem;

    public PaginatedGui(Plugin plugin, Player player, String title, int rows) {
        super(plugin, player, title, rows);

        // Default content area (exclude last row for navigation)
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < (rows - 1) * 9; i++) {
            slots.add(i);
        }
        this.contentSlots = slots.stream().mapToInt(Integer::intValue).toArray();

        // Default navigation buttons (last row)
        int lastRow = (rows - 1) * 9;
        setPreviousButtonSlot(lastRow + 3);
        setNextButtonSlot(lastRow + 5);
        setPageInfoSlot(lastRow + 4);
    }

    /**
     * Set which slots can contain page items
     */
    public PaginatedGui setContentSlots(int... slots) {
        this.contentSlots = slots;
        return this;
    }

    /**
     * Add item to pages
     */
    public PaginatedGui addPageItem(ItemStack item, Consumer<Player> onClick) {
        pageItems.add(new GuiRegistry.ClickableItem(item, onClick));
        return this;
    }

    /**
     * Add multiple items to pages
     */
    public PaginatedGui addPageItems(List<ItemStack> items, Consumer<Player> onClick) {
        for (ItemStack item : items) {
            pageItems.add(new GuiRegistry.ClickableItem(item, onClick));
        }
        return this;
    }

    /**
     * Clear all page items
     */
    public PaginatedGui clearPageItems() {
        pageItems.clear();
        return this;
    }

    /**
     * Set previous button slot and item
     */
    public PaginatedGui setPreviousButtonSlot(int slot) {
        this.previousButtonSlot = slot;
        if (previousButton == null) {
            previousButton = createDefaultPreviousButton();
        }
        return this;
    }

    /**
     * Set next button slot and item
     */
    public PaginatedGui setNextButtonSlot(int slot) {
        this.nextButtonSlot = slot;
        if (nextButton == null) {
            nextButton = createDefaultNextButton();
        }
        return this;
    }

    /**
     * Set page info slot
     */
    public PaginatedGui setPageInfoSlot(int slot) {
        this.pageInfoSlot = slot;
        return this;
    }

    /**
     * Customize previous button
     */
    public PaginatedGui setPreviousButton(ItemStack item) {
        this.previousButton = item;
        return this;
    }

    /**
     * Customize next button
     */
    public PaginatedGui setNextButton(ItemStack item) {
        this.nextButton = item;
        return this;
    }

    /**
     * Go to specific page
     */
    public PaginatedGui goToPage(int page) {
        int maxPage = getMaxPage();
        this.currentPage = Math.max(0, Math.min(page, maxPage));
        renderPage();
        return this;
    }

    /**
     * Go to next page
     */
    public PaginatedGui nextPage() {
        if (currentPage < getMaxPage()) {
            currentPage++;
            renderPage();
        }
        return this;
    }

    /**
     * Go to previous page
     */
    public PaginatedGui previousPage() {
        if (currentPage > 0) {
            currentPage--;
            renderPage();
        }
        return this;
    }

    /**
     * Get current page number
     */
    public int getCurrentPage() {
        return currentPage;
    }

    /**
     * Get max page number
     */
    public int getMaxPage() {
        return Math.max(0, (int) Math.ceil((double) pageItems.size() / contentSlots.length) - 1);
    }

    /**
     * Get total pages
     */
    public int getTotalPages() {
        return getMaxPage() + 1;
    }

    /**
     * Render current page
     */
    private void renderPage() {
        // Clear content area
        for (int slot : contentSlots) {
            items.remove(slot);
            if (inventory != null) {
                inventory.setItem(slot, null);
            }
        }

        // Calculate page items range
        int startIndex = currentPage * contentSlots.length;
        int endIndex = Math.min(startIndex + contentSlots.length, pageItems.size());

        // Add page items
        for (int i = startIndex; i < endIndex; i++) {
            int slotIndex = i - startIndex;
            if (slotIndex < contentSlots.length) {
                int slot = contentSlots[slotIndex];
                GuiRegistry.ClickableItem item = pageItems.get(i);
                items.put(slot, item);
                if (inventory != null) {
                    inventory.setItem(slot, item.getItem());
                }
            }
        }

        // Update navigation buttons
        updateNavigationButtons();
    }

    /**
     * Update navigation buttons
     */
    private void updateNavigationButtons() {
        // Previous button
        if (previousButtonSlot >= 0) {
            if (currentPage > 0) {
                setItem(previousButtonSlot, previousButton, p -> previousPage());
            } else {
                setItem(previousButtonSlot, createDisabledButton("Sidste Side"));
            }
        }

        // Next button
        if (nextButtonSlot >= 0) {
            if (currentPage < getMaxPage()) {
                setItem(nextButtonSlot, nextButton, p -> nextPage());
            } else {
                setItem(nextButtonSlot, createDisabledButton("Næste Side"));
            }
        }

        // Page info
        if (pageInfoSlot >= 0) {
            ItemStack pageInfo = new ItemStack(Material.PAPER);
            ItemMeta meta = pageInfo.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§eSide " + (currentPage + 1) + " / " + getTotalPages());
                meta.setLore(Arrays.asList(
                        "§7Totale Items: §f" + pageItems.size(),
                        "§7Items på denne side: §f" + Math.min(contentSlots.length, pageItems.size() - (currentPage * contentSlots.length))
                ));
                pageInfo.setItemMeta(meta);
            }
            setItem(pageInfoSlot, pageInfo);
        }
    }

    @Override
    public void open() {
        renderPage();
        super.open();
    }

    @Override
    public void update() {
        renderPage();
        super.update();
    }

    private ItemStack createDefaultPreviousButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§aPrevious Page");
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createDefaultNextButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§aNext Page");
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createDisabledButton(String name) {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§c" + name);
            item.setItemMeta(meta);
        }
        return item;
    }
}
