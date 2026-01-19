package dev.cobalt.library.command;

import dev.cobalt.library.event.EventBus;
import org.bukkit.plugin.Plugin;

public class CommandRegistry {
    public CommandRegistry(Plugin plugin, EventBus eventBus) {}
    public int getRegisteredCommandCount() { return 0; }
    public void unregisterAll() {}
}
