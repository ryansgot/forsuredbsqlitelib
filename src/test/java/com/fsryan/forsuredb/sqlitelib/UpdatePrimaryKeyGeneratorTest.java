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

import com.fsryan.forsuredb.api.info.TableInfo;
import com.fsryan.forsuredb.api.migration.QueryGenerator;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

import static com.fsryan.forsuredb.sqlitelib.SqlGenerator.CURRENT_UTC_TIME;
import static com.fsryan.forsuredb.sqlitelib.TestData.*;

@RunWith(Parameterized.class)
public class UpdatePrimaryKeyGeneratorTest extends BaseSQLiteGeneratorTest {

    private UpdatePrimaryKeyGenerator generatorUnderTest;

    private final String tableName;
    private final Map<String, TableInfo> targetSchema;

    public UpdatePrimaryKeyGeneratorTest(String tableName, Map<String, TableInfo> targetSchema, String... expectedSql) {
        super (expectedSql);
        this.tableName = tableName;
        this.targetSchema = targetSchema;
    }

    @Parameterized.Parameters
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] {
                {   // 00: default primary key case
                        "table_name",
                        tableMapOf(table().tableName("table_name")
                                .columnMap(columnMapOf(longCol().build()))
                                .build()),
                        new String[] {
                                "DROP TABLE IF EXISTS temp_table_name;",
                                "CREATE TEMP TABLE temp_table_name AS SELECT _id, created, deleted, modified, long_column FROM table_name;",
                                "DROP TABLE IF EXISTS table_name;",
                                "CREATE TABLE table_name(_id INTEGER PRIMARY KEY, created DATETIME DEFAULT(" + CURRENT_UTC_TIME + "), deleted INTEGER DEFAULT '0', modified DATETIME DEFAULT(" + CURRENT_UTC_TIME + "));",
                                "CREATE TRIGGER table_name_updated_trigger AFTER UPDATE ON table_name BEGIN UPDATE table_name SET modified=" + CURRENT_UTC_TIME + " WHERE _id=NEW._id; END;",
                                "ALTER TABLE table_name ADD COLUMN long_column INTEGER;",
                                "INSERT INTO table_name SELECT _id, created, deleted, modified, long_column FROM temp_table_name;",
                                "DROP TABLE IF EXISTS temp_table_name;"
                        }
                },
                {   // 01: non-default, non-composite primary key case
                        "table_name",
                        tableMapOf(table().tableName("table_name")
                                .primaryKey(new HashSet<>(Arrays.asList(stringCol().build().getColumnName())))
                                .columnMap(columnMapOf(stringCol().build()))
                                .build()),
                        new String[] {
                                "DROP TABLE IF EXISTS temp_table_name;",
                                "CREATE TEMP TABLE temp_table_name AS SELECT _id, created, deleted, modified, string_column FROM table_name;",
                                "DROP TABLE IF EXISTS table_name;",
                                "CREATE TABLE table_name(_id INTEGER, created DATETIME DEFAULT(" + CURRENT_UTC_TIME + "), deleted INTEGER DEFAULT '0', modified DATETIME DEFAULT(" + CURRENT_UTC_TIME + "), string_column TEXT PRIMARY KEY);",
                                "CREATE TRIGGER table_name_updated_trigger AFTER UPDATE ON table_name BEGIN UPDATE table_name SET modified=" + CURRENT_UTC_TIME + " WHERE string_column=NEW.string_column; END;",
                                "INSERT INTO table_name SELECT _id, created, deleted, modified, string_column FROM temp_table_name;",
                                "DROP TABLE IF EXISTS temp_table_name;"
                        }
                },
                {   // 02: non-default, non-composite primary key case with on conflict
                        "table_name",
                        tableMapOf(table().tableName("table_name")
                                .primaryKey(new HashSet<>(Arrays.asList(stringCol().build().getColumnName())))
                                .primaryKeyOnConflict("REPLACE")
                                .columnMap(columnMapOf(stringCol().build()))
                                .build()),
                        new String[] {
                                "DROP TABLE IF EXISTS temp_table_name;",
                                "CREATE TEMP TABLE temp_table_name AS SELECT _id, created, deleted, modified, string_column FROM table_name;",
                                "DROP TABLE IF EXISTS table_name;",
                                "CREATE TABLE table_name(_id INTEGER, created DATETIME DEFAULT(" + CURRENT_UTC_TIME + "), deleted INTEGER DEFAULT '0', modified DATETIME DEFAULT(" + CURRENT_UTC_TIME + "), string_column TEXT PRIMARY KEY ON CONFLICT REPLACE);",
                                "CREATE TRIGGER table_name_updated_trigger AFTER UPDATE ON table_name BEGIN UPDATE table_name SET modified=" + CURRENT_UTC_TIME + " WHERE string_column=NEW.string_column; END;",
                                "INSERT INTO table_name SELECT _id, created, deleted, modified, string_column FROM temp_table_name;",
                                "DROP TABLE IF EXISTS temp_table_name;"
                        }
                },
                {   // 03: non-default, composite primary key case with no on-conflict
                        "table_name",
                        tableMapOf(table().tableName("table_name")
                                .primaryKey(new HashSet<>(Arrays.asList(stringCol().build().getColumnName(), "string_column_2")))
                                .columnMap(columnMapOf(stringCol().build(),
                                        stringCol().columnName("string_column_2").build()))
                                .build()),
                        new String[] {
                                "DROP TABLE IF EXISTS temp_table_name;",
                                "CREATE TEMP TABLE temp_table_name AS SELECT _id, created, deleted, modified, string_column, string_column_2 FROM table_name;",
                                "DROP TABLE IF EXISTS table_name;",
                                "CREATE TABLE table_name(_id INTEGER, created DATETIME DEFAULT(" + CURRENT_UTC_TIME + "), deleted INTEGER DEFAULT '0', modified DATETIME DEFAULT(" + CURRENT_UTC_TIME + "), string_column TEXT, string_column_2 TEXT, PRIMARY KEY(string_column, string_column_2));",
                                "CREATE TRIGGER table_name_updated_trigger AFTER UPDATE ON table_name BEGIN UPDATE table_name SET modified=" + CURRENT_UTC_TIME + " WHERE string_column=NEW.string_column AND string_column_2=NEW.string_column_2; END;",
                                "INSERT INTO table_name SELECT _id, created, deleted, modified, string_column, string_column_2 FROM temp_table_name;",
                                "DROP TABLE IF EXISTS temp_table_name;"
                        }
                },
                {   // 04: non-default, composite primary key case with on-conflict
                        "table_name",
                        tableMapOf(table().tableName("table_name")
                                .primaryKey(new HashSet<>(Arrays.asList(stringCol().build().getColumnName(), "string_column_2")))
                                .primaryKeyOnConflict("FAIL")
                                .columnMap(columnMapOf(stringCol().build(),
                                        stringCol().columnName("string_column_2").build()))
                                .build()),
                        new String[] {
                                "DROP TABLE IF EXISTS temp_table_name;",
                                "CREATE TEMP TABLE temp_table_name AS SELECT _id, created, deleted, modified, string_column, string_column_2 FROM table_name;",
                                "DROP TABLE IF EXISTS table_name;",
                                "CREATE TABLE table_name(_id INTEGER, created DATETIME DEFAULT(" + CURRENT_UTC_TIME + "), deleted INTEGER DEFAULT '0', modified DATETIME DEFAULT(" + CURRENT_UTC_TIME + "), string_column TEXT, string_column_2 TEXT, PRIMARY KEY(string_column, string_column_2) ON CONFLICT FAIL);",
                                "CREATE TRIGGER table_name_updated_trigger AFTER UPDATE ON table_name BEGIN UPDATE table_name SET modified=" + CURRENT_UTC_TIME + " WHERE string_column=NEW.string_column AND string_column_2=NEW.string_column_2; END;",
                                "INSERT INTO table_name SELECT _id, created, deleted, modified, string_column, string_column_2 FROM temp_table_name;",
                                "DROP TABLE IF EXISTS temp_table_name;"
                        }
                },
        });
    }

    @Before
    public void setUp() {
        if (generatorUnderTest == null) {
            generatorUnderTest = new UpdatePrimaryKeyGenerator(tableName, targetSchema);
        }
    }

    @Override
    protected QueryGenerator getGenerator() {
        return generatorUnderTest;
    }
}
