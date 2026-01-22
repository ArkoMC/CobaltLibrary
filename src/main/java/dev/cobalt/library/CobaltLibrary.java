package dev.cobalt.library;

import dev.cobalt.library.cache.CacheManager;
import dev.cobalt.library.command.CommandRegistry;
import dev.cobalt.library.config.ConfigurationManager;
import dev.cobalt.library.database.DatabaseManager;
import dev.cobalt.library.di.DependencyContainer;
import dev.cobalt.library.economy.EconomyManager;
import dev.cobalt.library.event.EventBus;
import dev.cobalt.library.gui.GuiRegistry;
import dev.cobalt.library.i18n.LocalizationManager;
import dev.cobalt.library.logger.CobaltLogger;
import dev.cobalt.library.messaging.MessageBroker;
import dev.cobalt.library.metrics.MetricsCollector;
import dev.cobalt.library.playerdata.PlayerDataRegistry;
import dev.cobalt.library.plugin.PluginRegistry;
import dev.cobalt.library.scheduler.AdvancedScheduler;
import dev.cobalt.library.security.SecurityManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * CobaltAPI - The Ultimate Static API Plugin
 *
 * A comprehensive, production-ready API that provides enterprise-grade
 * features to all Minecraft plugins on the server.
 *
 * @version 2.0.0
 * @author Bimulu
 */
public final class CobaltLibrary extends JavaPlugin {

    private static CobaltLibrary instance;
    private static volatile boolean initialized = false;

    // Core Services
    private DependencyContainer container;
    private EventBus eventBus;
    private PluginRegistry pluginRegistry;
    private ConfigurationManager configurationManager;
    private DatabaseManager databaseManager;
    private CacheManager cacheManager;
    private MessageBroker messageBroker;
    private PlayerDataRegistry playerDataRegistry;
    private CommandRegistry commandRegistry;
    private GuiRegistry guiRegistry;
    private MetricsCollector metricsCollector;
    private LocalizationManager localizationManager;
    private EconomyManager economyManager;
    private dev.cobalt.library.security.SecurityManager securityManager;
    private CobaltLogger logger;

    @Override
    public void onLoad() {
        instance = this;
        logger = new CobaltLogger(this);

        logger.info("╔════════════════════════════════════════════════════════════════╗");
        logger.info("║       ____ ___  ____    _    _   _____   _     ___ ____        ║");
        logger.info("║    / ___/ _ \\| __ )  / \\  | | |_   _| | |   |_ _| __ )    ║");
        logger.info("║    | |  | | | |  _ \\ / _ \\ | |   | |   | |    | ||  _ \\     ║");
        logger.info("║    | |__| |_| | |_) / ___ \\| |___| |   | |___ | || |_) |     ║");
        logger.info("║    \\____\\___/|____/_/   \\_\\_____|_|   |_____|___|____/     ║");
        logger.info("╚════════════════════════════════════════════════════════════════╝");

        // Initialize dependency container first
        container = new DependencyContainer(this);
        registerCoreServices();
    }

    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();

