package dev.cobalt.library.database.builders;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.cobalt.library.database.DatabaseManager;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Fluent Connection Builder for establishing database connections
 *
 * Usage:
 * DatabaseManager db = ConnectionBuilder.create(plugin)
 *   .mysql()
 *   .host("localhost")
 *   .port(3306)
 *   .database("minecraft_network")
 *   .username("root")
 *   .password("password")
 *   .poolSize(10)
 *   .connect();
 *
 * Or from config:
 * DatabaseManager db = ConnectionBuilder.create(plugin)
 *   .fromConfig(config, "database")
 *   .connect();
 */
public class ConnectionBuilder {

    private final Plugin plugin;
    private DatabaseType type = DatabaseType.MYSQL;

    // Connection properties
    private String host = "localhost";
    private int port = 3306;
    private String database = "minecraft";
    private String username = "root";
    private String password = "";
    private String file = "database.db";
    private Map<String, String> properties = new HashMap<>();

    // Pool settings
    private int poolSize = 10;
    private int minIdle = 2;
    private int connectionTimeout = 30000;
    private int idleTimeout = 600000;
    private int maxLifetime = 1800000;
    private int leakDetection = 60000;

    // Features
    private boolean enableStats = true;
    private boolean enableLeakDetection = true;
    private boolean enableOptimizations = true;

