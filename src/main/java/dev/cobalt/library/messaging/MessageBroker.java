package dev.cobalt.library.messaging;

import dev.cobalt.library.config.ConfigurationManager;
import org.bukkit.plugin.Plugin;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Message broker for cross-server communication using Redis pub/sub
 */
public class MessageBroker {

    private final Plugin plugin;
    private final ConfigurationManager config;
    private JedisPool jedisPool;
    private final Map<String, ChannelSubscription> subscriptions = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private boolean connected = false;
    private String brokerType;

    public MessageBroker(Plugin plugin, ConfigurationManager config) {
        this.plugin = plugin;
        this.config = config;
        this.brokerType = config.getString("messaging.type", "none").toLowerCase();
    }

    /**
     * Connect to message broker
     */
    public CompletableFuture<Void> connect() {
        return CompletableFuture.runAsync(() -> {
            if (brokerType.equals("redis")) {
                connectRedis();
            } else {
                plugin.getLogger().info("Message broker disabled (type: " + brokerType + ")");
            }
        });
    }

    private void connectRedis() {
        try {
            String host = config.getString("messaging.redis.host", "localhost");
            int port = config.getInt("messaging.redis.port", 6379);
            String password = config.getString("messaging.redis.password", "");

            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(config.getInt("messaging.redis.pool-size", 10));
            poolConfig.setMaxIdle(config.getInt("messaging.redis.max-idle", 5));
            poolConfig.setMinIdle(config.getInt("messaging.redis.min-idle", 1));

            if (password.isEmpty()) {
                jedisPool = new JedisPool(poolConfig, host, port);
            } else {
                jedisPool = new JedisPool(poolConfig, host, port, 2000, password);
            }

            // Test connection
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.ping();
            }

            connected = true;
            plugin.getLogger().info("Connected to Redis at " + host + ":" + port);

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to connect to Redis: " + e.getMessage());
            connected = false;
        }
    }

    /**
     * Publish message to channel
     */
    public void publish(String channel, String message) {
        if (!connected || jedisPool == null) return;

        executor.submit(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.publish(channel, message);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to publish message: " + e.getMessage());
            }
        });
    }

    /**
     * Subscribe to channel
     */
    public void subscribe(String channel, Consumer<String> handler) {
        if (!connected || jedisPool == null) return;

        ChannelSubscription subscription = new ChannelSubscription(channel, handler);
        subscriptions.put(channel, subscription);

        executor.submit(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.subscribe(subscription.pubSub, channel);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to subscribe to channel " + channel + ": " + e.getMessage());
            }
        });
    }

    /**
     * Unsubscribe from channel
     */
    public void unsubscribe(String channel) {
        ChannelSubscription subscription = subscriptions.remove(channel);
        if (subscription != null) {
            subscription.pubSub.unsubscribe();
        }
    }

    /**
     * Unsubscribe from all channels
     */
    public void unsubscribeAll() {
        subscriptions.values().forEach(sub -> sub.pubSub.unsubscribe());
        subscriptions.clear();
    }

    /**
     * Disconnect from broker
     */
    public void disconnect() {
        unsubscribeAll();

        if (jedisPool != null) {
            jedisPool.close();
            jedisPool = null;
        }

        executor.shutdown();
        connected = false;
        plugin.getLogger().info("Disconnected from message broker");
    }

    /**
     * Get broker type
     */
    public String getBrokerType() {
        return brokerType;
    }

    /**
     * Check if connected
     */
    public boolean isConnected() {
        return connected;
    }

    /**
     * Get active subscription count
     */
    public int getSubscriptionCount() {
        return subscriptions.size();
    }

    /**
     * Channel subscription wrapper
     */
    private class ChannelSubscription {
        final String channel;
        final Consumer<String> handler;
        final JedisPubSub pubSub;

        ChannelSubscription(String channel, Consumer<String> handler) {
            this.channel = channel;
            this.handler = handler;
            this.pubSub = new JedisPubSub() {
                @Override
                public void onMessage(String channel, String message) {
                    try {
                        handler.accept(message);
                    } catch (Exception e) {
                        plugin.getLogger().severe("Error handling message on channel " + channel + ": " + e.getMessage());
                    }
                }
            };
        }
    }
}
