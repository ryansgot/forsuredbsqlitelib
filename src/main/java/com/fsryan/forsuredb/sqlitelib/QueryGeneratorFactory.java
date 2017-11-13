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
import com.fsryan.forsuredb.info.TableForeignKeyInfo;
import com.fsryan.forsuredb.info.TableInfo;
import com.fsryan.forsuredb.migration.Migration;
import com.fsryan.forsuredb.migration.MigrationSet;
import com.fsryan.forsuredb.serialization.FSDbInfoSerializer;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.*;

import static com.fsryan.forsuredb.migration.Migration.Type.*;
import static com.fsryan.forsuredb.sqlitelib.ApiInfo.DEFAULT_COLUMN_MAP;

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
    private static final Type tableForeignKeysInfoSetType = new TypeToken<Set<TableForeignKeyInfo>>() {}.getType();
    private static final Gson gson = new Gson();

    // tableName -> list of column names that are NEW foreign key columns--not existing
    private final Map<String, List<String>> newForeignKeyColumnMap;

    public QueryGeneratorFactory(MigrationSet migrationSet) {
        newForeignKeyColumnMap = createNewForeignKeyMap(migrationSet);
    }

    public QueryGenerator getFor(Migration migration, Map<String, TableInfo> targetSchema, FSDbInfoSerializer serializer) {
        // Guards against null pointer exception by passing back a query generator that does nothing
        if (migration == null || migration.type() == null) {
            return emptyGenerator;
        }

        TableInfo table = targetSchema.get(migration.tableName());
        if (migration.type() != Migration.Type.DROP_TABLE && table == null) {
            return emptyGenerator;  // <-- the target context will not have the table if it is about to be dropped
        }

        switch (migration.type()) {
            case CREATE_TABLE:
                return new CreateTableGenerator(migration.tableName(), targetSchema);
            case ADD_FOREIGN_KEY_REFERENCE:
                List<String> allForeignKeys = newForeignKeyColumnMap.remove(migration.tableName());
                if (allForeignKeys == null) {   // <-- migration has already been run that creates all foreign keys
                    return emptyGenerator;
                }
                return new AddForeignKeyGenerator(table, listOfColumnInfo(table, allForeignKeys), targetSchema);
            // TODO: figure out whether you will do anything with this or just always put the unique columns in the table create queries
//            case ALTER_TABLE_ADD_UNIQUE:
//                return new AddUniqueColumnGenerator(table.tableName(), table.getColumn(migration.columnName()));
            case MAKE_COLUMN_UNIQUE:
                // Intentionally falling through
            case ADD_UNIQUE_INDEX:
                return new AddIndexGenerator(table.tableName(), table.getColumn(migration.columnName()), true);
            case ADD_INDEX:
                return new AddIndexGenerator(table.tableName(), table.getColumn(migration.columnName()));
            case ALTER_TABLE_ADD_COLUMN:
                return new AddColumnGenerator(table.tableName(), table.getColumn(migration.columnName()));
            case DROP_TABLE:
                return new DropTableGenerator(migration.tableName());
            case CHANGE_DEFAULT_VALUE:
                return new ChangeDefaultValueGenerator(migration.tableName(), targetSchema);
            case UPDATE_PRIMARY_KEY:
                return new UpdatePrimaryKeyGenerator(migration.tableName(), existingColumnNamesFrom(migration), targetSchema);
            case UPDATE_FOREIGN_KEYS:
                final String currentForeignKeysJson = migration.extras().get(migration.extras().get("current_foreign_keys"));
                final Set<TableForeignKeyInfo> currentForeignKeys = gson.fromJson(currentForeignKeysJson, tableForeignKeysInfoSetType);
                return new UpdateForeignKeysGenerator(table.tableName(), currentForeignKeys, existingColumnNamesFrom(migration), targetSchema);
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
        if (migrationSet == null || !migrationSet.containsMigrations() || migrationSet.targetSchema() == null) {
            return Collections.emptyMap();
        }

        Map<String, List<String>> retMap = new HashMap<>();
        for (Migration m : migrationSet.orderedMigrations()) {
            if (m.type() != ADD_FOREIGN_KEY_REFERENCE) {
                continue;
            }
            List<String> newForeignKeyColumns = retMap.get(m.tableName());
            if (newForeignKeyColumns == null) {
                List<String> columnNames = new ArrayList<>();
                columnNames.add(m.columnName());
                retMap.put(m.tableName(), columnNames);
            } else {
                newForeignKeyColumns.add(m.columnName());
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

    private static Set<String> existingColumnNamesFrom(Migration migration) {
        final String currentColumnsJson = migration.extras().get("existing_column_names");
        if (currentColumnsJson == null) {
            Set<String> ret = new HashSet<>();
            for (ColumnInfo column : DEFAULT_COLUMN_MAP.values()) {
                ret.add(column.columnName());
            }
            return ret;
        }
        return gson.fromJson(currentColumnsJson, new TypeToken<Set<String>>() {}.getType());
    }
}