    private ConnectionBuilder(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Create a new connection builder
     */
    public static ConnectionBuilder create(Plugin plugin) {
        return new ConnectionBuilder(plugin);
    }

    // ==================== DATABASE TYPE ====================

    /**
     * Use MySQL database
     */
    public ConnectionBuilder mysql() {
        this.type = DatabaseType.MYSQL;
        this.port = 3306;
        return this;
    }

    /**
     * Use PostgreSQL database
     */
    public ConnectionBuilder postgresql() {
        this.type = DatabaseType.POSTGRESQL;
        this.port = 5432;
        return this;
    }

    /**
     * Use SQLite database
     */
    public ConnectionBuilder sqlite() {
        this.type = DatabaseType.SQLITE;
        return this;
    }

    /**
     * Use H2 database
     */
    public ConnectionBuilder h2() {
        this.type = DatabaseType.H2;
        return this;
    }

    // ==================== CONNECTION PROPERTIES ====================

    /**
     * Set database host
     */
    public ConnectionBuilder host(String host) {
        this.host = host;
        return this;
    }

    /**
     * Set database port
     */
    public ConnectionBuilder port(int port) {
        this.port = port;
        return this;
    }

    /**
     * Set database name
     */
    public ConnectionBuilder database(String database) {
        this.database = database;
        return this;
    }

    /**
     * Set username
     */
    public ConnectionBuilder username(String username) {
        this.username = username;
        return this;
    }

    /**
     * Set password
     */
    public ConnectionBuilder password(String password) {
        this.password = password;
        return this;
    }

    /**
     * Set file path for SQLite/H2
     */
    public ConnectionBuilder file(String file) {
        this.file = file;
        return this;
    }

    /**
     * Add custom connection property
     */
    public ConnectionBuilder property(String key, String value) {
        this.properties.put(key, value);
        return this;
    }

    // ==================== POOL SETTINGS ====================

    /**
     * Set connection pool size
     */
    public ConnectionBuilder poolSize(int size) {
        this.poolSize = size;
        return this;
    }

    /**
     * Set minimum idle connections
     */
    public ConnectionBuilder minIdle(int minIdle) {
        this.minIdle = minIdle;
        return this;
    }

    /**
     * Set connection timeout (ms)
     */
    public ConnectionBuilder timeout(int timeout) {
        this.connectionTimeout = timeout;
        return this;
    }

    /**
     * Set idle timeout (ms)
     */
    public ConnectionBuilder idleTimeout(int timeout) {
        this.idleTimeout = timeout;
        return this;
    }

    /**
     * Set max connection lifetime (ms)
     */
    public ConnectionBuilder maxLifetime(int lifetime) {
        this.maxLifetime = lifetime;
        return this;
    }

    /**
     * Set leak detection threshold (ms)
     */
    public ConnectionBuilder leakDetection(int threshold) {
        this.leakDetection = threshold;
        return this;
    }

    // ==================== FEATURES ====================

    /**
     * Enable/disable query statistics
     */
    public ConnectionBuilder statistics(boolean enable) {
        this.enableStats = enable;
        return this;
    }

    /**
     * Enable/disable connection leak detection
     */
    public ConnectionBuilder leakDetection(boolean enable) {
        this.enableLeakDetection = enable;
        return this;
    }

    /**
     * Enable/disable database optimizations
     */
    public ConnectionBuilder optimizations(boolean enable) {
        this.enableOptimizations = enable;
        return this;
    }

    // ==================== PRESETS ====================

    /**
     * Small server preset (1-50 players)
     */
    public ConnectionBuilder small() {
        return poolSize(5).minIdle(2).timeout(20000);
    }

    /**
     * Medium server preset (50-200 players)
     */
    public ConnectionBuilder medium() {
        return poolSize(10).minIdle(3).timeout(30000);
    }

    /**
     * Large server preset (200-500 players)
     */
    public ConnectionBuilder large() {
        return poolSize(20).minIdle(5).timeout(40000);
    }

    /**
     * Network preset (multi-server)
     */
    public ConnectionBuilder network() {
        return poolSize(30).minIdle(10).timeout(50000);
    }

    // ==================== CONFIG LOADING ====================

    /**
     * Load from config section
     */
    public ConnectionBuilder fromConfig(Map<String, Object> config) {
        // Database type
        String typeStr = (String) config.getOrDefault("type", "mysql");
        switch (typeStr.toLowerCase()) {
            case "postgresql": postgresql(); break;
            case "sqlite": sqlite(); break;
            case "h2": h2(); break;
            default: mysql(); break;
        }

        // Connection properties
        if (config.containsKey("host")) host((String) config.get("host"));
        if (config.containsKey("port")) port((Integer) config.get("port"));
        if (config.containsKey("database")) database((String) config.get("database"));
        if (config.containsKey("username")) username((String) config.get("username"));
        if (config.containsKey("password")) password((String) config.get("password"));
        if (config.containsKey("file")) file((String) config.get("file"));

        // Pool settings
        if (config.containsKey("pool-size")) poolSize((Integer) config.get("pool-size"));
        if (config.containsKey("min-idle")) minIdle((Integer) config.get("min-idle"));
        if (config.containsKey("timeout")) timeout((Integer) config.get("timeout"));
        if (config.containsKey("idle-timeout")) idleTimeout((Integer) config.get("idle-timeout"));
        if (config.containsKey("max-lifetime")) maxLifetime((Integer) config.get("max-lifetime"));
        if (config.containsKey("leak-detection")) leakDetection((Integer) config.get("leak-detection"));

        // Features
        if (config.containsKey("enable-stats")) statistics((Boolean) config.get("enable-stats"));

        return this;
    }

    // ==================== BUILD & CONNECT ====================

    /**
     * Build HikariConfig
     */
    private HikariConfig buildConfig() {
        HikariConfig config = new HikariConfig();

        // JDBC URL and credentials
        switch (type) {
            case MYSQL:
                config.setJdbcUrl(buildMySQLUrl());
                config.setUsername(username);
                config.setPassword(password);
                config.setDriverClassName("com.mysql.cj.jdbc.Driver");
                if (enableOptimizations) applyMySQLOptimizations(config);
                break;

            case POSTGRESQL:
                config.setJdbcUrl(buildPostgreSQLUrl());
                config.setUsername(username);
                config.setPassword(password);
                config.setDriverClassName("org.postgresql.Driver");
                if (enableOptimizations) applyPostgreSQLOptimizations(config);
                break;

            case SQLITE:
                config.setJdbcUrl(buildSQLiteUrl());
                config.setDriverClassName("org.sqlite.JDBC");
                config.setMaximumPoolSize(1); // SQLite only supports 1 connection
                if (enableOptimizations) applySQLiteOptimizations(config);
                break;

            case H2:
                config.setJdbcUrl(buildH2Url());
                config.setDriverClassName("org.h2.Driver");
                break;
        }

        // Pool settings
        if (type != DatabaseType.SQLITE) {
            config.setMaximumPoolSize(poolSize);
            config.setMinimumIdle(minIdle);
        }
        config.setConnectionTimeout(connectionTimeout);
        config.setIdleTimeout(idleTimeout);
        config.setMaxLifetime(maxLifetime);

        if (enableLeakDetection) {
            config.setLeakDetectionThreshold(leakDetection);
        }

        // Connection test
        config.setConnectionTestQuery("SELECT 1");

        // Custom properties
        properties.forEach(config::addDataSourceProperty);

        return config;
    }

    private String buildMySQLUrl() {
        StringBuilder url = new StringBuilder();
        url.append("jdbc:mysql://").append(host).append(":").append(port).append("/").append(database);
        url.append("?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        return url.toString();
    }

    private String buildPostgreSQLUrl() {
        return "jdbc:postgresql://" + host + ":" + port + "/" + database;
    }

    private String buildSQLiteUrl() {
        String path = plugin.getDataFolder().getAbsolutePath() + File.separator + file;
        return "jdbc:sqlite:" + path;
    }

    private String buildH2Url() {
        String path = plugin.getDataFolder().getAbsolutePath() + File.separator + file;
        return "jdbc:h2:" + path + ";MODE=MySQL";
    }

    private void applyMySQLOptimizations(HikariConfig config) {
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");
    }

    private void applyPostgreSQLOptimizations(HikariConfig config) {
        config.addDataSourceProperty("preparedStatementCacheSize", "250");
        config.addDataSourceProperty("preparedStatementCacheSizeMiB", "5");
    }

    private void applySQLiteOptimizations(HikariConfig config) {
        config.addDataSourceProperty("journal_mode", "WAL");
        config.addDataSourceProperty("synchronous", "NORMAL");
    }

    /**
     * Connect and return DatabaseManager
     */
    public CompletableFuture<DatabaseManager> connect() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HikariConfig config = buildConfig();
                HikariDataSource dataSource = new HikariDataSource(config);

                // Test connection
                try (Connection conn = dataSource.getConnection()) {
                    if (!conn.isValid(5)) {
                        throw new SQLException("Connection validation failed");
                    }
                }

                plugin.getLogger().info("Database connected: " + type.name().toLowerCase());

                return new DatabaseManager(plugin, dataSource, type, enableStats);

            } catch (Exception e) {
                plugin.getLogger().severe("Failed to connect to database: " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Connect synchronously
     */
    public DatabaseManager connectSync() {
        return connect().join();
    }

    // ==================== DATABASE TYPE ENUM ====================

    public static enum DatabaseType {
        MYSQL, POSTGRESQL, SQLITE, H2
    }
}
