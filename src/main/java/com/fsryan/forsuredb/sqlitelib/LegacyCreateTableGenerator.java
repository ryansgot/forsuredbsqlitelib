/*
   forsuredbsqlitelib, sqlite library for the forsuredb project

   Copyright 2015 Ryan Scott

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package com.fsryan.forsuredb.sqlitelib;

import com.fsryan.forsuredb.api.migration.QueryGenerator;
import com.fsryan.forsuredb.info.ColumnInfo;
import com.fsryan.forsuredb.info.TableInfo;
import com.fsryan.forsuredb.migration.Migration;

import java.util.*;

import static com.fsryan.forsuredb.sqlitelib.ApiInfo.DEFAULT_COLUMN_MAP;
import static com.fsryan.forsuredb.sqlitelib.SqlGenerator.CURRENT_UTC_TIME;

/**
 * <p>
 *     Depends upon foreign keys and primary key being marked on the columns.
 *     Only used by deprecated {@link AddForeignKeyGenerator}
 * </p>
 */
@Deprecated
public class LegacyCreateTableGenerator extends QueryGenerator {

    private final Map<String, TableInfo> targetSchema;
    private final Set<String> primaryKey;
    private final String primaryKeyOnConflict;
    private final List<String> sortedPrimaryKeyColumnNames;

    public LegacyCreateTableGenerator(String tableName, Map<String, TableInfo> targetSchema) {
        super(tableName, Migration.Type.CREATE_TABLE);
        this.targetSchema = targetSchema;
        TableInfo table = targetSchema.get(tableName);
        primaryKey = table.getPrimaryKey();
        primaryKeyOnConflict = table.primaryKeyOnConflict();
        sortedPrimaryKeyColumnNames = new ArrayList<>(primaryKey);
        Collections.sort(sortedPrimaryKeyColumnNames);
    }

    @Override
    public List<String> generate() {
        List<String> queries = new LinkedList<>();
        queries.add(createTableQuery());
        queries.add(modifiedTriggerQuery());
        queries.addAll(uniqueIndexQueries());
        return queries;
    }

    private String createTableQuery() {
        StringBuilder buf = new StringBuilder("CREATE TABLE ").append(getTableName()).append("(");
        List<ColumnInfo> columnsToAdd = determineColumnsToAdd();
        Collections.sort(columnsToAdd);
        for (ColumnInfo column : columnsToAdd) {
            buf.append(columnDefinition(column)).append(", ");
        }
        buf.delete(buf.length() - 2, buf.length());
        if (primaryKey.size() > 1) {
            buf.append(", PRIMARY KEY(");
            for (String primaryKeyColumnName : sortedPrimaryKeyColumnNames) {
                buf.append(primaryKeyColumnName).append(", ");
            }
            buf.delete(buf.length() - 2, buf.length()).append(')');
            if (primaryKeyOnConflict != null && !primaryKeyOnConflict.isEmpty()) {
                buf.append(" ON CONFLICT ").append(primaryKeyOnConflict);
            }
        }
        return buf.append(");").toString();
    }

    private List<ColumnInfo> determineColumnsToAdd() {
        List<ColumnInfo> ret = new ArrayList<>(DEFAULT_COLUMN_MAP.values());
        for (ColumnInfo column : targetSchema.get(getTableName()).getColumns()) {
            if (column.getColumnName().equals(TableInfo.DEFAULT_PRIMARY_KEY_COLUMN)) {
                continue;
            }
            if (column.unique() || primaryKey.contains(column.getColumnName())) {
                ret.add(column);
            }
        }
        return ret;
    }

    private List<String> uniqueIndexQueries() {
        List<String> ret = new ArrayList<>();
        for (ColumnInfo column : targetSchema.get(getTableName()).getColumns()) {
            if (!column.unique()) {
                continue;
            }
            ret.addAll(new AddIndexGenerator(getTableName(), column).generate());
        }
        return ret;
    }

    private String columnDefinition(ColumnInfo column) {
        return column.getColumnName()
                + " " + TypeTranslator.from(column.getQualifiedType()).getSqlString()
                + (primaryKey.size() == 1 && primaryKey.contains(column.getColumnName()) ? " PRIMARY KEY" + (primaryKeyOnConflict == null || primaryKeyOnConflict.isEmpty() ? "" : " ON CONFLICT " + primaryKeyOnConflict): "")
                + (column.unique() ? " UNIQUE" : "")
                + (column.hasDefaultValue() ? " DEFAULT" + getDefaultValueFrom(column) : "");
    }

    private String getDefaultValueFrom(ColumnInfo column) {
        TypeTranslator tt = TypeTranslator.from(column.getQualifiedType());
        if (tt != TypeTranslator.DATE || !"CURRENT_TIMESTAMP".equals(column.defaultValue())) {
            return " '" + column.defaultValue() + "'";
        }
        return "(" + CURRENT_UTC_TIME + ")";
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
