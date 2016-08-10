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
import com.fsryan.forsuredb.api.migration.MigrationSet;
import com.fsryan.forsuredb.api.migration.QueryGenerator;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 *     QueryGeneratorFactory is intended to create all of the {@link QueryGenerator} objects that generate the
 *     correct queries in the correct order to perform all of the migrations in the {@link MigrationSet} that is the
 *     argument to the constructor.
 * </p>
 */
public class QueryGeneratorFactory {

    private static final QueryGenerator emptyGenerator = new QueryGenerator("empty", Migration.Type.DROP_TABLE) {
        @Override
        public List<String> generate() {
            return new ArrayList<>();
        }
    };

    // tableName -> list of column names that are NEW foreign key columns--not existing
    private final Map<String, List<String>> newForeignKeyColumnMap;

    public QueryGeneratorFactory(MigrationSet migrationSet) {
        newForeignKeyColumnMap = createNewForeignKeyMap(migrationSet);
    }

    public QueryGenerator getFor(Migration migration, Map<String, TableInfo> targetSchema) {
        // Guards against null pointer exception by passing back a query generator that does nothing
        if (migration == null || migration.getType() == null) {
            return emptyGenerator;
        }

        TableInfo table = targetSchema.get(migration.getTableName());
        if (migration.getType() != Migration.Type.DROP_TABLE && table == null) {
            return emptyGenerator;  // <-- the target context will not have the table if it is about to be dropped
        }

        switch (migration.getType()) {
            case CREATE_TABLE:
                return new CreateTableGenerator(table.getTableName(), targetSchema);
            case ADD_FOREIGN_KEY_REFERENCE:
                List<String> allForeignKeys = newForeignKeyColumnMap.remove(migration.getTableName());
                if (allForeignKeys == null) {   // <-- migration has already been run that creates all foreign keys
                    return emptyGenerator;
                }
                return new AddForeignKeyGenerator(table, listOfColumnInfo(table, allForeignKeys), targetSchema);
//            case ALTER_TABLE_ADD_UNIQUE:
//                return new AddUniqueColumnGenerator(table.getTableName(), table.getColumn(migration.getColumnName()));
            case ADD_UNIQUE_INDEX:
                return new AddUniqueIndexGenerator(table.getTableName(), table.getColumn(migration.getColumnName()));
            case ALTER_TABLE_ADD_COLUMN:
                return new AddColumnGenerator(table.getTableName(), table.getColumn(migration.getColumnName()));
            case DROP_TABLE:
                return new DropTableGenerator(migration.getTableName());
        }

        return emptyGenerator;
    }

    /**
     * <p>
     *     Foreign key migrations are difficult in that, in SQLite, they mist be added when the table is created.
     *     This is problematic because, if you have multiple foreign keys to add to the same table in one migration
     *     set, then you cannot know the correct temp table to create for temporary data storage simply by looking at
     *     the target context. You actually have to know all of the foreign key columns to add for a table prior to
     *     adding any foreign key column.
     * </p>
     * @param migrationSet the full set of migrations that this query generator factory is creating queries
     * @return a map of tableName to list of column names that are new foreign keys
     */
    private Map<String, List<String>> createNewForeignKeyMap(MigrationSet migrationSet) {
        if (migrationSet == null || !migrationSet.containsMigrations() || migrationSet.getTargetSchema() == null) {
            return Collections.emptyMap();
        }

        Map<String, List<String>> retMap = new HashMap<>();
        for (Migration m : migrationSet.getOrderedMigrations()) {
            if (m.getType() != Migration.Type.ADD_FOREIGN_KEY_REFERENCE) {
                continue;
            }
            List<String> newForeignKeyColumns = retMap.get(m.getTableName());
            if (newForeignKeyColumns == null) {
                retMap.put(m.getTableName(), Lists.newArrayList(m.getColumnName()));
            } else {
                newForeignKeyColumns.add(m.getColumnName());
            }
        }

        return retMap;
    }

    private List<ColumnInfo> listOfColumnInfo(TableInfo table, List<String> columnNames) {
        List<ColumnInfo> retList = new ArrayList<>();
        for (String columnName : columnNames) {
            retList.add(table.getColumn(columnName));
        }
        return retList;
    }
}
