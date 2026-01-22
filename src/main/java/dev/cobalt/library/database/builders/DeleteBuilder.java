package dev.cobalt.library.database.builders;

import dev.cobalt.library.database.DatabaseManager;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class DeleteBuilder {

    private final DatabaseManager db;
    private final String table;
    private final List<String> whereClauses = new ArrayList<>();
    private final List<Object> whereParams = new ArrayList<>();
    private Integer limit;

    public DeleteBuilder(DatabaseManager db, String table) {
        this.db = db;
        this.table = table;
    }

    public DeleteBuilder where(String condition, Object... params) {
        whereClauses.add(condition);
        whereParams.addAll(Arrays.asList(params));

        return this;
    }

    public DeleteBuilder and(String condition, Object... params) {
        return where(condition, params);
    }

    public DeleteBuilder limit(int limit) {
        this.limit = limit;
        return this;
    }

    public String build() {
        StringBuilder sql = new StringBuilder();

        sql.append("DELETE FROM ").append(table);

        if (!whereClauses.isEmpty()) sql.append(" WHERE ").append(String.join(" AND ", whereClauses));
        if (limit != null) sql.append(" LIMIT ").append(limit);

        return sql.toString();
    }

    public CompletableFuture<Integer> executeAsync() {
        String sql = build();
        return db.updateAsync(sql, whereParams.toArray());
    }

    public Object[] getParams() {
        return whereParams.toArray();
    }

    @Override
    public String toString() {
        return build();
    }
}
