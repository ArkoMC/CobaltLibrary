package dev.cobalt.library.database.builders;

import dev.cobalt.library.database.DatabaseManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class SchemaBuilder {

    private final DatabaseManager db;
    private final String databaseName;
    private final List<TableDefinition> tables = new ArrayList<>();
    private final List<String> migrations = new ArrayList<>();

    public SchemaBuilder(DatabaseManager db, String databaseName) {
        this.db = db;
        this.databaseName = databaseName;
    }

    public SchemaBuilder table(String tableName, Consumer<TableBuilder> definition) {
        tables.add(new TableDefinition(tableName, definition));
        return this;
    }

    public SchemaBuilder migration(String sql) {
        migrations.add(sql);
        return this;
    }

// EXECUTE

    public CompletableFuture<SchemaResult> execute() {
        long startTime = System.currentTimeMillis();
        SchemaResult result = new SchemaResult();

        CompletableFuture<Void> future = CompletableFuture.completedFuture(null);

        for (TableDefinition tableDef : tables) {
            future = future.thenCompose(v -> {
               TableBuilder builder = db.createTable(tableDef.name);
               tableDef.definition.accept(builder);

               return builder.execute()
                       .thenAccept(rows -> {
                           result.tablesCreated++;
                           result.createdTables.add(tableDef.name);
                       })
                       .exceptionally(ex -> {
                           result.errors.add("Table '" + tableDef.name + "' failed: " + ex.getMessage());
                           return null;
                       });
            });
        }

        for (String migration : migrations) {
            future = future.thenCompose(v ->
                    db.updateAsync(migration)
                            .thenAccept(rows -> result.migrationsExecuted++)
                            .exceptionally(ex -> {
                                result.errors.add("Migration failed: " + ex.getMessage());
                                return null; // REQUIRED
                            })
            );
        }

        return future.thenApply(v -> {
            result.executionTimeMs = System.currentTimeMillis() - startTime;
            return result;
        });
    }

    public CompletableFuture<SchemaResult> executeAndPrint() {
        return execute().thenApply(result -> {
            System.out.println("\n=== Schema Execution Results ===");
            System.out.println("Tables created: " + result.tablesCreated);
            System.out.println("Migrations executed: " + result.migrationsExecuted);
            System.out.println("Execution time: " + result.executionTimeMs + "ms");

            if (!result.createdTables.isEmpty()) {
                System.out.println("\nCreated tables:");
                result.createdTables.forEach(t -> System.out.println("  - " + t));
            }

            if (!result.errors.isEmpty()) {
                System.out.println("\nErrors:");
                result.errors.forEach(e -> System.out.println("  ✗ " + e));
            }

            System.out.println("================================\n");
            return result;
        });
    }

    public String generateScript() {
        StringBuilder script = new StringBuilder();

        // Use database
        if (databaseName != null) {
            script.append("-- Using Database: ").append(databaseName).append("\n");
            script.append("USE ").append(databaseName).append(";\n\n");
        }

        // Table creation
        for (TableDefinition tableDef : tables) {
            script.append("-- Table: ").append(tableDef.name).append("\n");
            TableBuilder builder = db.createTable(tableDef.name);
            tableDef.definition.accept(builder);
            script.append(builder.build());
            script.append(";\n\n");
        }

        // Migrations
        if (!migrations.isEmpty()) {
            script.append("-- Migrations\n");
            for (String migration : migrations) {
                script.append(migration).append(";\n");
            }
            script.append("\n");
        }

        return script.toString();
    }

    public CompletableFuture<Boolean> saveToFile(String filepath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String script = generateScript();
                java.nio.file.Files.write(
                        java.nio.file.Paths.get(filepath),
                        script.getBytes()
                );
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        });
    }

// INNER CLASSES

    private static class TableDefinition {
        final String name;
        final Consumer<TableBuilder> definition;

        TableDefinition(String name, Consumer<TableBuilder> definition) {
            this.name = name;
            this.definition = definition;
        }
    }

    public static class SchemaResult {
        int tablesCreated = 0;
        int migrationsExecuted = 0;
        long executionTimeMs = 0;
        final List<String> createdTables = new ArrayList<>();
        final List<String> errors = new ArrayList<>();

        public boolean isSuccess() {
            return errors.isEmpty();
        }

        public int getTablesCreated() {
            return tablesCreated;
        }

        public int getMigrationsExecuted() {
            return migrationsExecuted;
        }

        public long getExecutionTimeMs() {
            return executionTimeMs;
        }

        public List<String> getCreatedTables() {
            return createdTables;
        }

        public List<String> getErrors() {
            return errors;
        }
    }
}
