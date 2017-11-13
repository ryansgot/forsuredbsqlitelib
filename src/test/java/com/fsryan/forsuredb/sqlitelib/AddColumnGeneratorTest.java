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
import com.fsryan.forsuredb.info.ColumnInfo;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;

import static com.fsryan.forsuredb.sqlitelib.TestData.*;

@RunWith(Parameterized.class)
public class AddColumnGeneratorTest extends BaseSQLiteGeneratorTest {

    private AddColumnGenerator generatorUnderTest;

    private ColumnInfo column;

    public AddColumnGeneratorTest(ColumnInfo column, String... expectedSql) {
        super(expectedSql);
        this.column = column;
    }

    @Parameterized.Parameters
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {   // 00: add a normal column
                        stringCol().build(),
                        new String[] {
                                "ALTER TABLE " + TABLE_NAME + " ADD COLUMN string_column TEXT;"
                        }
                },
                {   // 01: add a date column that has CURRENT_TIMESTAMP magic string as its time set
                        dateCol().defaultValue("CURRENT_TIMESTAMP").build(),
                        new String[] {
                                "ALTER TABLE " + TABLE_NAME + " ADD COLUMN date_column DATETIME DEFAULT(" + SqlGenerator.CURRENT_UTC_TIME + ");"
                        }
                },
                {   // 02: add a column that has a default set that is not CURRENT_TIMESTAMP
                        dateCol().defaultValue("2000-01-01 00:00:00.000").build(),
                        new String[] {
                                "ALTER TABLE " + TABLE_NAME + " ADD COLUMN date_column DATETIME DEFAULT '2000-01-01 00:00:00.000';"
                        }
                },
                {   // 03: add an integer column that has a default set
                        intCol().defaultValue("10").build(),
                        new String[] {
                                "ALTER TABLE " + TABLE_NAME + " ADD COLUMN int_column INTEGER DEFAULT '10';"
                        }
                },
                {   // 04: add column that is an index
                        intCol().index(true).build(),
                        new String[] {
                                "ALTER TABLE " + TABLE_NAME + " ADD COLUMN int_column INTEGER;",
                                "CREATE INDEX IF NOT EXISTS " + TABLE_NAME + "_int_column ON " + TABLE_NAME + "(int_column);"
                        }
                },
                {   // 05: add column that is an index and has a default value
                        intCol().defaultValue("10").index(true).build(),
                        new String[] {
                                "ALTER TABLE " + TABLE_NAME + " ADD COLUMN int_column INTEGER DEFAULT '10';",
                                "CREATE INDEX IF NOT EXISTS " + TABLE_NAME + "_int_column ON " + TABLE_NAME + "(int_column);"
                        }
                }
        });
    }

    @Override
    protected QueryGenerator getGenerator() {
        return generatorUnderTest;
    }

    @Before
    public void setUp() {
        generatorUnderTest = new AddColumnGenerator(TABLE_NAME, column);
    }
}
