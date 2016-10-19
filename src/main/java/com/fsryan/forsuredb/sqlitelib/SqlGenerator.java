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

import com.fsryan.forsuredb.api.migration.Migration;
import com.fsryan.forsuredb.api.migration.MigrationSet;
import com.fsryan.forsuredb.api.sqlgeneration.DBMSIntegrator;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Strings.isNullOrEmpty;

public class SqlGenerator implements DBMSIntegrator {

    public static final String CURRENT_UTC_TIME = "STRFTIME('%Y-%m-%d %H:%M:%f', 'NOW')";

    @VisibleForTesting
    /*package*/ static final String EMPTY_SQL = ";";
    private static final Set<String> columnExclusionFilter = Sets.newHashSet("_id", "created", "modified");

    public SqlGenerator() {}

    @Override
    public List<String> generateMigrationSql(MigrationSet migrationSet) {
        if (migrationSet == null || !migrationSet.containsMigrations() || migrationSet.getTargetSchema() == null) {
            return new ArrayList<>();
        }

        QueryGeneratorFactory qgf = new QueryGeneratorFactory(migrationSet);
        List<String> sqlList = new ArrayList<>();
        for (Migration m : migrationSet.getOrderedMigrations()) {
            sqlList.addAll(qgf.getFor(m, migrationSet.getTargetSchema()).generate());
        }

        return sqlList;
    }

    @Override
    public String newSingleRowInsertionSql(String tableName, Map<String, String> columnValueMap) {
        if (isNullOrEmpty(tableName) || columnValueMap == null || columnValueMap.isEmpty()) {
            return EMPTY_SQL;
        }

        final StringBuilder queryBuf = new StringBuilder("INSERT INTO " + tableName + " (");
        final StringBuilder valueBuf = new StringBuilder();

        for (Map.Entry<String, String> colValEntry : columnValueMap.entrySet()) {
            final String columnName = colValEntry.getKey();
            if (columnName.isEmpty() || columnExclusionFilter.contains(columnName)) {
                continue;   // <-- never insert an _id column
            }
            final String val = colValEntry.getValue();
            if (!isNullOrEmpty(val)) {
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
}
