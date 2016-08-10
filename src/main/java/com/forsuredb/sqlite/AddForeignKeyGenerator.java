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
package com.forsuredb.sqlite;

import com.fsryan.forsuredb.api.info.ColumnInfo;
import com.fsryan.forsuredb.api.info.TableInfo;
import com.fsryan.forsuredb.api.migration.Migration;
import com.fsryan.forsuredb.api.migration.QueryGenerator;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class AddForeignKeyGenerator extends QueryGenerator {

    private final TableInfo table;
    private final List<ColumnInfo> newForeignKeyColumns;
    private final Map<String, TableInfo> targetSchema;

    /**
     * <p>
     *     Add only one foreign key column to the table
     * </p>
     * @param table the table to which foreign key columns should be added
     * @param column the foreign key column to add
     */
    public AddForeignKeyGenerator(TableInfo table, ColumnInfo column, Map<String, TableInfo> targetSchema) {
        this(table, Lists.newArrayList(column), targetSchema);
    }

    /**
     * <p>
     *     Use this when there are multiple foreign keys to add to the same table
     * </p>
     * @param table the table to which foreign key columns should be added
     * @param newForeignKeyColumns a list of all new foreign key columns to add
     */
    public AddForeignKeyGenerator(TableInfo table, List<ColumnInfo> newForeignKeyColumns, Map<String, TableInfo> targetSchema) {
        super(table.getTableName(), Migration.Type.ADD_FOREIGN_KEY_REFERENCE);
        this.table = table;
        this.newForeignKeyColumns = newForeignKeyColumns;
        this.targetSchema = targetSchema;
    }

    @Override
    public List<String> generate() {
        List<String> retList = new LinkedList<>();

        retList.addAll(new CreateTempTableFromExisting(table, newForeignKeyColumns).generate());
        retList.addAll(new DropTableGenerator(getTableName()).generate());
        retList.addAll(recreateTableWithAllForeignKeysQuery());
        retList.addAll(allColumnAdditionQueries());
        retList.add(reinsertDataQuery());
        retList.addAll(new DropTableGenerator(tempTableName()).generate());

        return retList;
    }

    private List<String> recreateTableWithAllForeignKeysQuery() {
        final List<String> retList = new LinkedList<>();
        List<String> normalCreationQueries = new com.forsuredb.sqlite.CreateTableGenerator(getTableName(), targetSchema).generate();

        // add the default columns to the normal TABLE CREATE query
        StringBuffer buf = new StringBuffer(normalCreationQueries.remove(0));
        buf.delete(buf.length() - 2, buf.length());   // <-- removes );
        List<ColumnInfo> foreignKeyColumns = table.getForeignKeyColumns();
        addColumnDefinitionsToBuffer(buf, foreignKeyColumns);
        for (ColumnInfo fKeyColumn : newForeignKeyColumns) {
            addColumnDefinitionToBuffer(buf, fKeyColumn);
        }
        addForeignKeyDefinitionsToBuffer(buf, foreignKeyColumns);
        for (ColumnInfo fKeyColumn : newForeignKeyColumns) {
            addForeignKeyDefinitionToBuffer(buf, fKeyColumn);
        }
        retList.add(buf.append(");").toString());

        // add all remaining table create queries
        while (normalCreationQueries.size() > 0) {
            retList.add(normalCreationQueries.remove(0));
        }

        return retList;
    }

    private List<String> allColumnAdditionQueries() {
        List<String> retList = new LinkedList<>();
        for (ColumnInfo columnInfo : table.getNonForeignKeyColumns()) {
            if (TableInfo.DEFAULT_COLUMNS.containsKey(columnInfo.getColumnName()) || columnInfo.isUnique()) {
                continue;   // <-- these columns were added in the CREATE TABLE query
            }

            retList.addAll(new AddColumnGenerator(getTableName(), columnInfo).generate());
        }

        return retList;
    }

    private String reinsertDataQuery() {
        StringBuffer buf = new StringBuffer("INSERT INTO ").append(getTableName()).append(" SELECT ");
        List<ColumnInfo> tableColumns = new LinkedList<>(table.getColumns());
        Collections.sort(tableColumns);
        // append all of the previously existing columns first
        for (ColumnInfo tableColumn : tableColumns) {
            if (newForeignKeyColumns.contains(tableColumn)) {
                buf.append(", null AS ").append(tableColumn.getColumnName());
            } else {
                buf.append("_id".equals(tableColumn.getColumnName()) ? "" : ", ").append(tableColumn.getColumnName());
            }
        }
        // append the new foreign key last
        return buf.append(" FROM ").append(tempTableName()).append(";").toString();
    }

    private String tempTableName() {
        return "temp_" + getTableName();
    }

    private void addColumnDefinitionsToBuffer(StringBuffer buf, List<ColumnInfo> columns) {
        for (ColumnInfo column : columns) {
            if (!newForeignKeyColumns.contains(column)) {
                addColumnDefinitionToBuffer(buf, column);
            }
        }
    }

    private void addColumnDefinitionToBuffer(StringBuffer buf, ColumnInfo column) {
        buf.append(", ").append(column.getColumnName())
                .append(" ").append(TypeTranslator.from(column.getQualifiedType()).getSqlString());
    }

    private void addForeignKeyDefinitionsToBuffer(StringBuffer buf, List<ColumnInfo> columns) {
        for (ColumnInfo column : columns) {
            if (!newForeignKeyColumns.contains(column)) {
                addForeignKeyDefinitionToBuffer(buf, column);
            }
        }
    }

    private void addForeignKeyDefinitionToBuffer(StringBuffer buf, ColumnInfo column) {
        buf.append(", FOREIGN KEY(").append(column.getColumnName())
                .append(") REFERENCES ").append(column.getForeignKeyInfo().getTableName())
                .append("(").append(column.getForeignKeyInfo().getColumnName())
                .append(")")
                .append(" ON UPDATE ").append(column.getForeignKeyInfo().getUpdateAction().toString())
                .append(" ON DELETE ").append(column.getForeignKeyInfo().getDeleteAction().toString());
    }
}
