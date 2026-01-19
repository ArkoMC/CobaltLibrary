package dev.cobalt.library.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.cobalt.library.config.ConfigurationManager;
import org.bukkit.plugin.Plugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;

/**
 * Database manager with connection pooling (HikariCP)
 * Supports MySQL, PostgreSQL, SQLite, H2
 */
public class DatabaseManager {

    private final Plugin plugin;
    private final ConfigurationManager config;
    private HikariDataSource dataSource;
    private String databaseType;

    public DatabaseManager(Plugin plugin, ConfigurationManager config) {
        this.plugin = plugin;
        this.config = config;
        this.databaseType = config.getString("database.type", "sqlite").toLowerCase();
    }

    /**
     * Connect to database
     */
    public CompletableFuture<Void> connect() {
        return CompletableFuture.runAsync(() -> {
            try {
                HikariConfig hikariConfig = new HikariConfig();

                switch (databaseType) {
                    case "mysql":
                        setupMySQL(hikariConfig);
                        break;
                    case "postgresql":
                        setupPostgreSQL(hikariConfig);
                        break;
                    case "h2":
                        setupH2(hikariConfig);
                        break;
                    case "sqlite":
                    default:
                        setupSQLite(hikariConfig);
                        break;
                }

                hikariConfig.setMaximumPoolSize(config.getInt("database.pool-size", 10));
                hikariConfig.setMinimumIdle(config.getInt("database.min-idle", 2));
                hikariConfig.setConnectionTimeout(config.getInt("database.timeout", 30000));
                hikariConfig.setIdleTimeout(config.getInt("database.idle-timeout", 600000));
                hikariConfig.setMaxLifetime(config.getInt("database.max-lifetime", 1800000));

                dataSource = new HikariDataSource(hikariConfig);
                plugin.getLogger().info("Database connected: " + databaseType);

            } catch (Exception e) {
                plugin.getLogger().severe("Failed to connect to database: " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }

    private void setupMySQL(HikariConfig config) {
        String host = this.config.getString("database.host", "localhost");
        int port = this.config.getInt("database.port", 3306);
        String database = this.config.getString("database.database", "minecraft");
        String username = this.config.getString("database.username", "root");
        String password = this.config.getString("database.password", "");

        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
    }

    private void setupPostgreSQL(HikariConfig config) {
        String host = this.config.getString("database.host", "localhost");
        int port = this.config.getInt("database.port", 5432);
        String database = this.config.getString("database.database", "minecraft");
        String username = this.config.getString("database.username", "postgres");
        String password = this.config.getString("database.password", "");

        config.setJdbcUrl("jdbc:postgresql://" + host + ":" + port + "/" + database);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("org.postgresql.Driver");
    }

    private void setupSQLite(HikariConfig config) {
        String file = this.config.getString("database.file", "database.db");
        String path = plugin.getDataFolder().getAbsolutePath() + "/" + file;

        config.setJdbcUrl("jdbc:sqlite:" + path);
        config.setDriverClassName("org.sqlite.JDBC");
        config.setMaximumPoolSize(1); // SQLite doesn't support multiple connections
    }

    private void setupH2(HikariConfig config) {
        String file = this.config.getString("database.file", "database");
        String path = plugin.getDataFolder().getAbsolutePath() + "/" + file;

        config.setJdbcUrl("jdbc:h2:" + path);
        config.setDriverClassName("org.h2.Driver");
    }

    /**
     * Get a connection from the pool
     */
    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("Database is not connected");
        }
        return dataSource.getConnection();
    }

    /**
     * Execute a query asynchronously
     */
    public CompletableFuture<ResultSet> queryAsync(String sql, Object... params) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i]);
                }

                return stmt.executeQuery();

            } catch (SQLException e) {
                plugin.getLogger().severe("Query failed: " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Execute an update asynchronously
     */
    public CompletableFuture<Integer> updateAsync(String sql, Object... params) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i]);
                }

                return stmt.executeUpdate();

            } catch (SQLException e) {
                plugin.getLogger().severe("Update failed: " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Disconnect from database
     */
    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("Database disconnected");
        }
    }

    /**
     * Get database type
     */
    public String getDatabaseType() {
        return databaseType;
    }

    /**
     * Check if connected
     */
    public boolean isConnected() {
        return dataSource != null && !dataSource.isClosed();
    }
}
