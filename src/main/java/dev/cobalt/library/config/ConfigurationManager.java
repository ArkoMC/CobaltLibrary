package dev.cobalt.library.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages configuration files for plugins and modules
 */
public class ConfigurationManager {

    private final Plugin plugin;
    private final Map<String, FileConfiguration> configs = new ConcurrentHashMap<>();
    private FileConfiguration mainConfig;

    public ConfigurationManager(Plugin plugin) {
        this.plugin = plugin;
        this.mainConfig = plugin.getConfig();
    }

    /**
     * Get the main plugin configuration
     */
    public FileConfiguration getConfig() {
        return mainConfig;
    }

    /**
     * Load or get a custom configuration file
     */
    public FileConfiguration getConfig(String name) {
        if (configs.containsKey(name)) {
            return configs.get(name);
        }

        File file = new File(plugin.getDataFolder(), name + ".yml");
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create config file: " + name);
            }
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        configs.put(name, config);
        return config;
    }

    /**
     * Save a configuration file
     */
    public void saveConfig(String name) {
        FileConfiguration config = configs.get(name);
        if (config != null) {
            try {
                File file = new File(plugin.getDataFolder(), name + ".yml");
                config.save(file);
            } catch (IOException e) {
                plugin.getLogger().severe("Could not save config file: " + name);
            }
        }
    }

    /**
     * Reload a configuration file
     */
    public void reloadConfig(String name) {
        File file = new File(plugin.getDataFolder(), name + ".yml");
        if (file.exists()) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            configs.put(name, config);
        }
    }

    /**
     * Get a string from main config with default
     */
    public String getString(String path, String defaultValue) {
        return mainConfig.getString(path, defaultValue);
    }

    /**
     * Get an int from main config with default
     */
    public int getInt(String path, int defaultValue) {
        return mainConfig.getInt(path, defaultValue);
    }

    /**
     * Get a boolean from main config with default
     */
    public boolean getBoolean(String path, boolean defaultValue) {
        return mainConfig.getBoolean(path, defaultValue);
    }

    /**
     * Get a double from main config with default
     */
    public double getDouble(String path, double defaultValue) {
        return mainConfig.getDouble(path, defaultValue);
    }
}
