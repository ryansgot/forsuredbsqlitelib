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
import com.fsryan.forsuredb.info.TableForeignKeyInfo;
import com.fsryan.forsuredb.info.TableInfo;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static com.fsryan.forsuredb.sqlitelib.CollectionUtil.stringMapOf;
import static com.fsryan.forsuredb.sqlitelib.TestData.*;

@RunWith(Parameterized.class)
public class UpdateForeignKeysGeneratorTest extends BaseSQLiteGeneratorTest {

    private UpdateForeignKeysGenerator generatorUnderTest;

    private final String tableName;
    Set<TableForeignKeyInfo> currentForeignKeys;
    Set<String> currentColumns;
    private final Map<String, TableInfo> targetSchema;

    public UpdateForeignKeysGeneratorTest(String tableName,
                                          Set<TableForeignKeyInfo> currentForeignKeys,
                                          Set<String> currentColumns,
                                          Map<String, TableInfo> targetSchema,
                                          String... expectedSql) {
        super (expectedSql);
        this.tableName = tableName;
        this.currentForeignKeys = currentForeignKeys;
        this.currentColumns = currentColumns;
        this.targetSchema = targetSchema;
    }

    @Parameterized.Parameters
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] {
                {   // 00: add a composite foreign key
                        "referencing_table",
                        Collections.<TableForeignKeyInfo>emptySet(),
                        defaultColumnNameSet(),
                        tableMapOf(
                                table().tableName("referenced_table")
                                        .qualifiedClassName(UpdateForeignKeysGeneratorTest.class.getName())
                                        .primaryKey(setOf("referenced_int_column", "referenced_long_column"))
                                        .columnMap(columnMapOf(
                                                intCol().columnName("referenced_int_column").build(),
                                                longCol().columnName("referenced_long_column").build())
                                        )
                                        .build(),
                                table().tableName("referencing_table")
                                        .foreignKeys(setOf(
                                                TableForeignKeyInfo.builder()
                                                        .foreignTableName("referenced_table")
                                                        .foreignTableApiClassName(UpdateForeignKeysGeneratorTest.class.getName())
                                                        .localToForeignColumnMap(stringMapOf(
                                                                "referencing_int_column", "referenced_int_column",
                                                                "referencing_long_column", "referenced_long_column"
                                                        )).updateChangeAction("CASCADE")
                                                        .deleteChangeAction("CASCADE")
                                                        .build())
                                        )
                                        .primaryKey(setOf("_id"))
                                        .columnMap(columnMapOf(
                                                intCol().columnName("referencing_int_column").build(),
                                                longCol().columnName("referencing_long_column").build())
                                        )
                                        .build()
                                ),
                        new String[] {
                                "DROP TABLE IF EXISTS temp_referencing_table;",
                                "CREATE TEMP TABLE temp_referencing_table AS SELECT _id, created, deleted, modified FROM referencing_table;",
                                "DROP TABLE IF EXISTS referencing_table;",
                                "CREATE TABLE referencing_table(_id INTEGER PRIMARY KEY, created DATETIME DEFAULT(STRFTIME('%Y-%m-%d %H:%M:%f', 'NOW')), deleted INTEGER DEFAULT '0', modified DATETIME DEFAULT(STRFTIME('%Y-%m-%d %H:%M:%f', 'NOW')), referencing_int_column INTEGER, referencing_long_column INTEGER, FOREIGN KEY(referencing_int_column, referencing_long_column) REFERENCES referenced_table(referenced_int_column, referenced_long_column) ON UPDATE CASCADE ON DELETE CASCADE);",
                                "CREATE TRIGGER referencing_table_updated_trigger AFTER UPDATE ON referencing_table BEGIN UPDATE referencing_table SET modified=STRFTIME('%Y-%m-%d %H:%M:%f', 'NOW') WHERE _id=NEW._id; END;",
                                "INSERT INTO referencing_table SELECT _id, created, deleted, modified, null AS referencing_int_column, null AS referencing_long_column FROM temp_referencing_table;",
                                "DROP TABLE IF EXISTS temp_referencing_table;"
                        }
                },
        });
    }

    @Before
    public void setUp() {
        generatorUnderTest = new UpdateForeignKeysGenerator(tableName, currentForeignKeys, currentColumns, targetSchema);
    }

    @Override
    protected QueryGenerator getGenerator() {
        return generatorUnderTest;
    }
}
