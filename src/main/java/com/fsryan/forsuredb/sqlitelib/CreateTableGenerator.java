package com.fsryan.forsuredb.sqlitelib;

import com.fsryan.forsuredb.api.info.ColumnInfo;
import com.fsryan.forsuredb.api.info.ForeignKeyInfo;
import com.fsryan.forsuredb.api.info.TableForeignKeyInfo;
import com.fsryan.forsuredb.api.info.TableInfo;
import com.fsryan.forsuredb.api.migration.Migration;
import com.fsryan.forsuredb.api.migration.QueryGenerator;

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import static com.fsryan.forsuredb.sqlitelib.ApiInfo.DEFAULT_COLUMN_MAP;
import static com.fsryan.forsuredb.sqlitelib.SqlGenerator.CURRENT_UTC_TIME;

public class CreateTableGenerator extends QueryGenerator {

    private final TableInfo table;
    private final Map<String, TableInfo> targetSchema;
    private final boolean isCompositePrimaryKey;
    private final Set<TableForeignKeyInfo> foreignKeySet;
    private final List<String> foreignKeyColumnNames = new ArrayList<>();
    private final List<String> sortedPrimaryKeyColumnNames = new ArrayList<>();

    public CreateTableGenerator(String tableName, Map<String, TableInfo> targetSchema) {
        super(tableName, Migration.Type.CREATE_TABLE);
        table = targetSchema.get(tableName);
        this.targetSchema = targetSchema;

        foreignKeySet = table.getForeignKeys();
        if (foreignKeySet == null) {
            for (ColumnInfo column : table.getForeignKeyColumns()) {
                foreignKeyColumnNames.add(column.getColumnName());
            }
        } else {
            for (TableForeignKeyInfo foreignKey : foreignKeySet) {
                for (String columnName : foreignKey.getLocalToForeignColumnMap().keySet()) {
                    foreignKeyColumnNames.add(columnName);
                }
            }
        }
        Collections.sort(foreignKeyColumnNames);

        isCompositePrimaryKey = table.getPrimaryKey() != null && table.getPrimaryKey().size() > 1;
        if (table.getPrimaryKey() != null) {
            sortedPrimaryKeyColumnNames.addAll(table.getPrimaryKey());
            Collections.sort(sortedPrimaryKeyColumnNames);
        }
    }

    @Override
    public List<String> generate() {
        List<String> ret = new ArrayList<>(4);
        ret.add(createTableQuery());
        ret.add(modifiedTriggerQuery());
        ret.addAll(uniqueIndexQueries());
        return ret;
    }

    private String createTableQuery() {
        StringBuilder buf = new StringBuilder("CREATE TABLE ").append(getTableName()).append("(");
        for (ColumnInfo column : columnsToAdd()) {
            buf.append(columnDefinition(column)).append(", ");
        }
        buf.delete(buf.length() - 2, buf.length());

        if (sortedPrimaryKeyColumnNames.size() > 1) {
            buf.append(", PRIMARY KEY(");
            for (String primaryKeyColumnName : sortedPrimaryKeyColumnNames) {
                buf.append(primaryKeyColumnName).append(", ");
            }
            buf.delete(buf.length() - 2, buf.length()).append(')');
            if (table.getPrimaryKeyOnConflict() != null && !table.getPrimaryKeyOnConflict().isEmpty()) {
                buf.append(" ON CONFLICT ").append(table.getPrimaryKeyOnConflict());
            }
        }

        if (foreignKeySet == null) {
            for (ColumnInfo column : table.getForeignKeyColumns()) {
                addForeignKeyReferenceTo(buf, column.getForeignKeyInfo(), column.getColumnName());
            }
        } else {
            for (TableForeignKeyInfo foreignKey : foreignKeySet) {
                addForeignKeyReferenceTo(buf, foreignKey);
            }
        }

        return buf.append(");").toString();
    }

