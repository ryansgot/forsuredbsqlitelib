/*
   forsuredb, an object relational mapping tool

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

import com.forsuredb.annotationprocessor.info.TableInfo;
import com.forsuredb.migration.Migration;
import com.forsuredb.migration.QueryGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class QueryGeneratorFactory {

    private static final QueryGenerator emptyGenerator = new QueryGenerator("empty", Migration.Type.DROP_TABLE) {
        @Override
        public List<String> generate() {
            return new ArrayList<>();
        }
    };

    public static QueryGenerator getFor(Migration migration, Map<String, TableInfo> targetContext) {
        // Guards against null pointer exception by passing back a query generator that does nothing
        if (migration == null || migration.getType() == null) {
            return emptyGenerator;
        }

        TableInfo table = targetContext.get(migration.getTableName());
        if (table == null) {
            return emptyGenerator;
        }

        switch (migration.getType()) {
            case CREATE_TABLE:
                return new CreateTableGenerator(table.getTableName());
            case ADD_FOREIGN_KEY_REFERENCE:
                return new AddForeignKeyGenerator(table, table.getColumn(migration.getColumnName()));
            case ALTER_TABLE_ADD_UNIQUE:
                return new AddUniqueColumnGenerator(table.getTableName(), table.getColumn(migration.getColumnName()));
            case ADD_UNIQUE_INDEX:
                return new AddUniqueIndexGenerator(table.getTableName(), table.getColumn(migration.getColumnName()));
            case DROP_TABLE:
                return new DropTableGenerator(table.getTableName());
            case ALTER_TABLE_ADD_COLUMN:
                return new AddColumnGenerator(table.getTableName(), table.getColumn(migration.getColumnName()));
        }

        return emptyGenerator;
    }
}
