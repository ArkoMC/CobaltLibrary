package dev.cobalt.library.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.cobalt.library.config.ConfigurationManager;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;

/**
 * High-performance in-memory cache using Caffeine
 */
public class CacheManager {

    private final Plugin plugin;
    private final ConfigurationManager config;
    private final Cache<String, Object> cache;

    public CacheManager(Plugin plugin, ConfigurationManager config) {
        this.plugin = plugin;
        this.config = config;

        // Configure cache
        int maxSize = config.getInt("cache.max-size", 10000);
        int expireMinutes = config.getInt("cache.expire-minutes", 30);

        this.cache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireMinutes, TimeUnit.MINUTES)
                .recordStats()
                .build();

        plugin.getLogger().info("Cache initialized: max=" + maxSize + ", expire=" + expireMinutes + "m");
    }

    /**
     * Get a value from cache
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) cache.getIfPresent(key);
    }

    /**
     * Get or compute a value
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, java.util.function.Function<String, T> loader) {
        return (T) cache.get(key, k -> loader.apply(k));
    }

    /**
     * Put a value in cache
     */
    public void put(String key, Object value) {
        cache.put(key, value);
    }

    /**
     * Remove a value from cache
     */
    public void invalidate(String key) {
        cache.invalidate(key);
    }

    /**
     * Clear all cache
     */
    public void invalidateAll() {
        cache.invalidateAll();
    }

    /**
     * Check if key exists
     */
    public boolean contains(String key) {
        return cache.getIfPresent(key) != null;
    }

    /**
     * Get cache provider name
     */
    public String getCacheProvider() {
        return "Caffeine";
    }

    /**
     * Get current cache size
     */
    public int getSize() {
        return (int) cache.estimatedSize();
    }

    /**
     * Get cache statistics
     */
    public String getStats() {
        var stats = cache.stats();
        return String.format(
                "Hits: %d, Misses: %d, Hit Rate: %.2f%%",
                stats.hitCount(),
                stats.missCount(),
                stats.hitRate() * 100
        );
    }

    /**
     * Shutdown cache
     */
    public void shutdown() {
        cache.invalidateAll();
        cache.cleanUp();
    }
}
