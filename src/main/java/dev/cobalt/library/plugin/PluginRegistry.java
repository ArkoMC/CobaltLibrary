package dev.cobalt.library.plugin;

import dev.cobalt.library.event.EventBus;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for tracking plugins using CobaltAPI
 */
public class PluginRegistry {

    private final Plugin plugin;
    private final EventBus eventBus;
    private final Map<String, RegisteredPlugin> registeredPlugins = new ConcurrentHashMap<>();

    public PluginRegistry(Plugin plugin, EventBus eventBus) {
        this.plugin = plugin;
        this.eventBus = eventBus;
    }

    /**
     * Register a plugin with CobaltAPI
     */
    public void register(Plugin plugin, String version) {
        RegisteredPlugin registered = new RegisteredPlugin(plugin, version, System.currentTimeMillis());
        registeredPlugins.put(plugin.getName(), registered);

        this.plugin.getLogger().info("Plugin registered: " + plugin.getName() + " v" + version);
        eventBus.publish("plugin.registered", registered);
    }

    /**
     * Unregister a plugin
     */
    public void unregister(String pluginName) {
        RegisteredPlugin removed = registeredPlugins.remove(pluginName);
        if (removed != null) {
            plugin.getLogger().info("Plugin unregistered: " + pluginName);
            eventBus.publish("plugin.unregistered", removed);
        }
    }

    /**
     * Check if plugin is registered
     */
    public boolean isRegistered(String pluginName) {
        return registeredPlugins.containsKey(pluginName);
    }

    /**
     * Get registered plugin info
     */
    public RegisteredPlugin getPlugin(String pluginName) {
        return registeredPlugins.get(pluginName);
    }

    /**
     * Get all registered plugin names
     */
    public Set<String> getRegisteredPluginNames() {
        return registeredPlugins.keySet();
    }

    /**
     * Get registered plugin count
     */
    public int getRegisteredPluginCount() {
        return registeredPlugins.size();
    }

    /**
     * Registered plugin data
     */
    public static class RegisteredPlugin {
        private final Plugin plugin;
        private final String version;
        private final long registeredAt;

        public RegisteredPlugin(Plugin plugin, String version, long registeredAt) {
            this.plugin = plugin;
            this.version = version;
            this.registeredAt = registeredAt;
        }

        public Plugin getPlugin() { return plugin; }
        public String getName() { return plugin.getName(); }
        public String getVersion() { return version; }
        public long getRegisteredAt() { return registeredAt; }
    }
}
