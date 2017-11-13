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

import com.fsryan.forsuredb.migration.MigrationSet;
import com.fsryan.forsuredb.serialization.FSDbInfoGsonSerializer;
import com.fsryan.forsuredb.serialization.FSDbInfoSerializer;
import com.google.gson.Gson;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static com.fsryan.forsuredb.sqlitelib.SqlGenerator.CURRENT_UTC_TIME;
import static com.fsryan.forsuredb.sqlitelib.TestData.resourceText;
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
                        Arrays.asList("ALTER TABLE user ADD COLUMN global_id INTEGER;")
                },
                {   // 01 CREATE TABLE
                        resourceText("create_table_migration.json"),
                        Arrays.asList(
                                "CREATE TABLE profile_info(_id INTEGER PRIMARY KEY, created DATETIME DEFAULT(" + CURRENT_UTC_TIME + "), deleted INTEGER DEFAULT '0', modified DATETIME DEFAULT(" + CURRENT_UTC_TIME + "));",
                                "CREATE TRIGGER profile_info_updated_trigger AFTER UPDATE ON profile_info BEGIN UPDATE profile_info SET modified=" + CURRENT_UTC_TIME + " WHERE _id=NEW._id; END;")
                },
                {   // 02 ADD FOREIGN KEY; legacy foreign key
                        resourceText("alter_table_add_foreign_key_migration.json"),
                        Arrays.asList(
                                "DROP TABLE IF EXISTS temp_profile_info;",
                                "CREATE TEMP TABLE temp_profile_info AS SELECT _id, created, deleted, modified, binary_data, email_address FROM profile_info;",
                                "DROP TABLE IF EXISTS profile_info;",
                                "CREATE TABLE profile_info(_id INTEGER PRIMARY KEY, created DATETIME DEFAULT(" + CURRENT_UTC_TIME + "), deleted INTEGER DEFAULT '0', modified DATETIME DEFAULT(" + CURRENT_UTC_TIME + "), user_id INTEGER, FOREIGN KEY(user_id) REFERENCES user(_id) ON UPDATE CASCADE ON DELETE CASCADE);",
                                "CREATE TRIGGER profile_info_updated_trigger AFTER UPDATE ON profile_info BEGIN UPDATE profile_info SET modified=" + CURRENT_UTC_TIME + " WHERE _id=NEW._id; END;",
                                "ALTER TABLE profile_info ADD COLUMN binary_data BLOB;",
                                "ALTER TABLE profile_info ADD COLUMN email_address TEXT;",
                                "INSERT INTO profile_info SELECT _id, created, deleted, modified, null AS user_id, binary_data, email_address FROM temp_profile_info;",
                                "DROP TABLE IF EXISTS temp_profile_info;")
                },
                {   // 03 CREATE TABLE with unique column
                        resourceText("create_table_migration_with_unique_column.json"),
                        Arrays.asList(
                                "CREATE TABLE profile_info(_id INTEGER PRIMARY KEY, created DATETIME DEFAULT(" + CURRENT_UTC_TIME + "), deleted INTEGER DEFAULT '0', modified DATETIME DEFAULT(" + CURRENT_UTC_TIME + "), uuid TEXT UNIQUE);",
                                "CREATE TRIGGER profile_info_updated_trigger AFTER UPDATE ON profile_info BEGIN UPDATE profile_info SET modified=" + CURRENT_UTC_TIME + " WHERE _id=NEW._id; END;",
                                "CREATE UNIQUE INDEX IF NOT EXISTS profile_info_uuid ON profile_info(uuid);")
                },
                {   // 04 create two tables, one with a non unique index and unique index, the other with a foreign key to the unique index column; legacy foreign key
                        resourceText("create_two_tables_one_has_foreign_key_to_other.json"),
                        Arrays.asList(
                                "CREATE TABLE test_table(_id INTEGER PRIMARY KEY, created DATETIME DEFAULT(" + CURRENT_UTC_TIME + "), deleted INTEGER DEFAULT '0', modified DATETIME DEFAULT(" + CURRENT_UTC_TIME + "), unique_index_column TEXT UNIQUE);",
                                "CREATE TRIGGER test_table_updated_trigger AFTER UPDATE ON test_table BEGIN UPDATE test_table SET modified=" + CURRENT_UTC_TIME + " WHERE _id=NEW._id; END;",
                                "CREATE UNIQUE INDEX IF NOT EXISTS test_table_unique_index_column ON test_table(unique_index_column);",
                                "CREATE TABLE test_table2(_id INTEGER PRIMARY KEY, created DATETIME DEFAULT(STRFTIME('%Y-%m-%d %H:%M:%f', 'NOW')), deleted INTEGER DEFAULT '0', modified DATETIME DEFAULT(STRFTIME('%Y-%m-%d %H:%M:%f', 'NOW')), test_table_unique_index_column TEXT, FOREIGN KEY(test_table_unique_index_column) REFERENCES test_table(unique_index_column) ON UPDATE CASCADE ON DELETE CASCADE);",
                                "CREATE TRIGGER test_table2_updated_trigger AFTER UPDATE ON test_table2 BEGIN UPDATE test_table2 SET modified=" + CURRENT_UTC_TIME + " WHERE _id=NEW._id; END;",
                                "ALTER TABLE test_table ADD COLUMN non_unique_index_column TEXT;",
                                "CREATE INDEX IF NOT EXISTS test_table_non_unique_index_column ON test_table(non_unique_index_column);"
                        )
                },
                {   // 05 additional_data_table has foreign key to profile_info_table has foreign key to user_table; legacy foreign key
                        resourceText("three_table_zero_to_one_test.json"),
                        Arrays.asList(
                                "CREATE TABLE user(_id INTEGER PRIMARY KEY, created DATETIME DEFAULT(" + CURRENT_UTC_TIME + "), deleted INTEGER DEFAULT '0', modified DATETIME DEFAULT(" + CURRENT_UTC_TIME + "));",
                                "CREATE TRIGGER user_updated_trigger AFTER UPDATE ON user BEGIN UPDATE user SET modified=" + CURRENT_UTC_TIME + " WHERE _id=NEW._id; END;",
                                "CREATE TABLE profile_info(_id INTEGER PRIMARY KEY, created DATETIME DEFAULT(" + CURRENT_UTC_TIME + "), deleted INTEGER DEFAULT '0', modified DATETIME DEFAULT(" + CURRENT_UTC_TIME + "), user_id INTEGER, FOREIGN KEY(user_id) REFERENCES user(_id) ON UPDATE CASCADE ON DELETE CASCADE);",
                                "CREATE TRIGGER profile_info_updated_trigger AFTER UPDATE ON profile_info BEGIN UPDATE profile_info SET modified=" + CURRENT_UTC_TIME + " WHERE _id=NEW._id; END;",
                                "CREATE TABLE additional_data(_id INTEGER PRIMARY KEY, created DATETIME DEFAULT(STRFTIME('%Y-%m-%d %H:%M:%f', 'NOW')), deleted INTEGER DEFAULT '0', modified DATETIME DEFAULT(STRFTIME('%Y-%m-%d %H:%M:%f', 'NOW')), profile_info_id INTEGER, FOREIGN KEY(profile_info_id) REFERENCES profile_info(_id) ON UPDATE CASCADE ON DELETE CASCADE);",
                                "CREATE TRIGGER additional_data_updated_trigger AFTER UPDATE ON additional_data BEGIN UPDATE additional_data SET modified=" + CURRENT_UTC_TIME + " WHERE _id=NEW._id; END;",
                                "ALTER TABLE user ADD COLUMN app_rating REAL;",
                                "ALTER TABLE profile_info ADD COLUMN awesome INTEGER;",
                                "ALTER TABLE profile_info ADD COLUMN binary_data BLOB;",
                                "ALTER TABLE user ADD COLUMN competitor_app_rating REAL;",
                                "ALTER TABLE profile_info ADD COLUMN email_address TEXT;",
                                "ALTER TABLE user ADD COLUMN global_id INTEGER;",
                                "ALTER TABLE additional_data ADD COLUMN int_column INTEGER;",
                                "ALTER TABLE user ADD COLUMN login_count INTEGER;",
                                "ALTER TABLE additional_data ADD COLUMN long_column INTEGER;",
                                "ALTER TABLE additional_data ADD COLUMN string_column TEXT;"
                        )
                },
                {   // 06 same as 05, but with TableForeignKeyInfo instead of legacy foreign key
                        resourceText("three_table_zero_to_one_test_update_foreign_keys.json"),
                        Arrays.asList(
                                "CREATE TABLE user(_id INTEGER PRIMARY KEY, created DATETIME DEFAULT(" + CURRENT_UTC_TIME + "), deleted INTEGER DEFAULT '0', modified DATETIME DEFAULT(" + CURRENT_UTC_TIME + "));",
                                "CREATE TRIGGER user_updated_trigger AFTER UPDATE ON user BEGIN UPDATE user SET modified=" + CURRENT_UTC_TIME + " WHERE _id=NEW._id; END;",
                                "CREATE TABLE profile_info(_id INTEGER PRIMARY KEY, created DATETIME DEFAULT(" + CURRENT_UTC_TIME + "), deleted INTEGER DEFAULT '0', modified DATETIME DEFAULT(" + CURRENT_UTC_TIME + "), user_id INTEGER, FOREIGN KEY(user_id) REFERENCES user(_id) ON UPDATE CASCADE ON DELETE CASCADE);",
                                "CREATE TRIGGER profile_info_updated_trigger AFTER UPDATE ON profile_info BEGIN UPDATE profile_info SET modified=" + CURRENT_UTC_TIME + " WHERE _id=NEW._id; END;",
                                "CREATE TABLE additional_data(_id INTEGER PRIMARY KEY, created DATETIME DEFAULT(" + CURRENT_UTC_TIME + "), deleted INTEGER DEFAULT '0', modified DATETIME DEFAULT(" + CURRENT_UTC_TIME + "), profile_info_id INTEGER, FOREIGN KEY(profile_info_id) REFERENCES profile_info(_id));",
                                "CREATE TRIGGER additional_data_updated_trigger AFTER UPDATE ON additional_data BEGIN UPDATE additional_data SET modified=" + CURRENT_UTC_TIME + " WHERE _id=NEW._id; END;",
                                "ALTER TABLE user ADD COLUMN app_rating REAL;",
                                "ALTER TABLE profile_info ADD COLUMN awesome INTEGER;",
                                "ALTER TABLE profile_info ADD COLUMN binary_data BLOB;",
                                "ALTER TABLE user ADD COLUMN competitor_app_rating REAL;",
                                "ALTER TABLE profile_info ADD COLUMN email_address TEXT;",
                                "ALTER TABLE user ADD COLUMN global_id INTEGER;",
                                "ALTER TABLE additional_data ADD COLUMN int_column INTEGER;",
                                "ALTER TABLE user ADD COLUMN login_count INTEGER;",
                                "ALTER TABLE additional_data ADD COLUMN long_column INTEGER;",
                                "ALTER TABLE additional_data ADD COLUMN string_column TEXT;"
                        )
                },
        });
    }

    @Before
    public void setUp() {
        FSDbInfoSerializer gsonSerializer = new FSDbInfoGsonSerializer();
        MigrationSet migrationSet = gsonSerializer.deserializeMigrationSet(inputMigrationJson);
        actualSqlOutput = new SqlGenerator().generateMigrationSql(migrationSet, gsonSerializer);
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
            final String expected = expectedSqlOutput.get(i);
            final String actual = actualSqlOutput.get(i);
            final String message = "sql index: " + i + "\nexpected: " + expected + "\nactual:   " + actual + "\n";
            assertEquals(message, expected, actual);
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
