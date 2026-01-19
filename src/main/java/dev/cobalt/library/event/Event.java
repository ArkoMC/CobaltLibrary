package dev.cobalt.library.event;

/**
 * Event wrapper for event bus
 */
public class Event<T> {

    private final String name;
    private final T data;
    private final long timestamp;
    private boolean cancelled = false;

    public Event(String name, T data) {
        this.name = name;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    public String getName() {
        return name;
    }

    public T getData() {
        return data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
