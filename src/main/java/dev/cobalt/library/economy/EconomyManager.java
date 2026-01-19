package dev.cobalt.library.economy;

import dev.cobalt.library.event.EventBus;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Economy manager with Vault integration
 */
public class EconomyManager {

    private final Plugin plugin;
    private final EventBus eventBus;
    private Economy economy;
    private boolean vaultAvailable = false;

    public EconomyManager(Plugin plugin, EventBus eventBus) {
        this.plugin = plugin;
        this.eventBus = eventBus;
    }

    /**
     * Hook into economy provider (Vault)
     */
    public void hookIntoEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("Vault not found - economy features disabled");
            return;
        }

        RegisteredServiceProvider<Economy> rsp = plugin.getServer()
                .getServicesManager()
                .getRegistration(Economy.class);

        if (rsp == null) {
            plugin.getLogger().warning("No economy provider found - economy features disabled");
            return;
        }

        economy = rsp.getProvider();
        vaultAvailable = true;
        plugin.getLogger().info("Hooked into economy: " + economy.getName());

        // Publish event
        eventBus.publish("economy.hooked", economy.getName());
    }

    /**
     * Check if economy is available
     */
    public boolean isAvailable() {
        return vaultAvailable && economy != null;
    }

    /**
     * Get player balance
     */
    public double getBalance(OfflinePlayer player) {
        if (!isAvailable()) return 0.0;
        return economy.getBalance(player);
    }

    /**
     * Check if player has enough money
     */
    public boolean has(OfflinePlayer player, double amount) {
        if (!isAvailable()) return false;
        return economy.has(player, amount);
    }

    /**
     * Deposit money to player
     */
    public boolean deposit(OfflinePlayer player, double amount) {
        if (!isAvailable()) return false;

        var response = economy.depositPlayer(player, amount);
        if (response.transactionSuccess()) {
            eventBus.publish("economy.deposit", new Transaction(player, amount));
            return true;
        }
        return false;
    }

    /**
     * Withdraw money from player
     */
    public boolean withdraw(OfflinePlayer player, double amount) {
        if (!isAvailable()) return false;

        var response = economy.withdrawPlayer(player, amount);
        if (response.transactionSuccess()) {
            eventBus.publish("economy.withdraw", new Transaction(player, amount));
            return true;
        }
        return false;
    }

    /**
     * Transfer money between players
     */
    public boolean transfer(OfflinePlayer from, OfflinePlayer to, double amount) {
        if (!isAvailable()) return false;

        if (!has(from, amount)) {
            return false;
        }

        if (withdraw(from, amount)) {
            if (deposit(to, amount)) {
                eventBus.publish("economy.transfer", new Transfer(from, to, amount));
                return true;
            } else {
                // Rollback
                deposit(from, amount);
                return false;
            }
        }

        return false;
    }

    /**
     * Create a bank account
     */
    public boolean createAccount(OfflinePlayer player) {
        if (!isAvailable()) return false;
        return economy.createPlayerAccount(player);
    }

    /**
     * Check if player has account
     */
    public boolean hasAccount(OfflinePlayer player) {
        if (!isAvailable()) return false;
        return economy.hasAccount(player);
    }

    /**
     * Format money amount
     */
    public String format(double amount) {
        if (!isAvailable()) return String.format("%.2f", amount);
        return economy.format(amount);
    }

    /**
     * Get currency name (singular)
     */
    public String getCurrencyName() {
        if (!isAvailable()) return "Dollar";
        return economy.currencyNameSingular();
    }

    /**
     * Get currency name (plural)
     */
    public String getCurrencyNamePlural() {
        if (!isAvailable()) return "Dollars";
        return economy.currencyNamePlural();
    }

    /**
     * Get economy provider
     */
    public Economy getEconomy() {
        return economy;
    }

    /**
     * Transaction data
     */
    public static class Transaction {
        private final OfflinePlayer player;
        private final double amount;

        public Transaction(OfflinePlayer player, double amount) {
            this.player = player;
            this.amount = amount;
        }

        public OfflinePlayer getPlayer() { return player; }
        public double getAmount() { return amount; }
    }

    /**
     * Transfer data
     */
    public static class Transfer {
        private final OfflinePlayer from;
        private final OfflinePlayer to;
        private final double amount;

        public Transfer(OfflinePlayer from, OfflinePlayer to, double amount) {
            this.from = from;
            this.to = to;
            this.amount = amount;
        }

        public OfflinePlayer getFrom() { return from; }
        public OfflinePlayer getTo() { return to; }
        public double getAmount() { return amount; }
    }
}
