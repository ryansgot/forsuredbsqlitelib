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
import java.util.Map;

import static com.fsryan.forsuredb.sqlitelib.SqlGenerator.CURRENT_UTC_TIME;
import static com.fsryan.forsuredb.sqlitelib.TestData.*;

@RunWith(Parameterized.class)
public class ChangeDefaultValueGeneratorTest extends BaseSQLiteGeneratorTest {

    private ChangeDefaultValueGenerator generatorUnderTest;

    private final String tableName;
    private final Map<String, TableInfo> targetSchema;

    public ChangeDefaultValueGeneratorTest(String tableName, Map<String, TableInfo> targetSchema, String... expectedSql) {
        super (expectedSql);
        this.tableName = tableName;
        this.targetSchema = targetSchema;
    }

    @Parameterized.Parameters
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] {
                {   // 00: changes the default value of a column
                        "table_name",
                        tableMapOf(table().tableName("table_name")
                                .columnMap(columnMapOf(longCol().defaultValue("12").build()))
                                .build()),
                        new String[] {
                                "DROP TABLE IF EXISTS temp_table_name;",
                                "CREATE TEMP TABLE temp_table_name AS SELECT _id, created, deleted, modified, long_column FROM table_name;",
                                "DROP TABLE IF EXISTS table_name;",
                                "CREATE TABLE table_name(_id INTEGER PRIMARY KEY, created DATETIME DEFAULT(" + CURRENT_UTC_TIME + "), deleted INTEGER DEFAULT '0', modified DATETIME DEFAULT(" + CURRENT_UTC_TIME + "));",
                                "CREATE TRIGGER table_name_updated_trigger AFTER UPDATE ON table_name BEGIN UPDATE table_name SET modified=" + CURRENT_UTC_TIME + " WHERE _id=NEW._id; END;",
                                "ALTER TABLE table_name ADD COLUMN long_column INTEGER DEFAULT '12';",
                                "INSERT INTO table_name SELECT _id, created, deleted, modified, long_column FROM temp_table_name;",
                                "DROP TABLE IF EXISTS temp_table_name;"
                        }
                }
        });
    }

    @Before
    public void setUp() {
        if (generatorUnderTest == null) {
            generatorUnderTest = new ChangeDefaultValueGenerator(tableName, targetSchema);
        }
    }

    @Override
    protected QueryGenerator getGenerator() {
        return generatorUnderTest;
    }
}
