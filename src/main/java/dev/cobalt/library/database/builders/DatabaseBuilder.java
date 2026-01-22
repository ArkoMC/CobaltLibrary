package dev.cobalt.library.database.builders;

import dev.cobalt.library.database.DatabaseManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class DatabaseBuilder {

    private final DatabaseManager db;
    private final String databaseName;

    public DatabaseBuilder(DatabaseManager db, String databaseName) {
        this.db = db;
        this.databaseName = databaseName;
    }

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

    public CompletableFuture<Integer> getTableCount() {
        return listTables().thenApply(List::size);
    }

    public CompletableFuture<DatabaseStats> getStats() {
        CompletableFuture<Long> size = getSize();
        CompletableFuture<List<String>> tables = listTables();

        return size.thenCombine(tables, (sizeBytes, tableList) ->
                new DatabaseStats(databaseName, sizeBytes, tableList.size(), tableList)
        );
    }

    public CompletableFuture<Boolean> backup(String outputPath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String command = String.format(
                        "mysqldump -u%s -p%s %s > %s",
                        db.getConfig().getString("database.username", "null"),
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

    public TableBuilder createTable(String tableName) {
        return new TableBuilder(db, tableName);
    }

    public CompletableFuture<Integer> dropTable(String tableName) {
        String sql = "DROP TABLE IF EXISTS " + tableName;
        return db.updateAsync(sql);
    }

    public CompletableFuture<Integer> use() {
        String sql = "USE " + databaseName;
        return db.updateAsync(sql);
    }

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
            return sizeBytes / 1024 / 1024 / 1024;
        }

        public int getTableCount() {
            return tableCount;
        }

        public List<String> getTables() {
            return tables;
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