    private void addForeignKeyReferenceTo(StringBuilder buf, TableForeignKeyInfo foreignKey) {
        buf.append(", FOREIGN KEY(");

        StringBuilder foreignColumnBuf = new StringBuilder();
        List<Map.Entry<String, String>> sortedEntries = new ArrayList<>(foreignKey.getLocalToForeignColumnMap().entrySet());
        Collections.sort(sortedEntries, new Comparator<Map.Entry<String, String>>() {
            @Override
            public int compare(Map.Entry<String, String> entry1, Map.Entry<String, String> entry2) {
                // sorts the entries by their keys
                return entry1.getKey().compareTo(entry2.getKey());
            }
        });
        for (Map.Entry<String, String> entry : sortedEntries) {
            buf.append(entry.getKey()).append(", ");
            foreignColumnBuf.append(entry.getValue()).append(", ");
        }
        buf.delete(buf.length() - 2, buf.length())
                .append(") REFERENCES ").append(foreignKey.getForeignTableName())
                .append("(").append(foreignColumnBuf.delete(foreignColumnBuf.length() - 2, foreignColumnBuf.length()))
                .append(")");
        if (foreignKey.getUpdateChangeAction() != null && !foreignKey.getUpdateChangeAction().isEmpty()) {
            buf.append(" ON UPDATE ").append(foreignKey.getUpdateChangeAction());
        }
        if (foreignKey.getDeleteChangeAction() != null && !foreignKey.getDeleteChangeAction().isEmpty()) {
            buf.append(" ON DELETE ").append(foreignKey.getDeleteChangeAction());
        }
    }

    private void addForeignKeyReferenceTo(StringBuilder buf, ForeignKeyInfo foreignKey, String localColumn) {
        buf.append(", FOREIGN KEY(").append(localColumn)
                .append(") REFERENCES ").append(foreignKey.getTableName())
                .append("(").append(foreignKey.getColumnName())
                .append(")");
        if (foreignKey.getUpdateAction() != null) {
            buf.append(" ON UPDATE ").append(foreignKey.getUpdateAction().toString());
        }
        if (foreignKey.getDeleteAction() != null) {
            buf.append(" ON DELETE ").append(foreignKey.getDeleteAction().toString());
        }
    }

    private List<String> uniqueIndexQueries() {
        List<String> ret = new ArrayList<>();
        for (ColumnInfo column : table.getColumns()) {
            if (!column.isUnique()) {
                continue;
            }
            ret.addAll(new AddIndexGenerator(getTableName(), column).generate());
        }
        return ret;
    }

    private String columnDefinition(ColumnInfo column) {
        return column.getColumnName()
                + " " + TypeTranslator.from(column.getQualifiedType()).getSqlString()
                + (!isCompositePrimaryKey && sortedPrimaryKeyColumnNames.contains(column.getColumnName())
                        ? " PRIMARY KEY" + (table.getPrimaryKeyOnConflict() == null || table.getPrimaryKeyOnConflict().isEmpty() ? "" : " ON CONFLICT " + table.getPrimaryKeyOnConflict())
                        : "")
                + (column.isUnique() ? " UNIQUE" : "")
                + (column.hasDefaultValue() ? " DEFAULT" + getDefaultValueFrom(column) : "");
    }

    private String getDefaultValueFrom(ColumnInfo column) {
        TypeTranslator tt = TypeTranslator.from(column.getQualifiedType());
        if (tt != TypeTranslator.DATE || !"CURRENT_TIMESTAMP".equals(column.getDefaultValue())) {
            return " '" + column.getDefaultValue() + "'";
        }
        return "(" + CURRENT_UTC_TIME + ")";
    }

    private List<ColumnInfo> columnsToAdd() {
        List<ColumnInfo> ret = new ArrayList<>(DEFAULT_COLUMN_MAP.values());
        for (ColumnInfo column : targetSchema.get(getTableName()).getColumns()) {
            if (DEFAULT_COLUMN_MAP.keySet().contains(column.getColumnName())) {
                continue;
            }
            if (column.isUnique()
                    || foreignKeyColumnNames.contains(column.getColumnName())
                    || table.getPrimaryKey().contains(column.getColumnName())) {
                ret.add(column);
            }
        }
        Collections.sort(ret);
        return ret;
    }

    private String modifiedTriggerQuery() {
        return "CREATE TRIGGER "
                + getTableName() + "_updated_trigger AFTER UPDATE ON " + getTableName()
                + " BEGIN UPDATE " + getTableName() + " SET modified=" + CURRENT_UTC_TIME + " WHERE " + primaryKeyWhere() + "; END;";
    }

    private String primaryKeyWhere() {
        StringBuilder buf = new StringBuilder();
        for (String columnName : sortedPrimaryKeyColumnNames) {
            buf.append(columnName).append("=NEW.").append(columnName).append(" AND ");
        }
        return buf.delete(buf.length() - 5, buf.length()).toString();
    }
}
