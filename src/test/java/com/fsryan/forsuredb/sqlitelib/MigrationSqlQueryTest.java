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

import static org.junit.Assert.assertEquals;

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
                // 0 ALTER TABLE ADD COLUMN
                {
                        TestData.resourceText("alter_table_add_column_migration.json"),
                        Lists.newArrayList("ALTER TABLE user ADD COLUMN global_id INTEGER;")
                },
                // 1 CREATE TABLE
                {
                        TestData.resourceText("create_table_migration.json"),
                        Lists.newArrayList("CREATE TABLE profile_info(_id INTEGER PRIMARY KEY, created DATETIME DEFAULT CURRENT_TIMESTAMP, modified DATETIME DEFAULT CURRENT_TIMESTAMP, deleted INTEGER DEFAULT 0);",
                                           "CREATE TRIGGER profile_info_updated_trigger AFTER UPDATE ON profile_info BEGIN UPDATE profile_info SET modified=CURRENT_TIMESTAMP WHERE _id=NEW._id; END;")
                },
                // 2 ADD FOREIGN KEY
                {
                        TestData.resourceText("alter_table_add_foreign_key_migration.json"),
                        Lists.newArrayList("DROP TABLE IF EXISTS temp_profile_info;",
                                "CREATE TEMP TABLE temp_profile_info AS SELECT _id, created, deleted, modified, binary_data, email_address FROM profile_info;",
                                "DROP TABLE IF EXISTS profile_info;",
                                "CREATE TABLE profile_info(_id INTEGER PRIMARY KEY, created DATETIME DEFAULT CURRENT_TIMESTAMP, modified DATETIME DEFAULT CURRENT_TIMESTAMP, deleted INTEGER DEFAULT 0, user_id INTEGER, FOREIGN KEY(user_id) REFERENCES user(_id) ON UPDATE CASCADE ON DELETE CASCADE);",
                                "CREATE TRIGGER profile_info_updated_trigger AFTER UPDATE ON profile_info BEGIN UPDATE profile_info SET modified=CURRENT_TIMESTAMP WHERE _id=NEW._id; END;",
                                "ALTER TABLE profile_info ADD COLUMN binary_data BLOB;",
                                "ALTER TABLE profile_info ADD COLUMN email_address TEXT;",
                                "INSERT INTO profile_info SELECT _id, created, deleted, modified, null AS user_id, binary_data, email_address FROM temp_profile_info;",
                                "DROP TABLE IF EXISTS temp_profile_info;")
                },
                // 3 CREATE TABLE with unique column
                {
                        TestData.resourceText("create_table_migration_with_unique_column.json"),
                        Lists.newArrayList("CREATE TABLE profile_info(_id INTEGER PRIMARY KEY, created DATETIME DEFAULT CURRENT_TIMESTAMP, modified DATETIME DEFAULT CURRENT_TIMESTAMP, deleted INTEGER DEFAULT 0, uuid TEXT UNIQUE);",
                                "CREATE TRIGGER profile_info_updated_trigger AFTER UPDATE ON profile_info BEGIN UPDATE profile_info SET modified=CURRENT_TIMESTAMP WHERE _id=NEW._id; END;",
                                "CREATE UNIQUE INDEX profile_info_uuid ON profile_info(uuid);")
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
        assertEquals(expectedSqlOutput.size(), actualSqlOutput.size());
    }

    @Test
    public void shouldMatchExpectedSqlExactly() {
        for (int i = 0; i < expectedSqlOutput.size(); i++) {
            assertEquals("sql index: " + i, expectedSqlOutput.get(i), actualSqlOutput.get(i));
        }
    }
}