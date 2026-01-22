package dev.cobalt.library.database.builders;

import dev.cobalt.library.database.DatabaseManager;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class UpdateBuilder {

    private final DatabaseManager db;
    private final String table;
    private final Map<String, Object> sets = new LinkedHashMap<>();
    private final List<String> whereClauses = new ArrayList<>();
    private final List<Object> whereParams = new ArrayList<>();
    private Integer limit;

    public UpdateBuilder(DatabaseManager db, String table) {
        this.db = db;
        this.table = table;
    }

    public UpdateBuilder set(String column, Object value) {
        sets.put(column, value);
        return this;
    }

    public UpdateBuilder set(Map<String, Object> values) {
        sets.putAll(values);
        return this;
    }

    public UpdateBuilder increment(String column, int amount) {
        sets.put(column, "EXPRESSION:" + column + " + " + amount);
        return this;
    }

    public UpdateBuilder decrement(String column, int amount) {
        sets.put(column, "EXPRESSION:" + column + " - " + amount);
        return this;
    }

    public UpdateBuilder where(String condition, Object... params) {
        whereClauses.add(condition);
        whereParams.addAll(Arrays.asList(params));

        return this;
    }

    public UpdateBuilder and(String condition, Object... params) {
        return where(condition, params);
    }

    public UpdateBuilder limit(int limit) {
        this.limit = limit;
        return this;
    }

    public String build() {
        StringBuilder sql = new StringBuilder();

        sql.append("UPDATE ").append(table);
        sql.append(" SET ");

        List<String> setParts = new ArrayList<>();

        for (Map.Entry<String, Object> entry : sets.entrySet()) {
            String value = entry.getValue().toString();

            if (value.startsWith("EXPRESSION:")) {
                setParts.add(entry.getKey() + " = " + value.substring(11));
            } else {
                setParts.add(entry.getKey() + " = ?");
            }
        }

        sql.append(String.join(", ", setParts));

        if (!whereClauses.isEmpty()) sql.append(" WHERE ").append(String.join(" AND ", whereClauses));
        if (limit != null) sql.append(" LIMIT ").append(limit);

        return sql.toString();
    }

    public CompletableFuture<Integer> executeAsync() {
        String sql = build();

        List<Object> allParams = new ArrayList<>();

        for (Object value : sets.values()) {

            if (value != null && !value.toString().startsWith("EXPRESSION:")) allParams.add(value);
        }

        allParams.addAll(whereParams);

        return db.updateAsync(sql, allParams.toArray());
    }

    public Object[] getParams() {
        List<Object> allParams = new ArrayList<>();

        for (Object value : sets.values()) {

            if (value != null && !value.toString().startsWith("EXPRESSION:")) allParams.add(value);
        }

        allParams.addAll(whereParams);
        return allParams.toArray();
    }

    @Override
    public String toString() {
        return build();
    }
}