        try {
            // Initialize in dependency order
            initializeCore().thenCompose(v -> initializeMessaging())
                    .thenCompose(v -> initializeFrameworks())
                    .thenCompose(v -> initializeIntegrations())
                    .thenRun(() -> onStartupComplete(startTime))
                    .exceptionally(throwable -> {
                        logger.severe("Fatal error during initialization", throwable);
                        getServer().getPluginManager().disablePlugin(this);
                        return null;
                    });

        } catch (Exception e) {
            logger.severe("Failed to initialize CobaltAPI", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private CompletableFuture<Void> initializeCore() {
        //return CompletableFuture.runAsync(() -> {
        //            logger.info("→ Initializing core services...");
        //
        //            saveDefaultConfig();
        //            configurationManager = new ConfigurationManager(this);
        //            securityManager = new dev.cobalt.library.security.SecurityManager(this);
        //            eventBus = new EventBus(this);
        //            pluginRegistry = new PluginRegistry(this, eventBus);
        //            metricsCollector = new MetricsCollector(this);
        //
        //            logger.success("✓ Core services initialized");
        //        }, scheduler.getAsyncExecutor());
        return null;
    }

    private CompletableFuture<Void> initializeMessaging() {
        //return CompletableFuture.runAsync(() -> {
        //            logger.info("→ Initializing messaging systems...");
        //
        //            messageBroker = new MessageBroker(this, configurationManager);
        //            messageBroker.connect().join();
        //
        //            logger.success("✓ Messaging systems initialized");
        //        }, scheduler.getAsyncExecutor());
        return null;
    }

    private CompletableFuture<Void> initializeFrameworks() {
        //return CompletableFuture.runAsync(() -> {
        //            logger.info("→ Initializing frameworks...");
        //
        //            localizationManager = new LocalizationManager(this, configurationManager);
        //            commandRegistry = new CommandRegistry(this, eventBus);
        //            guiRegistry = new GuiRegistry(this, eventBus);
        //
        //            logger.success("✓ Frameworks initialized");
        //        }, scheduler.getAsyncExecutor());
        return null;
    }

    private CompletableFuture<Void> initializeIntegrations() {
        // return CompletableFuture.runAsync(() -> {
        //            logger.info("→ Initializing integrations...");
        //
        //            economyManager = new EconomyManager(this, eventBus);
        //            economyManager.hookIntoEconomy();
        //
        //            logger.success("✓ Integrations initialized");
        //        }, scheduler.getAsyncExecutor());
        return null;
    }

    private void onStartupComplete(long startTime) {
        initialized = true;
        long duration = System.currentTimeMillis() - startTime;

        // Start metrics collection
        metricsCollector.startCollection();

        // Announce to other plugins
        eventBus.publish("cobalt.api.ready", this);

        logger.info("╔════════════════════════════════════════╗");
        logger.info("║   CobaltAPI Successfully Started!     ║");
        logger.info("║   Startup time: " + String.format("%-4dms", duration) + "                ║");
        logger.info("╚════════════════════════════════════════╝");

        // Display registered services
        displayServicesSummary();
    }

    @Override
    public void onDisable() {
        if (!initialized) {
            return;
        }

        logger.info("Shutting down CobaltAPI...");
        long startTime = System.currentTimeMillis();

        try {
            // Shutdown in reverse dependency order
            shutdownGracefully().get(30, TimeUnit.SECONDS);

        } catch (Exception e) {
            logger.severe("Error during shutdown", e);
        }

        long duration = System.currentTimeMillis() - startTime;
        logger.info("CobaltAPI shut down in " + duration + "ms");
    }

    private CompletableFuture<Void> shutdownGracefully() {
        return CompletableFuture.allOf(
                CompletableFuture.runAsync(() -> {
                    if (commandRegistry != null) commandRegistry.unregisterAll();
                    if (guiRegistry != null) guiRegistry.closeAll();
                }),
                CompletableFuture.runAsync(() -> {
                    if (messageBroker != null) messageBroker.disconnect();
                }),
                CompletableFuture.runAsync(() -> {
                    if (playerDataRegistry != null) playerDataRegistry.saveAll().join();
                    if (databaseManager != null) databaseManager.disconnect();
                    if (cacheManager != null) cacheManager.shutdown();
                }),
                CompletableFuture.runAsync(() -> {
                    if (metricsCollector != null) metricsCollector.stopCollection();
                })
        ).thenRun(() -> {
            if (eventBus != null) {
                eventBus.publish("cobalt.api.shutdown", this);
                eventBus.shutdown();
            }
        });
    }

    private void registerCoreServices() {
        // Register services in the DI container
        container.singleton(CobaltLibrary.class, () -> this);
        container.singleton(CobaltLogger.class, () -> logger);
    }

    private void displayServicesSummary() {
        logger.info("");
        logger.info("▸ Registered Services:");
        logger.info("  • Event Bus: " + eventBus.getListenerCount() + " listeners");
        logger.info("  • Plugins: " + pluginRegistry.getRegisteredPluginCount() + " registered");
        logger.info("  • Commands: " + commandRegistry.getRegisteredCommandCount() + " registered");
        logger.info("  • GUIs: " + guiRegistry.getActiveGuiCount() + " active");
        logger.info("  • Database: " + databaseManager.getDatabaseType() + " (" +
                (databaseManager.isConnected() ? "connected" : "disconnected") + ")");
        logger.info("  • Cache: " + cacheManager.getCacheProvider() + " (" +
                cacheManager.getSize() + " entries)");
        logger.info("  • Message Broker: " + messageBroker.getBrokerType() + " (" +
                (messageBroker.isConnected() ? "connected" : "disconnected") + ")");
        logger.info("");
    }

    // ==================== Static API Access ====================

    /**
     * Get the CobaltAPI instance
     * @return The plugin instance
     * @throws IllegalStateException if API is not initialized
     */
    public static CobaltLibrary getInstance() {
        if (instance == null || !initialized) {
            throw new IllegalStateException("CobaltAPI is not initialized yet!");
        }
        return instance;
    }

    /**
     * Check if CobaltAPI is ready
     * @return true if fully initialized and ready
     */
    public static boolean isReady() {
        return initialized && instance != null;
    }

    // ==================== Service Accessors ====================

    public static DependencyContainer getContainer() {
        return getInstance().container;
    }

    public static EventBus getEventBus() {
        return getInstance().eventBus;
    }

    public static PluginRegistry getPluginRegistry() {
        return getInstance().pluginRegistry;
    }

    public static ConfigurationManager getConfigurationManager() {
        return getInstance().configurationManager;
    }

    public static DatabaseManager getDatabaseManager() {
        return getInstance().databaseManager;
    }

    public static CacheManager getCacheManager() {
        return getInstance().cacheManager;
    }

    public static MessageBroker getMessageBroker() {
        return getInstance().messageBroker;
    }

    public static PlayerDataRegistry getPlayerDataRegistry() {
        return getInstance().playerDataRegistry;
    }

    public static CommandRegistry getCommandRegistry() {
        return getInstance().commandRegistry;
    }

    public static GuiRegistry getGuiRegistry() {
        return getInstance().guiRegistry;
    }

    //public static AdvancedScheduler getScheduler() {
    //    return getInstance().scheduler;
    //}

    public static MetricsCollector getMetricsCollector() {
        return getInstance().metricsCollector;
    }

    public static LocalizationManager getLocalizationManager() {
        return getInstance().localizationManager;
    }

    public static EconomyManager getEconomyManager() {
        return getInstance().economyManager;
    }

    public static SecurityManager getSecurityManager() {
        return getInstance().securityManager;
    }

    public static CobaltLogger log() {
        return getInstance().logger;
    }
}
