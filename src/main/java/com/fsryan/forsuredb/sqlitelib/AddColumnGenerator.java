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
import com.fsryan.forsuredb.migration.Migration;

import java.util.LinkedList;
import java.util.List;

public class AddColumnGenerator extends QueryGenerator {

    private final ColumnInfo column;

    public AddColumnGenerator(String tableName, ColumnInfo column) {
        super(tableName, Migration.Type.ALTER_TABLE_ADD_COLUMN);
        this.column = column;
    }

    @Override
    public List<String> generate() {
        List<String> queries = new LinkedList<>();
        queries.add("ALTER TABLE " + getTableName()
                + " ADD COLUMN " + column.getColumnName()
                + " " + TypeTranslator.from(column.getQualifiedType()).getSqlString()
                + (column.hasDefaultValue() ? " DEFAULT" + getDefaultValueFrom(column) : "") + ";");
        if (column.index()) {
            queries.addAll(new AddIndexGenerator(getTableName(), column).generate());
        }
        return queries;
    }

    private String getDefaultValueFrom(ColumnInfo column) {
        TypeTranslator tt = TypeTranslator.from(column.getQualifiedType());
        if (tt != TypeTranslator.DATE || !"CURRENT_TIMESTAMP".equals(column.defaultValue())) {
            return " '" + column.defaultValue() + "'";
        }
        return "(" + SqlGenerator.CURRENT_UTC_TIME + ")";
    }
}
