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

import com.fsryan.forsuredb.api.info.ColumnInfo;
import com.fsryan.forsuredb.api.migration.QueryGenerator;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;

import static com.fsryan.forsuredb.sqlitelib.TestData.TABLE_NAME;
import static com.fsryan.forsuredb.sqlitelib.TestData.dateCol;
import static com.fsryan.forsuredb.sqlitelib.TestData.stringCol;

@RunWith(Parameterized.class)
public class MakeExistingColumnUniqueGeneratorTest extends BaseSQLiteGeneratorTest {

    private MakeExistingColumnUniqueGenerator generatorUndertest;

    private ColumnInfo column;

    public MakeExistingColumnUniqueGeneratorTest(ColumnInfo column, String... expectedSql) {
        super(expectedSql);
        this.column = column;
    }

    @Parameterized.Parameters
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {   // 00: make a normal column unique
                        stringCol().build(),
                        new String[] {
                                "ALTER TABLE " + TABLE_NAME + " MODIFY string_column TEXT UNIQUE;"
                        }
                },
                {   // 01: add a column that has a default set
                        dateCol().defaultValue("CURRENT_TIMESTAMP").build(),
                        new String[] {
                                "ALTER TABLE " + TABLE_NAME + " MODIFY date_column DATETIME UNIQUE DEFAULT CURRENT_TIMESTAMP;"
                        }
                }
        });
    }

    @Override
    protected QueryGenerator getGenerator() {
        return generatorUndertest;
    }

    @Before
    public void setUp() {
        generatorUndertest = new MakeExistingColumnUniqueGenerator(TABLE_NAME, column);
    }
}
