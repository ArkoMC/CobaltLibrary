package dev.cobalt.library.database;

import dev.cobalt.library.database.builders.TableBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Database Operations for managing existing databases
 *
 * Usage:
 * db.database("minecraft_network")
 *   .listTables()
 *   .thenAccept(tables -> ...);
 *
 * db.database("minecraft_network")
 *   .getStats()
 *   .thenAccept(stats -> ...);
 */
public class DatabaseOperations {

    private final DatabaseManager db;
    private final String databaseName;

    public DatabaseOperations(DatabaseManager db, String databaseName) {
        this.db = db;
        this.databaseName = databaseName;
    }

    // ==================== DATABASE OPERATIONS ====================

    /**
     * Use/select this database
     */
    public CompletableFuture<Integer> use() {
        String sql = "USE " + databaseName;
        return db.updateAsync(sql);
    }

    /**
     * List all tables in database
     */
    public CompletableFuture<List<String>> listTables() {
        String sql;

        switch (db.getDatabaseType()) {
            case "mysql":
                sql = "SELECT table_name FROM information_schema.tables " +
                        "WHERE table_schema = '" + databaseName + "' " +
                        "ORDER BY table_name";
                break;
            case "postgresql":
                sql = "SELECT tablename FROM pg_tables " +
                        "WHERE schemaname = 'public' " +
                        "ORDER BY tablename";
                break;
            case "sqlite":
                sql = "SELECT name FROM sqlite_master " +
                        "WHERE type='table' " +
                        "ORDER BY name";
                break;
            default:
                sql = "SELECT table_name FROM information_schema.tables " +
                        "WHERE table_schema = '" + databaseName + "' " +
                        "ORDER BY table_name";
        }

        return db.queryAsync(sql, rs -> rs.getString(1));
    }

    /**
     * Get database size in bytes
     */
    public CompletableFuture<Long> getSize() {
        String sql;

        switch (db.getDatabaseType()) {
            case "mysql":
                sql = "SELECT SUM(data_length + index_length) " +
                        "FROM information_schema.tables " +
                        "WHERE table_schema = '" + databaseName + "'";
                break;
            case "postgresql":
                sql = "SELECT pg_database_size('" + databaseName + "')";
                break;
            default:
                return CompletableFuture.completedFuture(0L);
        }

        return db.queryOneAsync(sql, rs -> rs.getLong(1))
                .thenApply(opt -> opt.orElse(0L));
    }

    /**
     * Get table count
     */
    public CompletableFuture<Integer> getTableCount() {
        return listTables().thenApply(List::size);
    }

    /**
     * Get database statistics
     */
    public CompletableFuture<DatabaseStats> getStats() {
        CompletableFuture<Long> size = getSize();
        CompletableFuture<List<String>> tables = listTables();

        return size.thenCombine(tables, (sizeBytes, tableList) ->
                new DatabaseStats(databaseName, sizeBytes, tableList.size(), tableList)
        );
    }

    /**
     * Backup database to SQL file (MySQL only)
     */
    public CompletableFuture<Boolean> backup(String outputPath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String command = String.format(
                        "mysqldump -u%s -p%s %s > %s",
                        db.getConfig().getString("database.username", "root"),
                        db.getConfig().getString("database.password", "null"),
                        databaseName,
                        outputPath
                );

                Process process = Runtime.getRuntime().exec(command);
                int exitCode = process.waitFor();
                return exitCode == 0;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    /**
     * Optimize all tables in database (MySQL only)
     */
    public CompletableFuture<Integer> optimize() {
        return listTables().thenCompose(tables -> {
            List<CompletableFuture<Integer>> futures = new ArrayList<>();

            for (String table : tables) {
                String sql = "OPTIMIZE TABLE " + table;
                futures.add(db.updateAsync(sql));
            }

            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> tables.size());
        });
    }

    /**
     * Analyze all tables in database (MySQL only)
     */
    public CompletableFuture<Integer> analyze() {
        return listTables().thenCompose(tables -> {
            List<CompletableFuture<Integer>> futures = new ArrayList<>();

            for (String table : tables) {
                String sql = "ANALYZE TABLE " + table;
                futures.add(db.updateAsync(sql));
            }

            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> tables.size());
        });
    }

    /**
     * Check all tables in database (MySQL only)
     */
    public CompletableFuture<Integer> check() {
        return listTables().thenCompose(tables -> {
            List<CompletableFuture<Integer>> futures = new ArrayList<>();

            for (String table : tables) {
                String sql = "CHECK TABLE " + table;
                futures.add(db.updateAsync(sql));
            }

            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> tables.size());
        });
    }

    /**
     * Repair all tables in database (MySQL only)
     */
    public CompletableFuture<Integer> repair() {
        return listTables().thenCompose(tables -> {
            List<CompletableFuture<Integer>> futures = new ArrayList<>();

            for (String table : tables) {
                String sql = "REPAIR TABLE " + table;
                futures.add(db.updateAsync(sql));
            }

            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> tables.size());
        });
    }

    // ==================== TABLE CREATION ====================

    /**
     * Create a table in this database
     */
    public TableBuilder createTable(String tableName) {
        return new TableBuilder(db, tableName);
    }

    /**
     * Drop a table from this database
     */
    public CompletableFuture<Integer> dropTable(String tableName) {
        String sql = "DROP TABLE IF EXISTS " + tableName;
        return db.updateAsync(sql);
    }

    // ==================== INNER CLASSES ====================

    /**
     * Database statistics
     */
    public static class DatabaseStats {
        private final String name;
        private final long sizeBytes;
        private final int tableCount;
        private final List<String> tables;

        public DatabaseStats(String name, long sizeBytes, int tableCount, List<String> tables) {
            this.name = name;
            this.sizeBytes = sizeBytes;
            this.tableCount = tableCount;
            this.tables = tables;
        }

        public String getName() {
            return name;
        }

        public long getSizeBytes() {
            return sizeBytes;
        }

        public double getSizeMB() {
            return sizeBytes / 1024.0 / 1024.0;
        }

        public double getSizeGB() {
            return sizeBytes / 1024.0 / 1024.0 / 1024.0;
        }

        public int getTableCount() {
            return tableCount;
        }

        public List<String> getTables() {
            return new ArrayList<>(tables);
        }

        @Override
        public String toString() {
            return String.format(
                    "Database: %s | Size: %.2f MB | Tables: %d",
                    name, getSizeMB(), tableCount
            );
        }
    }
}