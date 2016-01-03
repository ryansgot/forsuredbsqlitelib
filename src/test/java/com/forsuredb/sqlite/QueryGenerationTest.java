package com.forsuredb.sqlite;

import com.forsuredb.migration.Migration;
import com.forsuredb.migration.QueryGenerator;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class QueryGenerationTest {

    private List<String> actualSqlOutput;

    private final String inputMigrationJson;
    private final List<String> expectedSqlOutput;

    public QueryGenerationTest(String inputMigrationJson, List<String> expectedSqlOutput) {
        this.inputMigrationJson = inputMigrationJson;
        this.expectedSqlOutput = expectedSqlOutput;
    }

    @Parameterized.Parameters
    public static Iterable<Object[]> data() throws IOException {
        return Arrays.asList(new Object[][] {
                // ALTER TABLE ADD COLUMN
                {
                        TestData.resourceText("alter_table_add_column_migration.json"),
                        Lists.newArrayList("ALTER TABLE user ADD COLUMN global_id INTEGER;")
                },
                // CREATE TABLE
                {
                        TestData.resourceText("create_table_migration.json"),
                        Lists.newArrayList("CREATE TABLE profile_info(_id INTEGER PRIMARY KEY, created DATETIME DEFAULT CURRENT_TIMESTAMP, deleted INTEGER DEFAULT 0, modified DATETIME DEFAULT CURRENT_TIMESTAMP);",
                                           "CREATE TRIGGER profile_info_updated_trigger AFTER UPDATE ON profile_info BEGIN UPDATE profile_info SET modified=CURRENT_TIMESTAMP WHERE _id=NEW._id; END;")
                },
                // ADD FOREIGN KEY
                {
                        TestData.resourceText("alter_table_add_foreign_key_migration.json"),
                        Lists.newArrayList("DROP TABLE IF EXISTS temp_profile_info;",
                                "CREATE TEMP TABLE temp_profile_info AS SELECT _id, created, deleted, modified, binary_data, email_address FROM profile_info;",
                                "DROP TABLE IF EXISTS profile_info;",
                                "CREATE TABLE profile_info(_id INTEGER PRIMARY KEY, created DATETIME DEFAULT CURRENT_TIMESTAMP, deleted INTEGER DEFAULT 0, modified DATETIME DEFAULT CURRENT_TIMESTAMP, user_id INTEGER, FOREIGN KEY(user_id) REFERENCES user(_id) ON UPDATE CASCADE ON DELETE CASCADE);",
                                "CREATE TRIGGER profile_info_updated_trigger AFTER UPDATE ON profile_info BEGIN UPDATE profile_info SET modified=CURRENT_TIMESTAMP WHERE _id=NEW._id; END;",
                                "ALTER TABLE profile_info ADD COLUMN binary_data BLOB;",
                                "ALTER TABLE profile_info ADD COLUMN email_address TEXT;",
                                "INSERT INTO profile_info SELECT _id, created, deleted, modified, null AS user_id, binary_data, email_address FROM temp_profile_info;",
                                "DROP TABLE IF EXISTS temp_profile_info;")
                }
        });
    }

    @Before
    public void setUp() {
        actualSqlOutput = generateOutputSql();
    }

    @Test
    public void shouldHaveCorrectNumberOfStatements() {
        assertEquals(expectedSqlOutput.size(), actualSqlOutput.size());
    }

    @Test
    public void shouldMatchExpectedSqlExactly() {
        assertArrayEquals(expectedSqlOutput.toArray(new String[expectedSqlOutput.size()]), actualSqlOutput.toArray(new String[actualSqlOutput.size()]));
    }

    private List<String> generateOutputSql() {
        List<Migration> migrations = new Gson().fromJson(inputMigrationJson, new TypeToken<List<Migration>>() {}.getType());
        List<String> retList = new ArrayList<>();
        for (Migration migration : migrations) {
            retList.addAll(QueryGeneratorFactory.getFor(migration).generate());
        }
        return retList;
    }
}
