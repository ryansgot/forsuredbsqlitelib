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

import static com.fsryan.forsuredb.sqlitelib.TestData.TABLE_NAME;

@RunWith(Parameterized.class)
public class AddUniqueColumnGeneratorTest extends BaseSQLiteGeneratorTest {

    private AddUniqueColumnGenerator generatorUnderTest;

    private ColumnInfo column;

    public AddUniqueColumnGeneratorTest(ColumnInfo column, String... expectedSql) {
        super(expectedSql);
        this.column = column;
    }

    @Parameterized.Parameters
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {
                        TestData.stringCol().build(),
                        new String[] {
                                "ALTER TABLE " + TABLE_NAME + " ADD COLUMN string_column TEXT;",
                                "CREATE UNIQUE INDEX IF NOT EXISTS " + TABLE_NAME + "_string_column ON " + TABLE_NAME + "(string_column);"
                        }
                },
        });
    }

    @Before
    public void setUp() {
        generatorUnderTest = new AddUniqueColumnGenerator(TABLE_NAME, column);
    }

    @Override
    protected QueryGenerator getGenerator() {
        return generatorUnderTest;
    }
}
