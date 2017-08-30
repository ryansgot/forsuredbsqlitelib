package com.fsryan.forsuredb.sqlitelib;

import com.fsryan.forsuredb.api.FSOrdering;
import com.fsryan.forsuredb.api.OrderBy;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.*;

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
        private final String[] expectedOutputSqlAlternatives;

        private SqlGenerator sqlGenerator;

        public InsertionQueryGeneration(String tableName, Map<String, String> inputColumnValueMap, String[] expectedOutputSqlAlternatives) {
            this.tableName = tableName;
            this.inputColumnValueMap = inputColumnValueMap;
            this.expectedOutputSqlAlternatives = expectedOutputSqlAlternatives;
        }

        @Parameterized.Parameters
        public static Iterable<Object[]> data() {
            return Arrays.asList(new Object[][] {
                    {   // 00: empty input map
                            TABLE_NAME,
                            stringMapOf(),
                            new String[] {EMPTY_SQL}
                    },
                    {   // 01: empty input table name
                            "",
                            stringMapOf("col1", "val1"),
                            new String[] {EMPTY_SQL}
                    },
                    {   // 02: null input map
                            "",
                            null,
                            new String[] {EMPTY_SQL}
                    },
                    {   // 03: null input table name
                            null,
                            stringMapOf("col1", "val1"),
                            new String[] {EMPTY_SQL}
                    },
                    {   // 04: null input table name
                            null,
                            stringMapOf("col1", "val1"),
                            new String[] {EMPTY_SQL}
                    },
                    {   // 05: valid args, one column and one value
                            TABLE_NAME,
                            stringMapOf("col1", "val1"),
                            new String[] {"INSERT INTO test_table (col1) VALUES ('val1');"}
                    },
                    {   // 06: valid args, two columns and two values
                            TABLE_NAME,
                            stringMapOf("col2", "val2", "col1", "val1"),
                            new String[] {
                                    "INSERT INTO test_table (col2, col1) VALUES ('val2', 'val1');",
                                    "INSERT INTO test_table (col1, col2) VALUES ('val1', 'val2');"
                            }
                    },
                    {   // 07: valid args, attempt to insert an _id
                            TABLE_NAME,
                            stringMapOf("_id", "12345", "col2", "val2", "col1", "val1"),
                            new String[] {
                                    "INSERT INTO test_table (col2, col1) VALUES ('val2', 'val1');",
                                    "INSERT INTO test_table (col1, col2) VALUES ('val1', 'val2');"
                            }
                    },
                    {   // 08: valid args, attempt to insert modified
                            TABLE_NAME,
                            stringMapOf("modified", new Date().toString(), "col2", "val2", "col1", "val1"),
                            new String[] {
                                    "INSERT INTO test_table (col2, col1) VALUES ('val2', 'val1');",
                                    "INSERT INTO test_table (col1, col2) VALUES ('val1', 'val2');"
                            }
                    },
                    {   // 09: valid args, attempt to insert created
                            TABLE_NAME,
                            stringMapOf("created", new Date().toString(), "col2", "val2", "col1", "val1"),
                            new String[] {
                                    "INSERT INTO test_table (col2, col1) VALUES ('val2', 'val1');",
                                    "INSERT INTO test_table (col1, col2) VALUES ('val1', 'val2');"
                            }
                    },
                    {   // 10: valid args, attempt to insert _id, created, modified
                            TABLE_NAME,
                            stringMapOf("_id", "12345", "created", new Date().toString(), "modified", new Date().toString(), "col1", "val1", "col2", "val2"),
                            new String[] {
                                    "INSERT INTO test_table (col2, col1) VALUES ('val2', 'val1');",
                                    "INSERT INTO test_table (col1, col2) VALUES ('val1', 'val2');"
                            }
                    }
            });
        }

        @Before
        public void setUp() {
            sqlGenerator = new SqlGenerator();
        }

        @Test
        public void shouldOutputCorrectSql() {
            boolean matchedAnAlternative = false;
            final String actual = sqlGenerator.newSingleRowInsertionSql(tableName, inputColumnValueMap);
            for (String alternativeSql : expectedOutputSqlAlternatives) {
                if (alternativeSql.equals(actual)) {
                    matchedAnAlternative = true;
                    break;
                }
            }

            final String message = "no valid SQL generated;\n" +
                    "generated: " + actual + "\n" +
                    "possible valid values: " + Arrays.toString(expectedOutputSqlAlternatives);
            assertTrue(message, matchedAnAlternative);
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

    @RunWith(Parameterized.class)
    public static class ExpressOrdering {

        private final List<FSOrdering> input;
        private final String expectedOutput;

        public ExpressOrdering(List<FSOrdering> input, String expectedSql) {
            this.input = input;
            this.expectedOutput = expectedSql;
        }

        @Parameterized.Parameters
        public static Iterable<Object[]> data() {
            return Arrays.asList(new Object[][] {
                    {   // 00: number greater than OrderBy.ORDER_ASC treated as ascending
                            Arrays.asList(
                                    new FSOrdering("table", "column1", OrderBy.ORDER_ASC + 1)
                            ),
                            " ORDER BY table.column1 ASC"
                    },
                    {   // 01: number lower than OrderBy.ORDER_DESC treated as descending
                            Arrays.asList(
                                    new FSOrdering("table", "column1", OrderBy.ORDER_DESC - 1)
                            ),
                            " ORDER BY table.column1 DESC"
                    },
                    {   // 02: OrderBy.ORDER_ASC treated as ASC
                            Arrays.asList(
                                    new FSOrdering("table", "column1", OrderBy.ORDER_ASC)
                            ),
                            " ORDER BY table.column1 ASC"
                    },
                    {   // 03: OrderBy.ORDER_DESC treated as DESC
                            Arrays.asList(
                                    new FSOrdering("table", "column1", OrderBy.ORDER_DESC)
                            ),
                            " ORDER BY table.column1 DESC"
                    },
                    {   // 04: Two different ascending on same table
                            Arrays.asList(
                                    new FSOrdering("table", "column1", OrderBy.ORDER_ASC),
                                    new FSOrdering("table", "column2", OrderBy.ORDER_ASC)
                            ),
                            " ORDER BY table.column1 ASC, table.column2 ASC"
                    },
                    {   // 05: Two different descending on same table
                            Arrays.asList(
                                    new FSOrdering("table", "column1", OrderBy.ORDER_DESC),
                                    new FSOrdering("table", "column2", OrderBy.ORDER_DESC)
                            ),
                            " ORDER BY table.column1 DESC, table.column2 DESC"
                    },
                    {   // 06: ASC, then DESC for different columns
                            Arrays.asList(
                                    new FSOrdering("table", "column1", OrderBy.ORDER_ASC),
                                    new FSOrdering("table", "column2", OrderBy.ORDER_DESC)
                            ),
                            " ORDER BY table.column1 ASC, table.column2 DESC"
                    },
                    {   // 07: ASC, then DESC for different columns of different tables same column name
                            Arrays.asList(
                                    new FSOrdering("table", "column1", OrderBy.ORDER_ASC),
                                    new FSOrdering("table2", "column1", OrderBy.ORDER_DESC)
                            ),
                            " ORDER BY table.column1 ASC, table2.column1 DESC"
                    },
                    {   // 08: Empty list returns empty string
                            new ArrayList<FSOrdering>(0),
                            ""
                    },
                    {   // 09: null returns empty string
                            null,
                            ""
                    }
            });
        }

        @Test
        public void should() {
            assertEquals(expectedOutput, new SqlGenerator().expressOrdering(input));
        }
    }
}
