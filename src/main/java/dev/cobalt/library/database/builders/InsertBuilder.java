package dev.cobalt.library.database.builders;

import dev.cobalt.library.database.DatabaseManager;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class InsertBuilder {

    private final DatabaseManager db;
    private final String table;
    private final Map<String, Object> values = new LinkedHashMap<>();
    private boolean ignore = false;
    private String onDuplicateKey;

    public InsertBuilder(DatabaseManager db, String table) {
        this.db = db;
        this.table = table;
    }

    public InsertBuilder value(String column, Object value) {
        values.put(column, value);
        return this;
    }

    public InsertBuilder values(Map<String, Object> values) {
        this.values.putAll(values);
        return this;
    }

    public InsertBuilder ignore() {
        this.ignore = true;
        return this;
    }

    public InsertBuilder onDuplicateKeyUpdate(String updateClause) {
        this.onDuplicateKey = updateClause;
        return this;
    }

    public String build() {
        StringBuilder sql = new StringBuilder();

        sql.append("INSERT ");

        if (ignore) sql.append("IGNORE ");

        sql.append("INTO ").append(table);

        sql.append(" (");
        sql.append(String.join(", ", values.keySet().stream()
                .map(k -> "?")
                .toArray(String[]::new)));
        sql.append(")");

        if (onDuplicateKey != null) sql.append(" ON DUPLICATE KEY UPDATE ").append(onDuplicateKey);

        return sql.toString();
    }

    public CompletableFuture<Integer> executeAsync() {
        String sql = build();
        return db.updateAsync(sql, values.values().toArray());
    }

    public CompletableFuture<Long> executeAndGetIdAsync() {
        String sql = build();
        return db.insertAsync(sql, values.values().toArray());
    }

    public Object[] getParams() {
        return values.values().toArray();
    }

    @Override
    public String toString() {
        return build();
    }
}
