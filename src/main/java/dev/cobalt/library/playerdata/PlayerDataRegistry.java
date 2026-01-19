package dev.cobalt.library.playerdata;

import dev.cobalt.library.cache.CacheManager;
import dev.cobalt.library.database.DatabaseManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player data with cache-backed database storage
 */
public class PlayerDataRegistry {

    private final Plugin plugin;
    private final DatabaseManager database;
    private final CacheManager cache;
    private final Map<UUID, PlayerData> loadedData = new ConcurrentHashMap<>();

    public PlayerDataRegistry(Plugin plugin, DatabaseManager db, CacheManager cache) {
        this.plugin = plugin;
        this.database = db;
        this.cache = cache;
    }

    /**
     * Load player data (from cache or database)
     */
    public CompletableFuture<PlayerData> load(UUID playerId) {
        // Check if already loaded in memory
        if (loadedData.containsKey(playerId)) {
            return CompletableFuture.completedFuture(loadedData.get(playerId));
        }

        // Check cache
        String cacheKey = "playerdata:" + playerId;
        PlayerData cached = cache.get(cacheKey);
        if (cached != null) {
            loadedData.put(playerId, cached);
            return CompletableFuture.completedFuture(cached);
        }

        // Load from database
        return CompletableFuture.supplyAsync(() -> {
            PlayerData data = new PlayerData(playerId);
            // TODO: Load from database when database queries are implemented

            // Store in cache and memory
            cache.put(cacheKey, data);
            loadedData.put(playerId, data);

            return data;
        });
    }

    /**
     * Load player data by Player object
     */
    public CompletableFuture<PlayerData> load(Player player) {
        return load(player.getUniqueId());
    }

    /**
     * Save player data (to cache and schedule database write)
     */
    public CompletableFuture<Void> save(UUID playerId) {
        PlayerData data = loadedData.get(playerId);
        if (data == null) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            // Save to cache immediately
            String cacheKey = "playerdata:" + playerId;
            cache.put(cacheKey, data);

            // TODO: Save to database when database queries are implemented
        });
    }

    /**
     * Save player data by Player object
     */
    public CompletableFuture<Void> save(Player player) {
        return save(player.getUniqueId());
    }

    /**
     * Save all loaded player data
     */
    public CompletableFuture<Void> saveAll() {
        return CompletableFuture.allOf(
                loadedData.keySet().stream()
                        .map(this::save)
                        .toArray(CompletableFuture[]::new)
        );
    }

    /**
     * Unload player data from memory (after saving)
     */
    public CompletableFuture<Void> unload(UUID playerId) {
        return save(playerId).thenRun(() -> {
            loadedData.remove(playerId);
        });
    }

    /**
     * Get loaded player data (returns null if not loaded)
     */
    public PlayerData get(UUID playerId) {
        return loadedData.get(playerId);
    }

    /**
     * Get loaded player data by Player object
     */
    public PlayerData get(Player player) {
        return get(player.getUniqueId());
    }

    /**
     * Check if player data is loaded
     */
    public boolean isLoaded(UUID playerId) {
        return loadedData.containsKey(playerId);
    }

    /**
     * Simple PlayerData class - expand as needed
     */
    public static class PlayerData {
        private final UUID playerId;
        private final Map<String, Object> data = new ConcurrentHashMap<>();

        public PlayerData(UUID playerId) {
            this.playerId = playerId;
        }

        public UUID getPlayerId() {
            return playerId;
        }

        public void set(String key, Object value) {
            data.put(key, value);
        }

        @SuppressWarnings("unchecked")
        public <T> T get(String key) {
            return (T) data.get(key);
        }

        public <T> T get(String key, T defaultValue) {
            return data.containsKey(key) ? get(key) : defaultValue;
        }

        public boolean has(String key) {
            return data.containsKey(key);
        }

        public void remove(String key) {
            data.remove(key);
        }

        public Map<String, Object> getAll() {
            return new ConcurrentHashMap<>(data);
        }
    }
}
