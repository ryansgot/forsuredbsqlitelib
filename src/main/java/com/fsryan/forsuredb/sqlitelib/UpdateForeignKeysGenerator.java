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

import com.fsryan.forsuredb.api.info.ColumnInfo;
import com.fsryan.forsuredb.api.info.TableForeignKeyInfo;
import com.fsryan.forsuredb.api.info.TableInfo;
import com.fsryan.forsuredb.api.migration.Migration;
import com.fsryan.forsuredb.api.migration.QueryGenerator;

import java.util.*;

public class UpdateForeignKeysGenerator extends QueryGenerator {

    private final TableInfo table;
    private final List<ColumnInfo> newForeignKeyColumns;
    private final Set<TableForeignKeyInfo> currentForeignKeys;
    private final Map<String, TableInfo> targetSchema;

    public UpdateForeignKeysGenerator(String tableName,
                                      Set<TableForeignKeyInfo> currentForeignKeys,
                                      Set<String> currentColumns,
                                      Map<String, TableInfo> targetSchema) {
        super(tableName, Migration.Type.UPDATE_FOREIGN_KEYS);
        table = targetSchema.get(tableName);
        this.currentForeignKeys = currentForeignKeys;
        this.newForeignKeyColumns = new ArrayList<>();
        for (TableForeignKeyInfo foreignKey : table.getForeignKeys()) {
            for (String localColumnName : foreignKey.getLocalToForeignColumnMap().keySet()) {
                if (currentColumns.contains(localColumnName)) {
                    continue;
                }
                newForeignKeyColumns.add(table.getColumn(localColumnName));
            }
        }
        this.targetSchema = targetSchema;
    }

    @Override
    public List<String> generate() {
        List<String> retList = new ArrayList<>();

        retList.addAll(new CreateTempTableFromExisting(table, newForeignKeyColumns).generate());
        retList.addAll(new DropTableGenerator(getTableName()).generate());
        retList.addAll(new CreateTableGenerator(getTableName(), targetSchema).generate());
        for (ColumnInfo columnInfo : table.getNonForeignKeyColumns()) { // TODO: update this to filter based upon TableForeignKeyInfo
            if (TableInfo.DEFAULT_COLUMNS.containsKey(columnInfo.getColumnName()) || columnInfo.isUnique()) {
                continue;   // <-- these columns were added in the CREATE TABLE query
            }
            retList.addAll(new AddColumnGenerator(getTableName(), columnInfo).generate());
        }
        retList.add(reinsertDataQuery());
        retList.addAll(new DropTableGenerator(tempTableName()).generate());

        return retList;
    }

    private String reinsertDataQuery() {
        StringBuilder buf = new StringBuilder("INSERT INTO ").append(getTableName()).append(" SELECT ");
        List<ColumnInfo> tableColumns = new LinkedList<>(table.getColumns());
        Collections.sort(tableColumns);
        for (ColumnInfo tableColumn : tableColumns) {
            if (newForeignKeyColumns.contains(tableColumn)) {
                buf.append(", null AS ").append(tableColumn.getColumnName());
            } else {
                buf.append("_id".equals(tableColumn.getColumnName()) ? "" : ", ").append(tableColumn.getColumnName());
            }
        }
        return buf.append(" FROM ").append(tempTableName()).append(";").toString();
    }

    private String tempTableName() {
        return "temp_" + getTableName();
    }
}
