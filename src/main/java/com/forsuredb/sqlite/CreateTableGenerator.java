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

import com.forsuredb.annotationprocessor.info.ColumnInfo;
import com.forsuredb.annotationprocessor.info.TableInfo;
import com.forsuredb.migration.Migration;
import com.forsuredb.migration.QueryGenerator;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class CreateTableGenerator extends QueryGenerator {

    private final Map<String, TableInfo> targetSchema;

    public CreateTableGenerator(String tableName, Map<String, TableInfo> targetSchema) {
        super(tableName, Migration.Type.CREATE_TABLE);
        this.targetSchema = targetSchema;
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
        appendDefaultColumns(buf);
        appendUniqueColumns(buf);
        return buf.append(");").toString();
    }

    private void appendDefaultColumns(StringBuilder buf) {
        int startingLength = buf.length();
        for (ColumnInfo column : TableInfo.DEFAULT_COLUMNS.values()) {
            if (startingLength != buf.length()) {
                buf.append(", ");
            }
            buf.append(columnDefinition(column));
        }
    }

    private void appendUniqueColumns(StringBuilder buf) {
        for (ColumnInfo column : targetSchema.get(getTableName()).getColumns()) {
            if (column.isUnique()) {
                buf.append(", ").append(columnDefinition(column));
            }
        }
    }

    private List<String> uniqueIndexQueries() {
        List<String> ret = new ArrayList<>();
        for (ColumnInfo column : targetSchema.get(getTableName()).getColumns()) {
            if (!column.isUnique()) {
                continue;
            }
            ret.addAll(new AddUniqueIndexGenerator(getTableName(), column).generate());
        }
        return ret;
    }

    private String columnDefinition(ColumnInfo column) {
        return column.getColumnName()
                + " " + TypeTranslator.from(column.getQualifiedType()).getSqlString()
                + (column.isPrimaryKey() ? " PRIMARY KEY" : "")
                + (column.isUnique() ? " UNIQUE" : "")
                + (column.hasDefaultValue() ? " DEFAULT " + column.getDefaultValue() : "");
    }

    private String modifiedTriggerQuery() {
        return "CREATE TRIGGER "
                + getTableName() + "_updated_trigger AFTER UPDATE ON " + getTableName()
                + " BEGIN UPDATE " + getTableName() + " SET modified=CURRENT_TIMESTAMP WHERE _id=NEW._id; END;";
    }
}
