package com.eclipselink.analyzer;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DDLInspector {

    public static class TableMetadata {
        public String tableName;
        public Map<String, ColumnMetadata> columns = new HashMap<>();
    }

    public static class ColumnMetadata {
        public String columnName;
        public String typeName;
        public int columnSize;
        public boolean isNullable;
    }

    public Map<String, TableMetadata> inspectSchema(Connection connection) throws SQLException {
        Map<String, TableMetadata> tables = new HashMap<>();
        DatabaseMetaData metaData = connection.getMetaData();

        try (ResultSet rsTables = metaData.getTables(null, null, "%", new String[] { "TABLE" })) {
            while (rsTables.next()) {
                String tableName = rsTables.getString("TABLE_NAME");
                TableMetadata table = new TableMetadata();
                table.tableName = tableName;

                try (ResultSet rsColumns = metaData.getColumns(null, null, tableName, "%")) {
                    while (rsColumns.next()) {
                        ColumnMetadata column = new ColumnMetadata();
                        column.columnName = rsColumns.getString("COLUMN_NAME");
                        column.typeName = rsColumns.getString("TYPE_NAME");
                        column.columnSize = rsColumns.getInt("COLUMN_SIZE");
                        column.isNullable = "YES".equals(rsColumns.getString("IS_NULLABLE"));
                        table.columns.put(column.columnName, column);
                    }
                }
                tables.put(tableName, table);
            }
        }
        return tables;
    }
}
