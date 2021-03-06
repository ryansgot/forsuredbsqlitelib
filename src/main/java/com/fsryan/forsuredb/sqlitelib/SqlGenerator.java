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

import com.fsryan.forsuredb.api.Finder;
import com.fsryan.forsuredb.api.info.TableInfo;
import com.fsryan.forsuredb.api.migration.Migration;
import com.fsryan.forsuredb.api.migration.MigrationSet;
import com.fsryan.forsuredb.api.sqlgeneration.DBMSIntegrator;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class SqlGenerator implements DBMSIntegrator {

    public static final String CURRENT_UTC_TIME = "STRFTIME('%Y-%m-%d %H:%M:%f', 'NOW')";
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    public static final String CHANGE_ACTION_NO_ACTION = "NO ACTION";
    public static final String CHANGE_ACTION_RESTRICT = "RESTRICT";
    public static final String CHANGE_ACTION_SET_NULL = "SET NULL";
    public static final String CHANGE_ACTION_SET_DEFAULT = "SET DEFAULT";
    public static final String CHANGE_ACTION_CASCADE = "CASCADE";

    /*package*/ static final Set<Migration.Type> TYPES_REQUIRING_TABLE_RECREATION = new HashSet<>(4);
    static {
        TYPES_REQUIRING_TABLE_RECREATION.add(Migration.Type.CREATE_TABLE);
        TYPES_REQUIRING_TABLE_RECREATION.add(Migration.Type.CHANGE_DEFAULT_VALUE);
        TYPES_REQUIRING_TABLE_RECREATION.add(Migration.Type.UPDATE_FOREIGN_KEYS);
        TYPES_REQUIRING_TABLE_RECREATION.add(Migration.Type.UPDATE_PRIMARY_KEY);
    }

    // visible for testing
    /*package*/ static final String EMPTY_SQL = ";";
    private static final Set<String> columnExclusionFilter = new HashSet<>(Arrays.asList("_id", "created", "modified"));

    public SqlGenerator() {}

    @Override
    public List<String> generateMigrationSql(MigrationSet migrationSet) {
        if (migrationSet == null || !migrationSet.containsMigrations() || migrationSet.getTargetSchema() == null) {
            return new ArrayList<>();
        }

        QueryGeneratorFactory qgf = new QueryGeneratorFactory(migrationSet);
        List<Migration> migrations = migrationSet.getOrderedMigrations();
        Collections.sort(migrations, new MigrationComparator(migrationSet.getTargetSchema()));
        List<String> sqlList = new ArrayList<>();
        Set<String> recreatedTables = new HashSet<>();
        for (Migration m : migrations) {
            if (recreatedTables.contains(m.getTableName()) && isMigrationHandledOnCreate(m, migrationSet.getTargetSchema())) {
                continue;
            }
            if (TYPES_REQUIRING_TABLE_RECREATION.contains(m.getType())) {
                recreatedTables.add(m.getTableName());
            }
            sqlList.addAll(qgf.getFor(m, migrationSet.getTargetSchema()).generate());
        }

        return sqlList;
    }

    @Override
    public String newSingleRowInsertionSql(String tableName, Map<String, String> columnValueMap) {
        if (tableName == null || tableName.isEmpty() || columnValueMap == null || columnValueMap.isEmpty()) {
            return EMPTY_SQL;
        }

        final StringBuilder queryBuf = new StringBuilder("INSERT INTO " + tableName + " (");
        final StringBuilder valueBuf = new StringBuilder();

        for (Map.Entry<String, String> colValEntry : columnValueMap.entrySet()) {
            final String columnName = colValEntry.getKey();
            if (columnName.isEmpty() || columnExclusionFilter.contains(columnName)) {
                continue;   // <-- never insert _id, created, or modified columns
            }
            final String val = colValEntry.getValue();
            if (val != null && !val.isEmpty()) {
                queryBuf.append(columnName).append(", ");
                valueBuf.append("'").append(val).append("', ");
            }
        }

        queryBuf.delete(queryBuf.length() - 2, queryBuf.length());  // <-- remove final ", "
        valueBuf.delete(valueBuf.length() - 2, valueBuf.length());  // <-- remove final ", "
        return queryBuf.append(") VALUES (").append(valueBuf.toString()).append(");").toString();
    }

    @Override
    public String unambiguousColumn(String tableName, String columnName) {
        return tableName + "." + columnName;
    }

    @Override
    public String unambiguousRetrievalColumn(String tableName, String columnName) {
        return unambiguousColumn(tableName, columnName);
    }

    @Override
    public String orderByAsc(String tableName, String columnName) {
        return unambiguousColumn(tableName, columnName) + " ASC";
    }

    @Override
    public String orderByDesc(String tableName, String columnName) {
        return unambiguousColumn(tableName, columnName) + " DESC";
    }

    @Override
    public String combineOrderByExpressions(List<String> orderByList) {
        return orderByList.size() == 0 ? "" : orderByList.toString().replaceAll("(\\[|\\])", "");
    }

    @Override
    public String whereOperation(String tableName, String column, int operation) {
        switch (operation) {
            case Finder.OP_EQ: return unambiguousColumn(tableName, column) + " =";
            case Finder.OP_GE: return unambiguousColumn(tableName, column) + " >=";
            case Finder.OP_GT: return unambiguousColumn(tableName, column) + " >";
            case Finder.OP_LE: return unambiguousColumn(tableName, column) + " <=";
            case Finder.OP_LIKE: return unambiguousColumn(tableName, column) + " LIKE";
            case Finder.OP_LT: return unambiguousColumn(tableName, column) + " <";
            case Finder.OP_NE: return unambiguousColumn(tableName, column) + " !=";
        }
        return "";
    }

    @Override
    public String formatDate(Date date) {
        return DATE_FORMAT.format(date);
    }

    @Override
    public Date parseDate(String dateStr) {
        try {
            return DATE_FORMAT.parse(dateStr);
        } catch (ParseException pe) {
            pe.printStackTrace();
        }
        return null;
    }

    @Override
    public String wildcardKeyword() {
        return "%";
    }

    @Override
    public String andKeyword() {
        return "AND";
    }

    @Override
    public String orKeyword() {
        return "OR";
    }

    private static boolean isMigrationHandledOnCreate(Migration m, Map<String, TableInfo> targetSchema) {
        switch (m.getType()) {
            case ADD_UNIQUE_INDEX:
                // intentionally falling through
            case ADD_FOREIGN_KEY_REFERENCE:
                return true;
            case ALTER_TABLE_ADD_COLUMN:
                TableInfo table = targetSchema.get(m.getTableName());
                return table.isForeignKeyColumn(m.getColumnName())
                        || table.getPrimaryKey().contains(m.getColumnName())
                        || table.getColumn(m.getColumnName()).isUnique();
        }
        return false;
    }
}
