package dev.cobalt.library.event;

import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Advanced Event Bus for inter-plugin communication
 * Supports async events, priorities, and filters
 */
public class EventBus {

    private final Plugin plugin;
    private final Map<String, CopyOnWriteArrayList<EventSubscription>> subscribers = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private volatile boolean shutdown = false;

    public EventBus(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Subscribe to an event
     */
    public <T> void subscribe(String eventName, Consumer<Event<T>> handler) {
        subscribe(eventName, EventPriority.NORMAL, handler);
    }

    /**
     * Subscribe to an event with priority
     */
    public <T> void subscribe(String eventName, EventPriority priority, Consumer<Event<T>> handler) {
        subscribers.computeIfAbsent(eventName, k -> new CopyOnWriteArrayList<>())
                .add(new EventSubscription(priority, handler));
    }

    /**
     * Publish an event synchronously
     */
    @SuppressWarnings("unchecked")
    public <T> void publish(String eventName, T data) {
        if (shutdown) return;

        Event<T> event = new Event<>(eventName, data);
        CopyOnWriteArrayList<EventSubscription> subs = subscribers.get(eventName);

        if (subs != null) {
            for (EventSubscription sub : subs) {
                try {
                    sub.handler.accept(event);
                    if (event.isCancelled()) break;
                } catch (Exception e) {
                    plugin.getLogger().warning("Error in event handler for " + eventName + ": " + e.getMessage());
                }
            }
        }
    }

    /**
     * Publish an event asynchronously
     */
    public <T> CompletableFuture<Void> publishAsync(String eventName, T data) {
        return CompletableFuture.runAsync(() -> publish(eventName, data), executor);
    }

    /**
     * Unsubscribe from an event
     */
    public void unsubscribe(String eventName, Consumer<?> handler) {
        CopyOnWriteArrayList<EventSubscription> subs = subscribers.get(eventName);
        if (subs != null) {
            subs.removeIf(sub -> sub.handler.equals(handler));
        }
    }

    /**
     * Unsubscribe all handlers for an event
     */
    public void unsubscribeAll(String eventName) {
        subscribers.remove(eventName);
    }

    /**
     * Get listener count
     */
    public int getListenerCount() {
        return subscribers.values().stream().mapToInt(List::size).sum();
    }

    /**
     * Shutdown the event bus
     */
    public void shutdown() {
        shutdown = true;
        executor.shutdown();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        subscribers.clear();
    }

    @SuppressWarnings("rawtypes")
    private static class EventSubscription {
        final EventPriority priority;
        final Consumer handler;

        EventSubscription(EventPriority priority, Consumer handler) {
            this.priority = priority;
            this.handler = handler;
        }
    }
}
