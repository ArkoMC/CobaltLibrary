package dev.cobalt.library.metrics;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Metrics collector for performance monitoring
 */
public class MetricsCollector {

    private final Plugin plugin;
    private final Map<String, Counter> counters = new ConcurrentHashMap<>();
    private final Map<String, Gauge> gauges = new ConcurrentHashMap<>();
    private final Map<String, Timer> timers = new ConcurrentHashMap<>();
    private ScheduledExecutorService scheduler;
    private boolean collecting = false;

    public MetricsCollector(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Start metrics collection
     */
    public void startCollection() {
        if (collecting) return;

        scheduler = Executors.newScheduledThreadPool(1);
        collecting = true;

        // Collect system metrics every 30 seconds
        scheduler.scheduleAtFixedRate(this::collectSystemMetrics, 0, 30, TimeUnit.SECONDS);

        plugin.getLogger().info("Metrics collection started");
    }

    /**
     * Stop metrics collection
     */
    public void stopCollection() {
        if (!collecting) return;

        if (scheduler != null) {
            scheduler.shutdown();
            try {
                scheduler.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        collecting = false;
        plugin.getLogger().info("Metrics collection stopped");
    }

    /**
     * Increment a counter
     */
    public void increment(String name) {
        increment(name, 1);
    }

    /**
     * Increment a counter by amount
     */
    public void increment(String name, long amount) {
        counters.computeIfAbsent(name, k -> new Counter()).add(amount);
    }

    /**
     * Set a gauge value
     */
    public void gauge(String name, double value) {
        gauges.computeIfAbsent(name, k -> new Gauge()).set(value);
    }

    /**
     * Start a timer
     */
    public TimerContext startTimer(String name) {
        Timer timer = timers.computeIfAbsent(name, k -> new Timer());
        return new TimerContext(timer);
    }

    /**
     * Get counter value
     */
    public long getCounter(String name) {
        Counter counter = counters.get(name);
        return counter != null ? counter.get() : 0;
    }

    /**
     * Get gauge value
     */
    public double getGauge(String name) {
        Gauge gauge = gauges.get(name);
        return gauge != null ? gauge.get() : 0.0;
    }

    /**
     * Get timer statistics
     */
    public TimerStats getTimerStats(String name) {
        Timer timer = timers.get(name);
        return timer != null ? timer.getStats() : new TimerStats(0, 0, 0, 0);
    }

    /**
     * Collect system metrics
     */
    private void collectSystemMetrics() {
        Runtime runtime = Runtime.getRuntime();

        // Memory metrics
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();

        gauge("system.memory.used", usedMemory / 1024.0 / 1024.0); // MB
        gauge("system.memory.free", freeMemory / 1024.0 / 1024.0); // MB
        gauge("system.memory.max", maxMemory / 1024.0 / 1024.0); // MB
        gauge("system.memory.usage", (usedMemory * 100.0) / maxMemory); // %

        // Server metrics
        gauge("server.players.online", Bukkit.getOnlinePlayers().size());
        gauge("server.tps", getTPS());
    }

    /**
     * Get server TPS (approximate)
     */
    private double getTPS() {
        try {
            return Bukkit.getTPS()[0]; // 1-minute average
        } catch (Exception e) {
            return 20.0; // Default TPS
        }
    }

    /**
     * Get all metrics as string
     */
    public String getMetricsReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Metrics Report ===\n");

        sb.append("\nCounters:\n");
        counters.forEach((name, counter) ->
                sb.append(String.format("  %s: %d\n", name, counter.get())));

        sb.append("\nGauges:\n");
        gauges.forEach((name, gauge) ->
                sb.append(String.format("  %s: %.2f\n", name, gauge.get())));

        sb.append("\nTimers:\n");
        timers.forEach((name, timer) -> {
            TimerStats stats = timer.getStats();
            sb.append(String.format("  %s: count=%d, avg=%.2fms, min=%.2fms, max=%.2fms\n",
                    name, stats.count, stats.avgMs, stats.minMs, stats.maxMs));
        });

        return sb.toString();
    }

    /**
     * Reset all metrics
     */
    public void reset() {
        counters.clear();
        gauges.clear();
        timers.clear();
    }

    /**
     * Counter class
     */
    private static class Counter {
        private final AtomicLong value = new AtomicLong(0);

        void add(long amount) {
            value.addAndGet(amount);
        }

        long get() {
            return value.get();
        }
    }

    /**
     * Gauge class
     */
    private static class Gauge {
        private volatile double value = 0.0;

        void set(double value) {
            this.value = value;
        }

        double get() {
            return value;
        }
    }

    /**
     * Timer class
     */
    private static class Timer {
        private final AtomicLong count = new AtomicLong(0);
        private final AtomicLong totalNanos = new AtomicLong(0);
        private volatile long minNanos = Long.MAX_VALUE;
        private volatile long maxNanos = Long.MIN_VALUE;

        void record(long nanos) {
            count.incrementAndGet();
            totalNanos.addAndGet(nanos);

            // Update min
            long currentMin = minNanos;
            while (nanos < currentMin) {
                if (minNanos == currentMin) {
                    minNanos = nanos;
                    break;
                }
                currentMin = minNanos;
            }

            // Update max
            long currentMax = maxNanos;
            while (nanos > currentMax) {
                if (maxNanos == currentMax) {
                    maxNanos = nanos;
                    break;
                }
                currentMax = maxNanos;
            }
        }

        TimerStats getStats() {
            long cnt = count.get();
            if (cnt == 0) {
                return new TimerStats(0, 0, 0, 0);
            }

            double avgMs = (totalNanos.get() / (double) cnt) / 1_000_000.0;
            double minMs = minNanos / 1_000_000.0;
            double maxMs = maxNanos / 1_000_000.0;

            return new TimerStats(cnt, avgMs, minMs, maxMs);
        }
    }

    /**
     * Timer context for automatic timing
     */
    public static class TimerContext implements AutoCloseable {
        private final Timer timer;
        private final long startNanos;

        TimerContext(Timer timer) {
            this.timer = timer;
            this.startNanos = System.nanoTime();
        }

        @Override
        public void close() {
            long elapsed = System.nanoTime() - startNanos;
            timer.record(elapsed);
        }
    }

    /**
     * Timer statistics
     */
    public static class TimerStats {
        public final long count;
        public final double avgMs;
        public final double minMs;
        public final double maxMs;

        TimerStats(long count, double avgMs, double minMs, double maxMs) {
            this.count = count;
            this.avgMs = avgMs;
            this.minMs = minMs;
            this.maxMs = maxMs;
        }
    }
}
