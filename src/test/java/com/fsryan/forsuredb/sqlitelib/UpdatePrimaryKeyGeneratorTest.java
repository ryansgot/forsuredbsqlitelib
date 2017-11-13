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
import com.fsryan.forsuredb.info.TableInfo;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.fsryan.forsuredb.sqlitelib.SqlGenerator.CURRENT_UTC_TIME;
import static com.fsryan.forsuredb.sqlitelib.TestData.*;

@RunWith(Parameterized.class)
public class UpdatePrimaryKeyGeneratorTest extends BaseSQLiteGeneratorTest {

    private UpdatePrimaryKeyGenerator generatorUnderTest;

    private final String tableName;
    private final Set<String> existingColumnNames;
    private final Map<String, TableInfo> targetSchema;

    public UpdatePrimaryKeyGeneratorTest(String tableName, Set<String> existingColumnNames, Map<String, TableInfo> targetSchema, String... expectedSql) {
        super (expectedSql);
        this.tableName = tableName;
        this.existingColumnNames = existingColumnNames;
        this.targetSchema = targetSchema;
    }

    @Parameterized.Parameters
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] {
                {   // 00: default primary key case when additional column already existed
                        "table_name",
                        defaultColumnNamesWith(longCol().build().getColumnName()),
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
                {   // 01: make existing column a primary key (when it wasn't before)
                        "table_name",
                        defaultColumnNamesWith(longCol().build().getColumnName()),
                        tableMapOf(table().tableName("table_name")
                                .primaryKey(new HashSet<>(Arrays.asList(longCol().build().getColumnName())))
                                .columnMap(columnMapOf(longCol().primaryKey(true).build()))
                                .build()),
                        new String[] {
                                "DROP TABLE IF EXISTS temp_table_name;",
                                "CREATE TEMP TABLE temp_table_name AS SELECT _id, created, deleted, modified, long_column FROM table_name;",
                                "DROP TABLE IF EXISTS table_name;",
                                "CREATE TABLE table_name(_id INTEGER, created DATETIME DEFAULT(" + CURRENT_UTC_TIME + "), deleted INTEGER DEFAULT '0', modified DATETIME DEFAULT(" + CURRENT_UTC_TIME + "), long_column INTEGER PRIMARY KEY);",
                                "CREATE TRIGGER table_name_updated_trigger AFTER UPDATE ON table_name BEGIN UPDATE table_name SET modified=" + CURRENT_UTC_TIME + " WHERE long_column=NEW.long_column; END;",
                                "INSERT INTO table_name SELECT _id, created, deleted, modified, long_column FROM temp_table_name;",
                                "DROP TABLE IF EXISTS temp_table_name;"
                        }
                },
                {   // 02: make new column a primary key
                        "table_name",
                        defaultColumnNameSet(),
                        tableMapOf(table().tableName("table_name")
                                .primaryKey(new HashSet<>(Arrays.asList(longCol().build().getColumnName())))
                                .columnMap(columnMapOf(longCol().primaryKey(true).build()))
                                .build()),
                        new String[] {
                                "DROP TABLE IF EXISTS temp_table_name;",
                                "CREATE TEMP TABLE temp_table_name AS SELECT _id, created, deleted, modified FROM table_name;",
                                "DROP TABLE IF EXISTS table_name;",
                                "CREATE TABLE table_name(_id INTEGER, created DATETIME DEFAULT(" + CURRENT_UTC_TIME + "), deleted INTEGER DEFAULT '0', modified DATETIME DEFAULT(" + CURRENT_UTC_TIME + "), long_column INTEGER PRIMARY KEY);",
                                "CREATE TRIGGER table_name_updated_trigger AFTER UPDATE ON table_name BEGIN UPDATE table_name SET modified=" + CURRENT_UTC_TIME + " WHERE long_column=NEW.long_column; END;",
                                "INSERT INTO table_name SELECT _id, created, deleted, modified, null AS long_column FROM temp_table_name;",
                                "DROP TABLE IF EXISTS temp_table_name;"
                        }
                },
                {   // 03: non-default, non-composite primary key case with on conflict on an existing column
                        "table_name",
                        defaultColumnNamesWith(stringCol().build().getColumnName()),
                        tableMapOf(table().tableName("table_name")
                                .primaryKey(new HashSet<>(Arrays.asList(stringCol().build().getColumnName())))
                                .primaryKeyOnConflict("REPLACE")
                                .columnMap(columnMapOf(stringCol().primaryKey(true).build()))
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
                {   // 04: non-default, non-composite primary key case with on conflict on a new column
                        "table_name",
                        defaultColumnNameSet(),
                        tableMapOf(table().tableName("table_name")
                                .primaryKey(new HashSet<>(Arrays.asList(stringCol().build().getColumnName())))
                                .primaryKeyOnConflict("REPLACE")
                                .columnMap(columnMapOf(stringCol().primaryKey(true).build()))
                                .build()),
                        new String[] {
                                "DROP TABLE IF EXISTS temp_table_name;",
                                "CREATE TEMP TABLE temp_table_name AS SELECT _id, created, deleted, modified FROM table_name;",
                                "DROP TABLE IF EXISTS table_name;",
                                "CREATE TABLE table_name(_id INTEGER, created DATETIME DEFAULT(" + CURRENT_UTC_TIME + "), deleted INTEGER DEFAULT '0', modified DATETIME DEFAULT(" + CURRENT_UTC_TIME + "), string_column TEXT PRIMARY KEY ON CONFLICT REPLACE);",
                                "CREATE TRIGGER table_name_updated_trigger AFTER UPDATE ON table_name BEGIN UPDATE table_name SET modified=" + CURRENT_UTC_TIME + " WHERE string_column=NEW.string_column; END;",
                                "INSERT INTO table_name SELECT _id, created, deleted, modified, null AS string_column FROM temp_table_name;",
                                "DROP TABLE IF EXISTS temp_table_name;"
                        }
                },
                {   // 05: non-default, composite primary key case with no on-conflict new columns
                        "table_name",
                        defaultColumnNameSet(),
                        tableMapOf(table().tableName("table_name")
                                .primaryKey(new HashSet<>(Arrays.asList(stringCol().build().getColumnName(), "string_column_2")))
                                .columnMap(columnMapOf(stringCol().build(), stringCol().columnName("string_column_2").build())).build()),
                        new String[] {
                                "DROP TABLE IF EXISTS temp_table_name;",
                                "CREATE TEMP TABLE temp_table_name AS SELECT _id, created, deleted, modified FROM table_name;",
                                "DROP TABLE IF EXISTS table_name;",
                                "CREATE TABLE table_name(_id INTEGER, created DATETIME DEFAULT(" + CURRENT_UTC_TIME + "), deleted INTEGER DEFAULT '0', modified DATETIME DEFAULT(" + CURRENT_UTC_TIME + "), string_column TEXT, string_column_2 TEXT, PRIMARY KEY(string_column, string_column_2));",
                                "CREATE TRIGGER table_name_updated_trigger AFTER UPDATE ON table_name BEGIN UPDATE table_name SET modified=" + CURRENT_UTC_TIME + " WHERE string_column=NEW.string_column AND string_column_2=NEW.string_column_2; END;",
                                "INSERT INTO table_name SELECT _id, created, deleted, modified, null AS string_column, null AS string_column_2 FROM temp_table_name;",
                                "DROP TABLE IF EXISTS temp_table_name;"
                        }
                },
                {   // 06: non-default, composite primary key case with one of the columns existing and one not existing
                        "table_name",
                        defaultColumnNamesWith(stringCol().build().getColumnName()),
                        tableMapOf(table().tableName("table_name")
                                .primaryKey(new HashSet<>(Arrays.asList(stringCol().build().getColumnName(), "string_column_2")))
                                .columnMap(columnMapOf(stringCol().build(), stringCol().columnName("string_column_2").build())).build()),
                        new String[] {
                                "DROP TABLE IF EXISTS temp_table_name;",
                                "CREATE TEMP TABLE temp_table_name AS SELECT _id, created, deleted, modified, string_column FROM table_name;",
                                "DROP TABLE IF EXISTS table_name;",
                                "CREATE TABLE table_name(_id INTEGER, created DATETIME DEFAULT(" + CURRENT_UTC_TIME + "), deleted INTEGER DEFAULT '0', modified DATETIME DEFAULT(" + CURRENT_UTC_TIME + "), string_column TEXT, string_column_2 TEXT, PRIMARY KEY(string_column, string_column_2));",
                                "CREATE TRIGGER table_name_updated_trigger AFTER UPDATE ON table_name BEGIN UPDATE table_name SET modified=" + CURRENT_UTC_TIME + " WHERE string_column=NEW.string_column AND string_column_2=NEW.string_column_2; END;",
                                "INSERT INTO table_name SELECT _id, created, deleted, modified, string_column, null AS string_column_2 FROM temp_table_name;",
                                "DROP TABLE IF EXISTS temp_table_name;"
                        }
                },
                {   // 07: non-default, composite primary key case with both columns existing
                        "table_name",
                        defaultColumnNamesWith(stringCol().build().getColumnName(), "string_column_2"),
                        tableMapOf(table().tableName("table_name")
                                .primaryKey(new HashSet<>(Arrays.asList(stringCol().build().getColumnName(), "string_column_2")))
                                .columnMap(columnMapOf(stringCol().build(), stringCol().columnName("string_column_2").build())).build()),
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
                {   // 08: non-default, composite primary key case with both columns existing and on conflict
                        "table_name",
                        defaultColumnNamesWith(stringCol().build().getColumnName(), "string_column_2"),
                        tableMapOf(table().tableName("table_name")
                                .primaryKey(new HashSet<>(Arrays.asList(stringCol().build().getColumnName(), "string_column_2")))
                                .primaryKeyOnConflict("ROLLBACK")
                                .columnMap(columnMapOf(stringCol().build(), stringCol().columnName("string_column_2").build())).build()),
                        new String[] {
                                "DROP TABLE IF EXISTS temp_table_name;",
                                "CREATE TEMP TABLE temp_table_name AS SELECT _id, created, deleted, modified, string_column, string_column_2 FROM table_name;",
                                "DROP TABLE IF EXISTS table_name;",
                                "CREATE TABLE table_name(_id INTEGER, created DATETIME DEFAULT(STRFTIME('%Y-%m-%d %H:%M:%f', 'NOW')), deleted INTEGER DEFAULT '0', modified DATETIME DEFAULT(STRFTIME('%Y-%m-%d %H:%M:%f', 'NOW')), string_column TEXT, string_column_2 TEXT, PRIMARY KEY(string_column, string_column_2) ON CONFLICT ROLLBACK);",
                                "CREATE TRIGGER table_name_updated_trigger AFTER UPDATE ON table_name BEGIN UPDATE table_name SET modified=" + CURRENT_UTC_TIME + " WHERE string_column=NEW.string_column AND string_column_2=NEW.string_column_2; END;",
                                "INSERT INTO table_name SELECT _id, created, deleted, modified, string_column, string_column_2 FROM temp_table_name;",
                                "DROP TABLE IF EXISTS temp_table_name;"
                        }
                }
        });
    }

    @Before
    public void setUp() {
        if (generatorUnderTest == null) {
            generatorUnderTest = new UpdatePrimaryKeyGenerator(tableName, existingColumnNames, targetSchema);
        }
    }

    @Override
    protected QueryGenerator getGenerator() {
        return generatorUnderTest;
    }
}
