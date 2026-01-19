package dev.cobalt.library.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * Advanced scheduler with async and sync task execution
 */
public class AdvancedScheduler {

    private final Plugin plugin;
    private final ExecutorService asyncExecutor;
    private final ScheduledExecutorService scheduledExecutor;

    public AdvancedScheduler(Plugin plugin) {
        this.plugin = plugin;
        this.asyncExecutor = Executors.newCachedThreadPool();
        this.scheduledExecutor = Executors.newScheduledThreadPool(4);
    }

    /**
     * Get async executor for CompletableFuture
     */
    public Executor getAsyncExecutor() {
        return asyncExecutor;
    }

    /**
     * Run task asynchronously
     */
    public CompletableFuture<Void> runAsync(Runnable task) {
        return CompletableFuture.runAsync(task, asyncExecutor);
    }

    /**
     * Run task asynchronously with result
     */
    public <T> CompletableFuture<T> supplyAsync(Supplier<T> task) {
        return CompletableFuture.supplyAsync(task, asyncExecutor);
    }

    /**
     * Run task on main thread
     */
    public BukkitTask runSync(Runnable task) {
        return Bukkit.getScheduler().runTask(plugin, task);
    }

    /**
     * Run task on main thread after delay
     */
    public BukkitTask runSyncLater(Runnable task, long delayTicks) {
        return Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
    }

    /**
     * Run repeating task on main thread
     */
    public BukkitTask runSyncTimer(Runnable task, long delayTicks, long periodTicks) {
        return Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
    }

    /**
     * Run task asynchronously after delay
     */
    public BukkitTask runAsyncLater(Runnable task, long delayTicks) {
        return Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, delayTicks);
    }

    /**
     * Run repeating task asynchronously
     */
    public BukkitTask runAsyncTimer(Runnable task, long delayTicks, long periodTicks) {
        return Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delayTicks, periodTicks);
    }

    /**
     * Schedule task with delay (using ScheduledExecutorService)
     */
    public ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit) {
        return scheduledExecutor.schedule(task, delay, unit);
    }

    /**
     * Schedule repeating task at fixed rate
     */
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit unit) {
        return scheduledExecutor.scheduleAtFixedRate(task, initialDelay, period, unit);
    }

    /**
     * Schedule repeating task with fixed delay between executions
     */
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long initialDelay, long delay, TimeUnit unit) {
        return scheduledExecutor.scheduleWithFixedDelay(task, initialDelay, delay, unit);
    }

    /**
     * Shutdown all executors
     */
    public void shutdown() {
        asyncExecutor.shutdown();
        scheduledExecutor.shutdown();

        try {
            if (!asyncExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                asyncExecutor.shutdownNow();
            }
            if (!scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduledExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            asyncExecutor.shutdownNow();
            scheduledExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
