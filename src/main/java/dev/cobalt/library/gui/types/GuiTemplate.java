package dev.cobalt.library.gui.types;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class GuiTemplate {
    public final String title;
    public final int rows;
    public final List<TemplateItem> templateItems = new ArrayList<>();

    public GuiTemplate(String title, int rows) {
        this.title = title;
        this.rows = rows;
    }

    public GuiTemplate addItem(int slot, ItemStack item, Consumer<Player> onClick) {
        templateItems.add(new TemplateItem(slot, item, onClick));
        return this;
    }

    public void apply(ChestGui gui) {
        for (TemplateItem item : templateItems) {
            gui.setItem(item.slot, item.item, item.onClick);
        }
    }

    private record TemplateItem(int slot, ItemStack item, Consumer<Player> onClick) {}
}
