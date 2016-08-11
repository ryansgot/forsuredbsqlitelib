package com.fsryan.forsuredb.sqlitelib;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import static com.fsryan.forsuredb.sqlitelib.SqlGenerator.EMPTY_SQL;
import static com.fsryan.forsuredb.sqlitelib.TestData.TABLE_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SqlGeneratorTest {

    @RunWith(Parameterized.class)
    public static class InsertionQueryGeneration {

        private final String tableName;
        private final Map<String, String> inputColumnValueMap;
        private final String expectedOutputSql;

        private SqlGenerator sqlGenerator;

        public InsertionQueryGeneration(String tableName, Map<String, String> inputColumnValueMap, String expectedOutputSql) {
            this.tableName = tableName;
            this.inputColumnValueMap = inputColumnValueMap;
            this.expectedOutputSql = expectedOutputSql;
        }

        @Parameterized.Parameters
        public static Iterable<Object[]> data() {
            return Arrays.asList(new Object[][] {
                    {   // 00: empty input map
                            TABLE_NAME,
                            new ImmutableMap.Builder<String, String>()
                                    .build(),
                            EMPTY_SQL
                    },
                    {   // 01: empty input table name
                            "",
                            new ImmutableMap.Builder<String, String>()
                                    .put("col1", "val1")
                                    .build(),
                            EMPTY_SQL
                    },
                    {   // 02: null input map
                            "",
                            null,
                            EMPTY_SQL
                    },
                    {   // 03: null input table name
                            null,
                            new ImmutableMap.Builder<String, String>()
                                    .put("col1", "val1")
                                    .build(),
                            EMPTY_SQL
                    },
                    {   // 04: null input table name
                            null,
                            new ImmutableMap.Builder<String, String>()
                                    .put("col1", "val1")
                                    .build(),
                            EMPTY_SQL
                    },
                    {   // 05: valid args, one column and one value
                            TABLE_NAME,
                            new ImmutableMap.Builder<String, String>()
                                    .put("col1", "val1")
                                    .build(),
                            "INSERT INTO test_table (col1) VALUES ('val1');"
                    },
                    {   // 06: valid args, two columns and two values
                            TABLE_NAME,
                            new ImmutableMap.Builder<String, String>()
                                    .put("col1", "val1")
                                    .put("col2", "val2")
                                    .build(),
                            "INSERT INTO test_table (col1, col2) VALUES ('val1', 'val2');"
                    },
                    {   // 07: valid args, attempt to insert an _id
                            TABLE_NAME,
                            new ImmutableMap.Builder<String, String>()
                                    .put("_id", "12345")
                                    .put("col1", "val1")
                                    .put("col2", "val2")
                                    .build(),
                            "INSERT INTO test_table (col1, col2) VALUES ('val1', 'val2');"
                    },
                    {   // 08: valid args, attempt to insert modified
                            TABLE_NAME,
                            new ImmutableMap.Builder<String, String>()
                                    .put("modified", new Date().toString())
                                    .put("col1", "val1")
                                    .put("col2", "val2")
                                    .build(),
                            "INSERT INTO test_table (col1, col2) VALUES ('val1', 'val2');"
                    },
                    {   // 09: valid args, attempt to insert created
                            TABLE_NAME,
                            new ImmutableMap.Builder<String, String>()
                                    .put("created", new Date().toString())
                                    .put("col1", "val1")
                                    .put("col2", "val2")
                                    .build(),
                            "INSERT INTO test_table (col1, col2) VALUES ('val1', 'val2');"
                    },
                    {   // 10: valid args, attempt to insert _id, created, modified
                            TABLE_NAME,
                            new ImmutableMap.Builder<String, String>()
                                    .put("_id", "12345")
                                    .put("created", new Date().toString())
                                    .put("modified", new Date().toString())
                                    .put("col1", "val1")
                                    .put("col2", "val2")
                                    .build(),
                            "INSERT INTO test_table (col1, col2) VALUES ('val1', 'val2');"
                    }
            });
        }

        @Before
        public void setUp() {
            sqlGenerator = new SqlGenerator();
        }

        @Test
        public void shouldOutputCorrectSql() {
            assertEquals(expectedOutputSql, sqlGenerator.singleRowInsertionSql(tableName, inputColumnValueMap));
        }

        @Test
        public void outputShouldBeAtLeastLength1() {
            assertTrue(0 < sqlGenerator.singleRowInsertionSql(tableName, inputColumnValueMap).length());
        }

        @Test
        public void outputShouldEndInSemicolon() {
            assertTrue(sqlGenerator.singleRowInsertionSql(tableName, inputColumnValueMap).endsWith(";"));
        }
    }
}
