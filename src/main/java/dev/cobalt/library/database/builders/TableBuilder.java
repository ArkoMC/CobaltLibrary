package dev.cobalt.library.database.builders;

import dev.cobalt.library.database.DatabaseManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class TableBuilder {

    private final DatabaseManager db;
    private final String tableName;
    private final List<Column> columns = new ArrayList<>();
    private final List<Index> indexes = new ArrayList<>();
    private final List<ForeignKey> foreignKeys = new ArrayList<>();
    private String primaryKey;
    private String engine = "InnoDB";
    private String charset = "utf8mb4";
    private String collation = "utf8mb4_unicode_ci";
    private boolean ifNotExists = true;
    private boolean log = false;

    public TableBuilder(DatabaseManager db, String tableName) {
        this.db = db;
        this.tableName = tableName;
    }

    public TableBuilder id() {
        return id("id");
    }

    public TableBuilder id(String name) {
        String type = db.getDatabaseType().equals("sqlite")
                ? "INTEGER PRIMARY KEY AUTOINCREMENT"
                : "BIGINT AUTO_INCREMENT PRIMARY KEY";

        columns.add(new Column(name, type));
        this.primaryKey = name;
        return this;
    }

    public TableBuilder timestamps() {
        timestamp("created_at").defaultCurrentTimestamp();
        timestamp("updated_at").defaultCurrentTimestamp().onUpdateCurrentTimestamp();
        return this;
    }

    public TableBuilder softDeletes() {
        timestamp("deleted_at").nullable();
        return this;
    }

// INDEXES

    public TableBuilder index(String indexName, String... columns) {
        indexes.add(new Index(indexName, false, false, columns));
        return this;
    }

    public TableBuilder unique(String indexName, String... columns) {
        indexes.add(new Index(indexName, true, false, columns));
        return this;
    }

    public TableBuilder fulltext(String indexName, String... columns) {
        indexes.add(new Index(indexName, false, true, columns));
        return this;
    }

// FOREIGN KEYS

    public TableBuilder foreignKey(String column, String referencedTable, String referencedColumn) {
        foreignKeys.add(new ForeignKey(column, referencedTable, referencedColumn, "CASCADE", "CASCADE"));
        return this;
    }

    public TableBuilder foreignKey(String column, String referencedTable, String referencedColumn,
                                   String onDelete, String onUpdate) {
        foreignKeys.add(new ForeignKey(column, referencedTable, referencedColumn, onDelete, onUpdate));
        return this;
    }

// TABLE OPTIONS

    public TableBuilder engine(String engine) {
        this.engine = engine;
        return this;
    }

    public TableBuilder charset(String charset) {
        this.charset = charset;
        return this;
    }

    public TableBuilder collation(String collation) {
        this.collation = collation;
        return this;
    }

    public TableBuilder ifNotExists(boolean value) {
        this.ifNotExists = value;
        return this;
    }
// LOG
    public TableBuilder log(boolean value) {
        this.log = value;
        return this;
    }
// BUILD & EXECUTE

    public String build() {
        StringBuilder sql = new StringBuilder();

        sql.append("CREATE TABLE ");

        if (ifNotExists) sql.append("IF NOT EXISTS ");

        sql.append(tableName).append(" (\n");

        List<String> columnDefinitions = new ArrayList<>();

        for (Column column : columns) columnDefinitions.add("    " + column.build());
        for (Index index : indexes) columnDefinitions.add("    " + index.build());
        for (ForeignKey fk : foreignKeys) columnDefinitions.add("    " + fk.build());
        
        sql.append(String.join(",\n", columnDefinitions));
        sql.append("\n)");
        
        if (db.getDatabaseType().equals("mysql")) {
            sql.append(" ENGINE=").append(engine);
            sql.append(" DEFAULT charset=").append(charset);
            sql.append(" COLLATE=").append(collation);
        }

        return sql.toString();
    }

    public CompletableFuture<Integer> execute() {
        String sql = build();
        return db.updateAsync(sql);
    }

    public CompletableFuture<Integer> executeAndPrint() {
        String sql = build();

        if (log) {
            System.out.println("Executing SQL:");
            System.out.println(sql);
        }
        return db.updateAsync(sql);
    }

// COLUMN BUILDER TYPES
    // ================================================================
    // INTEGER TYPES
    // ================================================================

    public ColumnBuilder tinyint(String name) {
        return new ColumnBuilder(name, "TINYINT");
    }

    public ColumnBuilder smallint(String name) {
        return new ColumnBuilder(name, "SMALLINT");
    }

    public ColumnBuilder mediumint(String name) {
        return new ColumnBuilder(name, "MEDIUMINT");
    }

    public ColumnBuilder integer(String name) {
        return new ColumnBuilder(name, "INT");
    }

    public ColumnBuilder bigint(String name) {
        return new ColumnBuilder(name, "BIGINT");
    }

    // ================================================================
    // DECIMAL TYPES
    // ================================================================

    public ColumnBuilder decimal(String name, int precision, int scale) {
        return new ColumnBuilder(name, "DECIMAL(" + precision + ", " + scale + ")");
    }

    public ColumnBuilder floatType(String name) {
        return new ColumnBuilder(name, "FLOAT");
    }

    public ColumnBuilder doubleType(String name) {
        return new ColumnBuilder(name, "DOUBLE");
    }

    // ================================================================
    // STRING TYPES
    // ================================================================

    public ColumnBuilder charType(String name, int length) {
        return new ColumnBuilder(name, "CHAR(" + length + ")");
    }

    public ColumnBuilder varchar(String name, int length) {
        return new ColumnBuilder(name, "VARCHAR(" + length + ")");
    }

    public ColumnBuilder text(String name) {
        return new ColumnBuilder(name, "TEXT");
    }

    public ColumnBuilder mediumtext(String name) {
        return new ColumnBuilder(name, "MEDIUMTEXT");
    }

    public ColumnBuilder longtext(String name) {
        return new ColumnBuilder(name, "LONGTEXT");
    }

    // ================================================================
    // DATE/TIME TYPES
    // ================================================================

    public ColumnBuilder date(String name) {
        return new ColumnBuilder(name, "DATE");
    }

    public ColumnBuilder time(String name) {
        return new ColumnBuilder(name, "TIME");
    }

    public ColumnBuilder datetime(String name) {
        return new ColumnBuilder(name, "DATETIME");
    }

    public ColumnBuilder timestamp(String name) {
        return new ColumnBuilder(name, "TIMESTAMP");
    }

    public ColumnBuilder year(String name) {
        return new ColumnBuilder(name, "YEAR");
    }

    // ================================================================
    // BOOLEAN TYPES
    // ================================================================

    public ColumnBuilder bool(String name) {
        return new ColumnBuilder(name, "BOOLEAN");
    }

    public ColumnBuilder bit(String name, int length) {
        return new ColumnBuilder(name, "BIT(" + length + ")");
    }

    // ================================================================
    // BINARY TYPES
    // ================================================================

    public ColumnBuilder binary(String name, int length) {
        return new ColumnBuilder(name, "BINARY(" + length + ")");
    }

    public ColumnBuilder varbinary(String name, int length) {
        return new ColumnBuilder(name, "VARBINARY(" + length + ")");
    }

    public ColumnBuilder blob(String name) {
        return new ColumnBuilder(name, "BLOB");
    }

    public ColumnBuilder mediumblob(String name) {
        return new ColumnBuilder(name, "BLOB");
    }

    public ColumnBuilder longblob(String name) {
        return new ColumnBuilder(name, "BLOB");
    }

    // ================================================================
    // ENUM/SET TYPES
    // ================================================================

    public ColumnBuilder enumType(String name, String... values) {
        String valueList = String.join("', '", values);
        return new ColumnBuilder(name, "ENUM('" + valueList + "')");
    }

    public ColumnBuilder setType(String name, String... values) {
        String valueList = String.join("', '", values);
        return new ColumnBuilder(name, "SET('" + valueList + "')");
    }

    // ================================================================
    // JSON TYPE
    // ================================================================

    public ColumnBuilder json(String name) {
        return new ColumnBuilder(name, "JSON");
    }

    // ================================================================
    // CUSTOM TYPES
    // ================================================================

    public ColumnBuilder uuid(String name) {
        return new ColumnBuilder(name, "VARCHAR(36)");
    }

// INNER CLASSES

    // ================================================================
    // UTILITY BUILDERS
    // ================================================================

    public class ColumnBuilder {
        private final Column column;

        public ColumnBuilder(String name, String type) {
            this.column = new Column(name, type);
        }

        public ColumnBuilder unsigned() {
            column.unsigned = true;
            return this;
        }

        public ColumnBuilder notNull() {
            column.nullable = false;
            return this;
        }

        public ColumnBuilder nullable() {
            column.nullable = true;
            return this;
        }

        public ColumnBuilder defaultValue(Object value) {
            if (value instanceof String) {
                column.defaultValue = "'" + value + "'";
            } else if (value == null) {
                column.defaultValue = "NULL";
            } else {
                column.defaultValue = value.toString();
            }

            return this;
        }

        public ColumnBuilder defaultCurrentTimestamp() {
            column.defaultValue = "CURRENT_TIMESTAMP";
            return this;
        }

        public ColumnBuilder onUpdateCurrentTimestamp() {
            column.onUpdate = "CURRENT_TIMESTAMP";
            return this;
        }

        public ColumnBuilder unique() {
            column.unique = true;
            return this;
        }

        public ColumnBuilder autoIncrement() {
            column.autoIncrement = true;
            return this;
        }

        public ColumnBuilder primaryKey() {
            column.primaryKey = true;
            TableBuilder.this.primaryKey = column.name;
            return this;
        }

        public ColumnBuilder comment(String comment) {
            column.comment = comment;
            return this;
        }

        public TableBuilder build() {
            columns.add(column);
            return TableBuilder.this;
        }

        public TableBuilder end() {
            return build();
        }
    }

    // ================================================================
    // UTILITY MODELS
    // ================================================================

    private static class Column {
        String name;
        String type;
        boolean unsigned = false;
        boolean nullable = true;
        String defaultValue;
        String onUpdate;
        boolean unique = false;
        boolean autoIncrement = false;
        boolean primaryKey = false;
        String comment;

        Column(String name, String type) {
            this.name = name;
            this.type = type;
        }

        String build() {
            StringBuilder def = new StringBuilder();
            def.append(name).append(" ").append(type);

            if (unsigned) def.append(" UNSIGNED");
            if (!nullable) def.append(" NOT NULL");
            if (defaultValue != null) def.append(" DEFAULT ").append(defaultValue);
            if (onUpdate != null) def.append(" ON UPDATE ").append(onUpdate);
            if (unique) def.append(" UNIQUE");
            if (autoIncrement) def.append("AUTO_INCREMENT");
            if (primaryKey) def.append(" PRIMARY KEY");
            if (comment != null) def.append(" COMMENT '").append(comment).append("'");

            return def.toString();
        }

    }

    private static class Index {
        String name;
        boolean unique;
        boolean fulltext;
        String[] columns;

        Index(String name, boolean unique, boolean fulltext, String... columns) {
            this.name = name;
            this.unique = unique;
            this.fulltext = fulltext;
            this.columns = columns;
        }

        String build() {
            StringBuilder def = new StringBuilder();

            if (unique) {
                def.append("UNIQUE KEY ");
            } else if (fulltext) {
                def.append("FULLTEXT KEY ");
            } else {
                def.append("INDEX ");
            }

            def.append(name);
            def.append(" (").append(String.join(", ", columns)).append(")");

            return def.toString();
        }
    }

    private static class ForeignKey {
        String column;
        String referencedTable;
        String referencedColumn;
        String onDelete;
        String onUpdate;

        ForeignKey(String column, String referencedTable, String referencedColumn,
                   String onDelete, String onUpdate) {
            this.column = column;
            this.referencedTable = referencedTable;
            this.referencedColumn = referencedColumn;
            this.onDelete = onDelete;
            this.onUpdate = onUpdate;
        }

        String build() {
            return "FOREIGN KEY (" + column + ") REFERENCES " +
                    referencedTable + "(" + referencedColumn + ") " +
                    "ON DELETE " + onDelete + " ON UPDATE " + onUpdate;
        }
    }
}
