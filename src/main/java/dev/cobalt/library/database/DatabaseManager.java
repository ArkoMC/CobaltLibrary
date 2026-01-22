package dev.cobalt.library.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.cobalt.library.config.ConfigurationManager;
import dev.cobalt.library.database.builders.*;
import dev.cobalt.library.database.builders.ConnectionBuilder;
import org.bukkit.plugin.Plugin;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Smart Database Manager with fluent builder API
 *
 * Features:
 * - Fluent query builders
 * - Table and schema builders
 * - Transaction support
 * - Batch operations
 * - Migration system
 * - Query statistics
 * - Connection health monitoring
 *
 * Usage:
 * DatabaseManager db = ConnectionBuilder.create(plugin)
 *   .mysql()
 *   .host("localhost")
 *   .database("minecraft_network")
 *   .username("root")
 *   .password("password")
 *   .connect()
 *   .join();
 */
public class DatabaseManager {

    private final Plugin plugin;
    private final ConfigurationManager config;
    private final HikariDataSource dataSource;
    private final ConnectionBuilder.DatabaseType databaseType;
    private final boolean enableStats;

    // Query statistics
    private final Map<String, QueryStats> queryStats = new ConcurrentHashMap<>();

    // Migration tracking
    private final Set<String> appliedMigrations = new HashSet<>();

    // Package-private constructor (only ConnectionBuilder can create)
    public DatabaseManager(Plugin plugin, HikariDataSource dataSource, ConnectionBuilder.DatabaseType type, boolean enableStats) {
        this.plugin = plugin;
        this.dataSource = dataSource;
        this.databaseType = type;
        this.enableStats = enableStats;
        this.config = new ConfigurationManager(plugin);

        // Initialize migrations table
        initializeMigrationsTable();
    }

