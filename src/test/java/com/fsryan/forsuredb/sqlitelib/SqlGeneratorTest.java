package com.fsryan.forsuredb.sqlitelib;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import static com.fsryan.forsuredb.sqlitelib.CollectionUtil.stringMapOf;
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
                            stringMapOf(),
                            EMPTY_SQL
                    },
                    {   // 01: empty input table name
                            "",
                            stringMapOf("col1", "val1"),
                            EMPTY_SQL
                    },
                    {   // 02: null input map
                            "",
                            null,
                            EMPTY_SQL
                    },
                    {   // 03: null input table name
                            null,
                            stringMapOf("col1", "val1"),
                            EMPTY_SQL
                    },
                    {   // 04: null input table name
                            null,
                            stringMapOf("col1", "val1"),
                            EMPTY_SQL
                    },
                    {   // 05: valid args, one column and one value
                            TABLE_NAME,
                            stringMapOf("col1", "val1"),
                            "INSERT INTO test_table (col1) VALUES ('val1');"
                    },
                    {   // 06: valid args, two columns and two values
                            TABLE_NAME,
                            stringMapOf("col2", "val2", "col1", "val1"),
                            "INSERT INTO test_table (col2, col1) VALUES ('val2', 'val1');"
                    },
                    {   // 07: valid args, attempt to insert an _id
                            TABLE_NAME,
                            stringMapOf("_id", "12345", "col2", "val2", "col1", "val1"),
                            "INSERT INTO test_table (col2, col1) VALUES ('val2', 'val1');"
                    },
                    {   // 08: valid args, attempt to insert modified
                            TABLE_NAME,
                            stringMapOf("modified", new Date().toString(), "col2", "val2", "col1", "val1"),
                            "INSERT INTO test_table (col2, col1) VALUES ('val2', 'val1');"
                    },
                    {   // 09: valid args, attempt to insert created
                            TABLE_NAME,
                            stringMapOf("created", new Date().toString(), "col2", "val2", "col1", "val1"),
                            "INSERT INTO test_table (col2, col1) VALUES ('val2', 'val1');"
                    },
                    {   // 10: valid args, attempt to insert _id, created, modified
                            TABLE_NAME,
                            stringMapOf("_id", "12345",
                                    "created", new Date().toString(),
                                    "modified", new Date().toString(),
                                    "col1", "val1",
                                    "col2", "val2"),
                            "INSERT INTO test_table (col2, col1) VALUES ('val2', 'val1');"
                    }
            });
        }

        @Before
        public void setUp() {
            sqlGenerator = new SqlGenerator();
        }

        @Test
        public void shouldOutputCorrectSql() {
            assertEquals(expectedOutputSql, sqlGenerator.newSingleRowInsertionSql(tableName, inputColumnValueMap));
        }

        @Test
        public void outputShouldBeAtLeastLength1() {
            assertTrue(0 < sqlGenerator.newSingleRowInsertionSql(tableName, inputColumnValueMap).length());
        }

        @Test
        public void outputShouldEndInSemicolon() {
            assertTrue(sqlGenerator.newSingleRowInsertionSql(tableName, inputColumnValueMap).endsWith(";"));
        }
    }
}
