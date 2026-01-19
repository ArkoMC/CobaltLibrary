package dev.cobalt.library.i18n;

import dev.cobalt.library.config.ConfigurationManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Localization/Internationalization (i18n) Manager
 * Supports multiple languages with placeholders
 */
public class LocalizationManager {

    private final Plugin plugin;
    private final ConfigurationManager config;
    private final Map<String, Map<String, String>> messages = new ConcurrentHashMap<>();
    private String defaultLocale;

    public LocalizationManager(Plugin plugin, ConfigurationManager config) {
        this.plugin = plugin;
        this.config = config;
        this.defaultLocale = config.getString("locale.default", "en_US");

        loadLocales();
    }

    /**
     * Load all locale files from languages/ folder
     */
    private void loadLocales() {
        File langFolder = new File(plugin.getDataFolder(), "languages");
        if (!langFolder.exists()) {
            langFolder.mkdirs();

            // Create default locale file
            createDefaultLocale();
        }

        File[] files = langFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                String locale = file.getName().replace(".yml", "");
                loadLocale(locale);
            }
        }

        plugin.getLogger().info("Loaded " + messages.size() + " locale(s)");
    }

    /**
     * Load a specific locale file
     */
    public void loadLocale(String locale) {
        File file = new File(plugin.getDataFolder(), "languages/" + locale + ".yml");
        if (!file.exists()) {
            plugin.getLogger().warning("Locale file not found: " + locale);
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        Map<String, String> localeMessages = new HashMap<>();

        for (String key : config.getKeys(true)) {
            if (config.isString(key)) {
                localeMessages.put(key, config.getString(key));
            }
        }

        messages.put(locale, localeMessages);
        plugin.getLogger().info("Loaded locale: " + locale + " (" + localeMessages.size() + " messages)");
    }

    /**
     * Create default locale file (en_US)
     */
    private void createDefaultLocale() {
        File file = new File(plugin.getDataFolder(), "languages/en_US.yml");
        try {
            file.createNewFile();
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);

            // Add default messages
            config.set("welcome", "Welcome to the server, {player}!");
            config.set("goodbye", "Goodbye, {player}!");
            config.set("no_permission", "You don't have permission to do that.");
            config.set("unknown_command", "Unknown command.");

            config.save(file);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to create default locale file: " + e.getMessage());
        }
    }

    /**
     * Get message in default locale
     */
    public String getMessage(String key) {
        return getMessage(key, defaultLocale);
    }

    /**
     * Get message in specific locale
     */
    public String getMessage(String key, String locale) {
        Map<String, String> localeMessages = messages.get(locale);

        if (localeMessages != null && localeMessages.containsKey(key)) {
            return localeMessages.get(key);
        }

        // Fallback to default locale
        if (!locale.equals(defaultLocale)) {
            localeMessages = messages.get(defaultLocale);
            if (localeMessages != null && localeMessages.containsKey(key)) {
                return localeMessages.get(key);
            }
        }

        // Fallback to key itself
        return key;
    }

    /**
     * Get message with placeholders
     */
    public String getMessage(String key, Map<String, String> placeholders) {
        return getMessage(key, defaultLocale, placeholders);
    }

    /**
     * Get message with placeholders in specific locale
     */
    public String getMessage(String key, String locale, Map<String, String> placeholders) {
        String message = getMessage(key, locale);

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }

        return message;
    }

    /**
     * Get message for player (uses player's locale if available)
     */
    public String getMessageForPlayer(Player player, String key) {
        String locale = getPlayerLocale(player);
        return getMessage(key, locale);
    }

    /**
     * Get message for player with placeholders
     */
    public String getMessageForPlayer(Player player, String key, Map<String, String> placeholders) {
        String locale = getPlayerLocale(player);
        return getMessage(key, locale, placeholders);
    }

    /**
     * Get player's locale (from client or default)
     */
    public String getPlayerLocale(Player player) {
        try {
            // Try to get client locale
            String clientLocale = player.getLocale();
            if (clientLocale != null && messages.containsKey(clientLocale)) {
                return clientLocale;
            }
        } catch (Exception e) {
            // Fallback if locale not available
        }

        return defaultLocale;
    }

    /**
     * Set default locale
     */
    public void setDefaultLocale(String locale) {
        if (messages.containsKey(locale)) {
            this.defaultLocale = locale;
            plugin.getLogger().info("Default locale changed to: " + locale);
        } else {
            plugin.getLogger().warning("Locale not loaded: " + locale);
        }
    }

    /**
     * Get all available locales
     */
    public java.util.Set<String> getAvailableLocales() {
        return messages.keySet();
    }

    /**
     * Reload all locales
     */
    public void reload() {
        messages.clear();
        loadLocales();
    }

    /**
     * Check if locale is loaded
     */
    public boolean isLocaleLoaded(String locale) {
        return messages.containsKey(locale);
    }
}