    // ==================== CONNECTION MANAGEMENT ====================

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
     * Check connection health
     */
    public CompletableFuture<Boolean> healthCheck() {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection()) {
                return conn.isValid(5);
            } catch (SQLException e) {
                return false;
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
     * Check if connected
     */
    public boolean isConnected() {
        return dataSource != null && !dataSource.isClosed();
    }

    // ==================== QUERY EXECUTION ====================

    /**
     * Execute SELECT query asynchronously
     */
    public <T> CompletableFuture<List<T>> queryAsync(String sql, RowMapper<T> mapper, Object... params) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            List<T> results = new ArrayList<>();

            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                setParameters(stmt, params);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        results.add(mapper.map(rs));
                    }
                }

                recordQueryStats(sql, System.currentTimeMillis() - startTime, true);
                return results;

            } catch (SQLException e) {
                recordQueryStats(sql, System.currentTimeMillis() - startTime, false);
                throw new DatabaseException("Query failed: " + sql, e);
            }
        });
    }

    /**
     * Execute SELECT query and return single result
     */
    public <T> CompletableFuture<Optional<T>> queryOneAsync(String sql, RowMapper<T> mapper, Object... params) {
        return queryAsync(sql, mapper, params).thenApply(list ->
                list.isEmpty() ? Optional.empty() : Optional.of(list.get(0))
        );
    }

    /**
     * Execute UPDATE/INSERT/DELETE asynchronously
     */
    public CompletableFuture<Integer> updateAsync(String sql, Object... params) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();

            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                setParameters(stmt, params);
                int result = stmt.executeUpdate();

                recordQueryStats(sql, System.currentTimeMillis() - startTime, true);
                return result;

            } catch (SQLException e) {
                recordQueryStats(sql, System.currentTimeMillis() - startTime, false);
                throw new DatabaseException("Update failed: " + sql, e);
            }
        });
    }

    /**
     * Execute INSERT and return generated ID
     */
    public CompletableFuture<Long> insertAsync(String sql, Object... params) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();

            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                setParameters(stmt, params);
                stmt.executeUpdate();

                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    long id = rs.next() ? rs.getLong(1) : 0L;
                    recordQueryStats(sql, System.currentTimeMillis() - startTime, true);
                    return id;
                }

            } catch (SQLException e) {
                recordQueryStats(sql, System.currentTimeMillis() - startTime, false);
                throw new DatabaseException("Insert failed: " + sql, e);
            }
        });
    }

    /**
     * Execute batch updates
     */
    public CompletableFuture<int[]> batchUpdateAsync(String sql, List<Object[]> paramsList) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();

            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                for (Object[] params : paramsList) {
                    setParameters(stmt, params);
                    stmt.addBatch();
                }

                int[] results = stmt.executeBatch();
                recordQueryStats(sql, System.currentTimeMillis() - startTime, true);
                return results;

            } catch (SQLException e) {
                recordQueryStats(sql, System.currentTimeMillis() - startTime, false);
                throw new DatabaseException("Batch update failed: " + sql, e);
            }
        });
    }

    /**
     * Execute with retry logic
     */
    public <T> CompletableFuture<T> executeWithRetry(Function<Connection, T> function, int maxRetries) {
        return CompletableFuture.supplyAsync(() -> {
            int attempts = 0;
            Exception lastException = null;

            while (attempts < maxRetries) {
                try (Connection conn = getConnection()) {
                    return function.apply(conn);
                } catch (SQLException e) {
                    lastException = e;
                    attempts++;

                    if (attempts < maxRetries) {
                        try {
                            Thread.sleep(1000L * attempts); // Exponential backoff
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }

            throw new DatabaseException("Query failed after " + maxRetries + " attempts", lastException);
        });
    }

    // ==================== TRANSACTIONS ====================

    /**
     * Execute operations in a transaction
     */
    public <T> CompletableFuture<T> transaction(Function<Connection, T> operations) {
        return CompletableFuture.supplyAsync(() -> {
            Connection conn = null;
            try {
                conn = getConnection();
                conn.setAutoCommit(false);

                T result = operations.apply(conn);

                conn.commit();
                return result;

            } catch (Exception e) {
                if (conn != null) {
                    try {
                        conn.rollback();
                    } catch (SQLException rollbackEx) {
                        throw new DatabaseException("Rollback failed", rollbackEx);
                    }
                }
                throw new DatabaseException("Transaction failed", e);
            } finally {
                if (conn != null) {
                    try {
                        conn.setAutoCommit(true);
                        conn.close();
                    } catch (SQLException closeEx) {
                        plugin.getLogger().warning("Failed to close connection: " + closeEx.getMessage());
                    }
                }
            }
        });
    }

    /**
     * Execute void operations in a transaction
     */
    public CompletableFuture<Void> transactionVoid(Consumer<Connection> operations) {
        return transaction(conn -> {
            operations.accept(conn);
            return null;
        });
    }

    public ConfigurationManager getConfig() {
        return config;
    }

    public enum QueryType {
        SELECT,
        INSERT,
        UPDATE,
        DELETE;
    }

    // ==================== QUERY BUILDERS ====================

    /**
     * Create SELECT query builder
     */
    public QueryBuilder select(String... columns) {
        return new QueryBuilder(this, QueryType.SELECT, columns);
    }

    /**
     * Create INSERT query builder
     */
    public InsertBuilder insert(String table) {
        return new InsertBuilder(this, table);
    }

    /**
     * Create UPDATE query builder
     */
    public UpdateBuilder update(String table) {
        return new UpdateBuilder(this, table);
    }

    /**
     * Create DELETE query builder
     */
    public DeleteBuilder delete(String table) {
        return new DeleteBuilder(this, table);
    }

    // ==================== TABLE MANAGEMENT ====================

    /**
     * Create table builder
     */
    public TableBuilder createTable(String tableName) {
        return new TableBuilder(this, tableName);
    }

    /**
     * Drop table
     */
    public CompletableFuture<Integer> dropTable(String tableName) {
        return updateAsync("DROP TABLE IF EXISTS " + tableName);
    }

    /**
     * Check if table exists
     */
    public CompletableFuture<Boolean> tableExists(String tableName) {
        String sql = switch (getDatabaseType()) {
            case "mysql" -> "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?";
            case "postgresql" -> "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = ?";
            case "sqlite" -> "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name=?";
            default -> "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = ?";
        };

        return queryOneAsync(sql, rs -> rs.getInt(1), tableName)
                .thenApply(opt -> opt.orElse(0) > 0);
    }

    /**
     * Truncate table
     */
    public CompletableFuture<Integer> truncateTable(String tableName) {
        return updateAsync("TRUNCATE TABLE " + tableName);
    }

    /**
     * Rename table
     */
    public CompletableFuture<Integer> renameTable(String oldName, String newName) {
        String sql = getDatabaseType().equals("mysql")
                ? "RENAME TABLE " + oldName + " TO " + newName
                : "ALTER TABLE " + oldName + " RENAME TO " + newName;

        return updateAsync(sql);
    }

    // ==================== DATABASE MANAGEMENT ====================

    /**
     * Get database operations builder
     */
    public DatabaseOperations database(String databaseName) {
        return new DatabaseOperations(this, databaseName);
    }

    /**
     * Create schema builder
     */
    public SchemaBuilder schema(String databaseName) {
        return new SchemaBuilder(this, databaseName);
    }

    /**
     * List all databases
     */
    public CompletableFuture<List<String>> listDatabases() {
        String sql = switch (getDatabaseType()) {
            case "mysql" -> "SHOW DATABASES";
            case "postgresql" -> "SELECT datname FROM pg_database WHERE datistemplate = false";
            default -> null;
        };

        if (sql == null) {
            return CompletableFuture.completedFuture(new ArrayList<>());
        }

        return queryAsync(sql, rs -> rs.getString(1));
    }

    /**
     * Check if database exists
     */
    public CompletableFuture<Boolean> databaseExists(String databaseName) {
        return listDatabases().thenApply(databases -> databases.contains(databaseName));
    }

    /**
     * Get current database
     */
    public CompletableFuture<String> getCurrentDatabase() {
        String sql = switch (getDatabaseType()) {
            case "mysql" -> "SELECT DATABASE()";
            case "postgresql" -> "SELECT current_database()";
            case "sqlite" -> null;
            default -> "SELECT DATABASE()";
        };

        if (sql == null) {
            return CompletableFuture.completedFuture("main");
        }

        return queryOneAsync(sql, rs -> rs.getString(1))
                .thenApply(opt -> opt.orElse(""));
    }

    // ==================== MIGRATIONS ====================

    /**
     * Initialize migrations tracking table
     */
    private void initializeMigrationsTable() {
        String sql = getDatabaseType().equals("sqlite")
                ? "CREATE TABLE IF NOT EXISTS _migrations (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL UNIQUE, " +
                "applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)"
                : "CREATE TABLE IF NOT EXISTS _migrations (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "name VARCHAR(255) NOT NULL UNIQUE, " +
                "applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            loadAppliedMigrations();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to initialize migrations: " + e.getMessage());
        }
    }

    /**
     * Load applied migrations
     */
    private void loadAppliedMigrations() throws SQLException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM _migrations")) {
            while (rs.next()) {
                appliedMigrations.add(rs.getString("name"));
            }
        }
    }

    /**
     * Apply migration
     */
    public CompletableFuture<Boolean> applyMigration(String name, String sql) {
        return CompletableFuture.supplyAsync(() -> {
            if (appliedMigrations.contains(name)) {
                plugin.getLogger().info("Migration '" + name + "' already applied");
                return false;
            }

            return transaction(conn -> {
                try {
                    // Execute migration
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute(sql);
                    }

                    // Record migration
                    try (PreparedStatement stmt = conn.prepareStatement(
                            "INSERT INTO _migrations (name) VALUES (?)")) {
                        stmt.setString(1, name);
                        stmt.executeUpdate();
                    }

                    appliedMigrations.add(name);
                    plugin.getLogger().info("Applied migration: " + name);
                    return true;

                } catch (SQLException e) {
                    throw new DatabaseException("Migration failed: " + name, e);
                }
            }).join();
        });
    }

    /**
     * Apply multiple migrations
     */
    public CompletableFuture<Integer> applyMigrations(Map<String, String> migrations) {
        return CompletableFuture.supplyAsync(() -> {
            int applied = 0;
            List<String> sortedNames = new ArrayList<>(migrations.keySet());
            Collections.sort(sortedNames);

            for (String name : sortedNames) {
                if (applyMigration(name, migrations.get(name)).join()) {
                    applied++;
                }
            }

            return applied;
        });
    }

    // ==================== UTILITIES ====================

    /**
     * Set prepared statement parameters
     */
    private void setParameters(PreparedStatement stmt, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            Object param = params[i];

            if (param == null) {
                stmt.setNull(i + 1, Types.NULL);
            } else if (param instanceof UUID) {
                stmt.setString(i + 1, param.toString());
            } else if (param instanceof Enum) {
                stmt.setString(i + 1, ((Enum<?>) param).name());
            } else {
                stmt.setObject(i + 1, param);
            }
        }
    }

    /**
     * Record query statistics
     */
    private void recordQueryStats(String sql, long executionTime, boolean success) {
        if (!enableStats) return;

        String queryKey = sql.substring(0, Math.min(50, sql.length()));
        QueryStats stats = queryStats.computeIfAbsent(queryKey, k -> new QueryStats());
        stats.record(executionTime, success);
    }

    /**
     * Get query statistics
     */
    public Map<String, QueryStats> getQueryStats() {
        return new HashMap<>(queryStats);
    }

    /**
     * Reset query statistics
     */
    public void resetQueryStats() {
        queryStats.clear();
    }

    /**
     * Get pool statistics
     */
    public PoolStats getPoolStats() {
        if (dataSource == null) {
            return new PoolStats(0, 0, 0, 0);
        }

        return new PoolStats(
                dataSource.getHikariPoolMXBean().getTotalConnections(),
                dataSource.getHikariPoolMXBean().getActiveConnections(),
                dataSource.getHikariPoolMXBean().getIdleConnections(),
                dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection()
        );
    }

    /**
     * Get database type
     */
    public String getDatabaseType() {
        return databaseType.name().toLowerCase();
    }

    /**
     * Get plugin
     */
    public Plugin getPlugin() {
        return plugin;
    }

    // ==================== INNER CLASSES ====================

    /**
     * Row mapper functional interface
     */
    @FunctionalInterface
    public interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    /**
     * Query statistics
     */
    public static class QueryStats {
        private long totalExecutions = 0;
        private long totalTime = 0;
        private long successCount = 0;
        private long failureCount = 0;
        private long minTime = Long.MAX_VALUE;
        private long maxTime = 0;

        public synchronized void record(long executionTime, boolean success) {
            totalExecutions++;
            totalTime += executionTime;

            if (success) successCount++;
            else failureCount++;

            minTime = Math.min(minTime, executionTime);
            maxTime = Math.max(maxTime, executionTime);
        }

        public long getTotalExecutions() { return totalExecutions; }
        public long getAverageTime() { return totalExecutions > 0 ? totalTime / totalExecutions : 0; }
        public long getMinTime() { return minTime == Long.MAX_VALUE ? 0 : minTime; }
        public long getMaxTime() { return maxTime; }
        public long getSuccessCount() { return successCount; }
        public long getFailureCount() { return failureCount; }
        public double getSuccessRate() { return totalExecutions > 0 ? (double) successCount / totalExecutions : 0; }
    }

    /**
     * Pool statistics
     */
    public record PoolStats(
            int totalConnections,
            int activeConnections,
            int idleConnections,
            int threadsAwaitingConnection
    ) {}

    /**
     * Database exception
     */
    public static class DatabaseException extends RuntimeException {
        public DatabaseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}