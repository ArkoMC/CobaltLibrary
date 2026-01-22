package dev.cobalt.library.database.builders;

import dev.cobalt.library.database.DatabaseManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class QueryBuilder {

    private final DatabaseManager db;
    private final DatabaseManager.QueryType type;
    private final String[] columns;

    private String table;
    private final List<String> whereClauses = new ArrayList<>();
    private final List<Object> whereParams = new ArrayList<>();
    private final List<String> joins = new ArrayList<>();
    private String groupBy;
    private String having;
    private String orderBy;
    private Integer limit;
    private Integer offset;

    public QueryBuilder(DatabaseManager db, DatabaseManager.QueryType type, String... columns) {
        this.db = db;
        this.type = type;
        this.columns = columns;
    }

    public QueryBuilder from(String table) {
        this.table = table;
        return this;
    }

    public QueryBuilder where(String condition, Object... params) {
        whereClauses.add(condition);
        whereParams.addAll(Arrays.asList(params));

        return this;
    }

    public QueryBuilder and(String condition, Object... params) {
        return where(condition, params);
    }

    public QueryBuilder or(String condition, Object... params) {
        if (!whereClauses.isEmpty()) {
            int lastIndex = whereClauses.size() - 1;

            whereClauses.set(lastIndex, whereClauses.get(lastIndex) + " OR " + condition);

            whereParams.addAll(Arrays.asList(params));
        }

        return this;
    }

    public QueryBuilder join(String joinClause) {
        joins.add("JOIN " + joinClause);
        return this;
    }

    public QueryBuilder leftJoin(String joinClause) {
        joins.add("LEFT JOIN " + joinClause);
        return this;
    }

    public QueryBuilder rightJoin(String joinClause) {
        joins.add("RIGHT JOIN " + joinClause);
        return this;
    }

    public QueryBuilder innerJoin(String joinClause) {
        joins.add("INNER JOIN " + joinClause);
        return this;
    }

    public QueryBuilder groupBy(String groupBy) {
        this.groupBy = groupBy;
        return this;
    }

    public QueryBuilder having(String having) {
        this.having = having;
        return this;
    }

    public QueryBuilder orderBy(String orderBy) {
        this.orderBy = orderBy;
        return this;
    }

    public QueryBuilder limit(int limit) {
        this.limit = limit;
        return this;
    }

    public QueryBuilder offset(int offset) {
        this.offset = offset;
        return this;
    }

    public String build() {
        StringBuilder sql = new StringBuilder();

        sql.append("SELECT ");

        if (columns.length == 0 || (columns.length == 1 && columns[0].equals("*"))) sql.append("*");
        else sql.append(String.join(", ", columns));

        sql.append(" FROM ").append(table);

        if (!whereClauses.isEmpty()) sql.append(" WHERE ").append(String.join(" AND ", whereClauses));
        if (groupBy != null) sql.append(" GROUP BY ").append(groupBy);
        if (having != null) sql.append(" HAVING ").append(having);
        if (orderBy != null) sql.append(" ORDER BY ").append(orderBy);
        if (limit != null) sql.append(" LIMIT ").append(limit);
        if (offset != null) sql.append(" OFFSET ").append(offset);

        return sql.toString();
    }

    public <T> CompletableFuture<List<T>> executeAsync(DatabaseManager.RowMapper<T> mapper) {
        String sql = build();
        return db.queryAsync(sql, mapper, whereParams.toArray());
    }

    public <T> CompletableFuture<Optional<T>> executeOneAsync(DatabaseManager.RowMapper<T> mapper) {
        String sql = build();
        return db.queryOneAsync(sql, mapper, whereParams.toArray());
    }

    public CompletableFuture<Long> count() {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM ").append(table);

        if (!whereClauses.isEmpty()) sql.append(" WHERE ").append(String.join(" AND ", whereClauses));

        return db.queryOneAsync(sql.toString(), rs -> rs.getLong(1), whereParams.toArray())
                .thenApply(opt -> opt.orElse(0L));
    }

    public CompletableFuture<Boolean> exists() {
        return count().thenApply(c -> c > 0);
    }

    public Object[] getParams() {
        return whereParams.toArray();
    }

    @Override
    public String toString() {
        return build();
    }
}
