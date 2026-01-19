package dev.cobalt.library.logger;

import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;

import java.util.logging.Level;

/**
 * Enhanced logger with colored console output and formatting
 */
public class CobaltLogger {

    private final Plugin plugin;
    private final String prefix;
    private boolean debug = false;

    public CobaltLogger(Plugin plugin) {
        this.plugin = plugin;
        this.prefix = "[" + plugin.getName() + "] ";
    }

    public CobaltLogger(Plugin plugin, boolean debug) {
        this(plugin);
        this.debug = debug;
    }

    /**
     * Log info message
     */
    public void info(String msg) {
        plugin.getLogger().info(msg);
    }

    /**
     * Log success message with checkmark
     */
    public void success(String msg) {
        plugin.getLogger().info(ChatColor.GREEN + "✓ " + ChatColor.RESET + msg);
    }

    /**
     * Log warning message
     */
    public void warning(String msg) {
        plugin.getLogger().warning(msg);
    }

    /**
     * Log severe/error message
     */
    public void severe(String msg) {
        plugin.getLogger().severe(msg);
    }

    /**
     * Log severe/error message with throwable
     */
    public void severe(String msg, Throwable t) {
        plugin.getLogger().severe(msg);
        if (t != null) {
            t.printStackTrace();
        }
    }

    /**
     * Log error (alias for severe)
     */
    public void error(String msg) {
        severe(msg);
    }

    /**
     * Log error with throwable
     */
    public void error(String msg, Throwable t) {
        severe(msg, t);
    }

    /**
     * Log debug message (only if debug enabled)
     */
    public void debug(String msg) {
        if (debug) {
            plugin.getLogger().info(ChatColor.GRAY + "[DEBUG] " + ChatColor.RESET + msg);
        }
    }

    /**
     * Log with custom level
     */
    public void log(Level level, String msg) {
        plugin.getLogger().log(level, msg);
    }

    /**
     * Log with custom level and throwable
     */
    public void log(Level level, String msg, Throwable t) {
        plugin.getLogger().log(level, msg, t);
    }

    /**
     * Log formatted message
     */
    public void format(String format, Object... args) {
        info(String.format(format, args));
    }

    /**
     * Log header with box
     */
    public void header(String title) {
        int length = title.length() + 4;
        String line = "═".repeat(Math.max(0, length));

        info("╔" + line + "╗");
        info("║  " + title + "  ║");
        info("╚" + line + "╝");
    }

    /**
     * Log section separator
     */
    public void separator() {
        info("─────────────────────────────────────────");
    }

    /**
     * Log bullet point
     */
    public void bullet(String msg) {
        info("• " + msg);
    }

    /**
     * Log arrow point
     */
    public void arrow(String msg) {
        info("→ " + msg);
    }

    /**
     * Enable debug mode
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
     * Check if debug is enabled
     */
    public boolean isDebug() {
        return debug;
    }

    /**
     * Get the underlying plugin logger
     */
    public java.util.logging.Logger getLogger() {
        return plugin.getLogger();
    }
}
