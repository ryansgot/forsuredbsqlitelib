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

import com.fsryan.forsuredb.api.migration.MigrationSet;
import com.google.common.collect.Lists;
import com.google.gson.Gson;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static com.fsryan.forsuredb.sqlitelib.TestData.resourceText;
import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
public class MigrationSqlQueryTest {

    private List<String> actualSqlOutput;

    private final String inputMigrationJson;
    private final List<String> expectedSqlOutput;

    public MigrationSqlQueryTest(String inputMigrationJson, List<String> expectedSqlOutput) {
        this.inputMigrationJson = inputMigrationJson;
        this.expectedSqlOutput = expectedSqlOutput;
    }

    @Parameterized.Parameters
    public static Iterable<Object[]> data() throws IOException {
        return Arrays.asList(new Object[][] {
                {   // 00 ALTER TABLE ADD COLUMN
                        resourceText("alter_table_add_column_migration.json"),
                        newArrayList("ALTER TABLE user ADD COLUMN global_id INTEGER;")
                },
                {   // 01 CREATE TABLE
                        resourceText("create_table_migration.json"),
                        newArrayList(
                                "CREATE TABLE profile_info(_id INTEGER PRIMARY KEY, created DATETIME DEFAULT(" + SqlGenerator.CURRENT_UTC_TIME + "), modified DATETIME DEFAULT(" + SqlGenerator.CURRENT_UTC_TIME + "), deleted INTEGER DEFAULT '0');",
                                "CREATE TRIGGER profile_info_updated_trigger AFTER UPDATE ON profile_info BEGIN UPDATE profile_info SET modified=" + SqlGenerator.CURRENT_UTC_TIME + " WHERE _id=NEW._id; END;")
                },
                {   // 02 ADD FOREIGN KEY
                        resourceText("alter_table_add_foreign_key_migration.json"),
                        newArrayList(
                                "DROP TABLE IF EXISTS temp_profile_info;",
                                "CREATE TEMP TABLE temp_profile_info AS SELECT _id, created, deleted, modified, binary_data, email_address FROM profile_info;",
                                "DROP TABLE IF EXISTS profile_info;",
                                "CREATE TABLE profile_info(_id INTEGER PRIMARY KEY, created DATETIME DEFAULT(" + SqlGenerator.CURRENT_UTC_TIME + "), modified DATETIME DEFAULT(" + SqlGenerator.CURRENT_UTC_TIME + "), deleted INTEGER DEFAULT '0', user_id INTEGER, FOREIGN KEY(user_id) REFERENCES user(_id) ON UPDATE CASCADE ON DELETE CASCADE);",
                                "CREATE TRIGGER profile_info_updated_trigger AFTER UPDATE ON profile_info BEGIN UPDATE profile_info SET modified=" + SqlGenerator.CURRENT_UTC_TIME + " WHERE _id=NEW._id; END;",
                                "ALTER TABLE profile_info ADD COLUMN binary_data BLOB;",
                                "ALTER TABLE profile_info ADD COLUMN email_address TEXT;",
                                "INSERT INTO profile_info SELECT _id, created, deleted, modified, null AS user_id, binary_data, email_address FROM temp_profile_info;",
                                "DROP TABLE IF EXISTS temp_profile_info;")
                },
                {   // 03 CREATE TABLE with unique column
                        resourceText("create_table_migration_with_unique_column.json"),
                        newArrayList(
                                "CREATE TABLE profile_info(_id INTEGER PRIMARY KEY, created DATETIME DEFAULT(" + SqlGenerator.CURRENT_UTC_TIME + "), modified DATETIME DEFAULT(" + SqlGenerator.CURRENT_UTC_TIME + "), deleted INTEGER DEFAULT '0', uuid TEXT UNIQUE);",
                                "CREATE TRIGGER profile_info_updated_trigger AFTER UPDATE ON profile_info BEGIN UPDATE profile_info SET modified=" + SqlGenerator.CURRENT_UTC_TIME + " WHERE _id=NEW._id; END;",
                                "CREATE UNIQUE INDEX IF NOT EXISTS profile_info_uuid ON profile_info(uuid);")
                },
                {   // 04 create two tables, one with a non unique index and unique index, the other with a foreign key to the unique index column
                        resourceText("create_two_tables_one_has_foreign_key_to_other.json"),
                        Lists.newArrayList(
                                "CREATE TABLE test_table(_id INTEGER PRIMARY KEY, created DATETIME DEFAULT(" + SqlGenerator.CURRENT_UTC_TIME + "), modified DATETIME DEFAULT(" + SqlGenerator.CURRENT_UTC_TIME + "), deleted INTEGER DEFAULT '0', unique_index_column TEXT UNIQUE);",
                                "CREATE TRIGGER test_table_updated_trigger AFTER UPDATE ON test_table BEGIN UPDATE test_table SET modified=" + SqlGenerator.CURRENT_UTC_TIME + " WHERE _id=NEW._id; END;",
                                "CREATE UNIQUE INDEX IF NOT EXISTS test_table_unique_index_column ON test_table(unique_index_column);",
                                "CREATE TABLE test_table2(_id INTEGER PRIMARY KEY, created DATETIME DEFAULT(" + SqlGenerator.CURRENT_UTC_TIME + "), modified DATETIME DEFAULT(" + SqlGenerator.CURRENT_UTC_TIME + "), deleted INTEGER DEFAULT '0');",
                                "CREATE TRIGGER test_table2_updated_trigger AFTER UPDATE ON test_table2 BEGIN UPDATE test_table2 SET modified=" + SqlGenerator.CURRENT_UTC_TIME + " WHERE _id=NEW._id; END;",
                                "ALTER TABLE test_table ADD COLUMN non_unique_index_column TEXT;",
                                "CREATE INDEX IF NOT EXISTS test_table_non_unique_index_column ON test_table(non_unique_index_column);",
                                "DROP TABLE IF EXISTS temp_test_table2;",
                                "CREATE TEMP TABLE temp_test_table2 AS SELECT _id, created, deleted, modified FROM test_table2;",
                                "DROP TABLE IF EXISTS test_table2;",
                                "CREATE TABLE test_table2(_id INTEGER PRIMARY KEY, created DATETIME DEFAULT(" + SqlGenerator.CURRENT_UTC_TIME + "), modified DATETIME DEFAULT(" + SqlGenerator.CURRENT_UTC_TIME + "), deleted INTEGER DEFAULT '0', test_table_unique_index_column TEXT, FOREIGN KEY(test_table_unique_index_column) REFERENCES test_table(unique_index_column) ON UPDATE CASCADE ON DELETE CASCADE);",
                                "CREATE TRIGGER test_table2_updated_trigger AFTER UPDATE ON test_table2 BEGIN UPDATE test_table2 SET modified=" + SqlGenerator.CURRENT_UTC_TIME + " WHERE _id=NEW._id; END;",
                                "INSERT INTO test_table2 SELECT _id, created, deleted, modified, null AS test_table_unique_index_column FROM temp_test_table2;",
                                "DROP TABLE IF EXISTS temp_test_table2;"
                        )
                }
        });
    }

    @Before
    public void setUp() {
        MigrationSet migrationSet = new Gson().fromJson(inputMigrationJson, MigrationSet.class);
        actualSqlOutput = new SqlGenerator().generateMigrationSql(migrationSet);
    }

    @Test
    public void shouldHaveCorrectNumberOfStatements() {
        if (expectedSqlOutput.size() != actualSqlOutput.size()) {
            fail(formatFailureMessage("expected count: " + expectedSqlOutput.size() + "; actual count = " + actualSqlOutput.size()));
        }
    }

    @Test
    public void shouldMatchExpectedSqlExactly() {
        for (int i = 0; i < expectedSqlOutput.size(); i++) {
            assertEquals("sql index: " + i, expectedSqlOutput.get(i), actualSqlOutput.get(i));
        }
    }

    private String formatFailureMessage(String openingLine) {
        StringBuilder buf = new StringBuilder(openingLine).append("\nexpected:");
        for (String sql : expectedSqlOutput) {
            buf.append("\n").append(sql);
        }
        buf.append("\nactual:");
        for (String sql : actualSqlOutput) {
            buf.append("\n").append(sql);
        }
        return buf.toString();
    }
}
