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
import com.fsryan.forsuredb.info.TableInfo;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.List;

@RunWith(Parameterized.class)
public class AddForeignKeyGeneratorTest extends BaseSQLiteGeneratorTest {

    private AddForeignKeyGenerator generatorUnderTest;

    private TableInfo table;
    private List<ColumnInfo> newForeignKeyColumns;

    public AddForeignKeyGeneratorTest(TableInfo table, List<ColumnInfo> newForeignKeyColumns, String... expectedSql) {
        super (expectedSql);
        this.table = table;
        this.newForeignKeyColumns = newForeignKeyColumns;
    }

    @Parameterized.Parameters
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] {
//                {   // 0 Add a foreign key to a basic table with no extra columns
//                        table().columnMap(columnMapOf(longCol()
//                                        .foreignKeyInfo(cascadeFKI("user")
//                                                .build())
//                                        .build()))
//                                .build(),
//                        Lists.newArrayList(longCol().foreignKeyInfo(cascadeFKI("user").build()).build()),
//                        new String[] {
//                                "DROP TABLE IF EXISTS temp_" + TABLE_NAME + ";",
//                                "CREATE TEMP TABLE temp_" + TABLE_NAME + " AS SELECT _id, created, deleted, modified FROM " + TABLE_NAME + ";",
//                                "DROP TABLE IF EXISTS " + TABLE_NAME + ";",
//                                "CREATE TABLE " + TABLE_NAME + "(_id INTEGER PRIMARY KEY, created DATETIME DEFAULT CURRENT_TIMESTAMP, deleted INTEGER DEFAULT 0, modified DATETIME DEFAULT CURRENT_TIMESTAMP, long_column INTEGER, FOREIGN KEY(long_column) REFERENCES user(_id) ON UPDATE CASCADE ON DELETE CASCADE);",
//                                "CREATE TRIGGER " + TABLE_NAME + "_updated_trigger AFTER UPDATE ON " + TABLE_NAME + " BEGIN UPDATE " + TABLE_NAME + " SET modified=CURRENT_TIMESTAMP WHERE _id=NEW._id; END;",
//                                "INSERT INTO " + TABLE_NAME + " SELECT _id, created, deleted, modified, null AS long_column FROM temp_" + TABLE_NAME + ";",
//                                "DROP TABLE IF EXISTS temp_" + TABLE_NAME + ";"
//                        }
//                },
//                {   // 1 Add a foreign key to a basic table with one extra non-foreign key column
//                        table().columnMap(columnMapOf(longCol().foreignKeyInfo(cascadeFKI("user")
//                                                .build())
//                                        .build(),
//                                        intCol().build()))
//                                .build(),
//                        Lists.newArrayList(longCol().foreignKeyInfo(cascadeFKI("user").build()).build()),
//                        new String[]{
//                                "DROP TABLE IF EXISTS temp_" + TABLE_NAME + ";",
//                                "CREATE TEMP TABLE temp_" + TABLE_NAME + " AS SELECT _id, created, deleted, modified, int_column FROM " + TABLE_NAME + ";",
//                                "DROP TABLE IF EXISTS " + TABLE_NAME + ";",
//                                "CREATE TABLE " + TABLE_NAME + "(_id INTEGER PRIMARY KEY, created DATETIME DEFAULT CURRENT_TIMESTAMP, deleted INTEGER DEFAULT 0, modified DATETIME DEFAULT CURRENT_TIMESTAMP, long_column INTEGER, FOREIGN KEY(long_column) REFERENCES user(_id) ON UPDATE CASCADE ON DELETE CASCADE);",
//                                "CREATE TRIGGER " + TABLE_NAME + "_updated_trigger AFTER UPDATE ON " + TABLE_NAME + " BEGIN UPDATE " + TABLE_NAME + " SET modified=CURRENT_TIMESTAMP WHERE _id=NEW._id; END;",
//                                "ALTER TABLE " + TABLE_NAME + " ADD COLUMN int_column INTEGER;",
//                                "INSERT INTO " + TABLE_NAME + " SELECT _id, created, deleted, modified, null AS long_column, int_column FROM temp_" + TABLE_NAME + ";",
//                                "DROP TABLE IF EXISTS temp_" + TABLE_NAME + ";"
//                        }
//                },
//                {   // 2 Add a foreign key with NO_ACTION as its delete and update action
//                        table().columnMap(columnMapOf(longCol().foreignKeyInfo(noActionFKI("user")
//                                                .build())
//                                        .build(),
//                                        intCol().build()))
//                                .build(),
//                        Lists.newArrayList(longCol().foreignKeyInfo(noActionFKI("user").build()).build()),
//                        new String[]{
//                                "DROP TABLE IF EXISTS temp_" + TABLE_NAME + ";",
//                                "CREATE TEMP TABLE temp_" + TABLE_NAME + " AS SELECT _id, created, deleted, modified, int_column FROM " + TABLE_NAME + ";",
//                                "DROP TABLE IF EXISTS " + TABLE_NAME + ";",
//                                "CREATE TABLE " + TABLE_NAME + "(_id INTEGER PRIMARY KEY, created DATETIME DEFAULT CURRENT_TIMESTAMP, deleted INTEGER DEFAULT 0, modified DATETIME DEFAULT CURRENT_TIMESTAMP, long_column INTEGER, FOREIGN KEY(long_column) REFERENCES user(_id) ON UPDATE NO ACTION ON DELETE NO ACTION);",
//                                "CREATE TRIGGER " + TABLE_NAME + "_updated_trigger AFTER UPDATE ON " + TABLE_NAME + " BEGIN UPDATE " + TABLE_NAME + " SET modified=CURRENT_TIMESTAMP WHERE _id=NEW._id; END;",
//                                "ALTER TABLE " + TABLE_NAME + " ADD COLUMN int_column INTEGER;",
//                                "INSERT INTO " + TABLE_NAME + " SELECT _id, created, deleted, modified, null AS long_column, int_column FROM temp_" + TABLE_NAME + ";",
//                                "DROP TABLE IF EXISTS temp_" + TABLE_NAME + ";"
//                        }
//                },
//                {   // 3 Add a foreign key with RESTRICT as its delete and update action
//                        table().columnMap(columnMapOf(longCol().foreignKeyInfo(restrictFKI("user")
//                                                .build())
//                                        .build(),
//                                        intCol().build()))
//                                .build(),
//                        Lists.newArrayList(longCol().foreignKeyInfo(restrictFKI("user").build()).build()),
//                        new String[]{
//                                "DROP TABLE IF EXISTS temp_" + TABLE_NAME + ";",
//                                "CREATE TEMP TABLE temp_" + TABLE_NAME + " AS SELECT _id, created, deleted, modified, int_column FROM " + TABLE_NAME + ";",
//                                "DROP TABLE IF EXISTS " + TABLE_NAME + ";",
//                                "CREATE TABLE " + TABLE_NAME + "(_id INTEGER PRIMARY KEY, created DATETIME DEFAULT CURRENT_TIMESTAMP, deleted INTEGER DEFAULT 0, modified DATETIME DEFAULT CURRENT_TIMESTAMP, long_column INTEGER, FOREIGN KEY(long_column) REFERENCES user(_id) ON UPDATE RESTRICT ON DELETE RESTRICT);",
//                                "CREATE TRIGGER " + TABLE_NAME + "_updated_trigger AFTER UPDATE ON " + TABLE_NAME + " BEGIN UPDATE " + TABLE_NAME + " SET modified=CURRENT_TIMESTAMP WHERE _id=NEW._id; END;",
//                                "ALTER TABLE " + TABLE_NAME + " ADD COLUMN int_column INTEGER;",
//                                "INSERT INTO " + TABLE_NAME + " SELECT _id, created, deleted, modified, null AS long_column, int_column FROM temp_" + TABLE_NAME + ";",
//                                "DROP TABLE IF EXISTS temp_" + TABLE_NAME + ";"
//                        }
//                },
//                {   // 4 Add a foreign key with SET_NULL as its delete and update action
//                        table().columnMap(columnMapOf(longCol().foreignKeyInfo(setNullFKI("user")
//                                                .build())
//                                        .build(),
//                                        intCol().build()))
//                                .build(),
//                        Lists.newArrayList(longCol().foreignKeyInfo(setNullFKI("user").build()).build()),
//                        new String[]{
//                                "DROP TABLE IF EXISTS temp_" + TABLE_NAME + ";",
//                                "CREATE TEMP TABLE temp_" + TABLE_NAME + " AS SELECT _id, created, deleted, modified, int_column FROM " + TABLE_NAME + ";",
//                                "DROP TABLE IF EXISTS " + TABLE_NAME + ";",
//                                "CREATE TABLE " + TABLE_NAME + "(_id INTEGER PRIMARY KEY, created DATETIME DEFAULT CURRENT_TIMESTAMP, deleted INTEGER DEFAULT 0, modified DATETIME DEFAULT CURRENT_TIMESTAMP, long_column INTEGER, FOREIGN KEY(long_column) REFERENCES user(_id) ON UPDATE SET NULL ON DELETE SET NULL);",
//                                "CREATE TRIGGER " + TABLE_NAME + "_updated_trigger AFTER UPDATE ON " + TABLE_NAME + " BEGIN UPDATE " + TABLE_NAME + " SET modified=CURRENT_TIMESTAMP WHERE _id=NEW._id; END;",
//                                "ALTER TABLE " + TABLE_NAME + " ADD COLUMN int_column INTEGER;",
//                                "INSERT INTO " + TABLE_NAME + " SELECT _id, created, deleted, modified, null AS long_column, int_column FROM temp_" + TABLE_NAME + ";",
//                                "DROP TABLE IF EXISTS temp_" + TABLE_NAME + ";"
//                        }
//                },
//                {   // 5 Add a foreign key with SET_DEFAULT as its delete and update action
//                        table().columnMap(columnMapOf(longCol().foreignKeyInfo(setDefaultFKI("user")
//                                                .build())
//                                        .build(),
//                                        intCol().build()))
//                                .build(),
//                        Lists.newArrayList(longCol().foreignKeyInfo(setDefaultFKI("user").build()).build()),
//                        new String[]{
//                                "DROP TABLE IF EXISTS temp_" + TABLE_NAME + ";",
//                                "CREATE TEMP TABLE temp_" + TABLE_NAME + " AS SELECT _id, created, deleted, modified, int_column FROM " + TABLE_NAME + ";",
//                                "DROP TABLE IF EXISTS " + TABLE_NAME + ";",
//                                "CREATE TABLE " + TABLE_NAME + "(_id INTEGER PRIMARY KEY, created DATETIME DEFAULT CURRENT_TIMESTAMP, deleted INTEGER DEFAULT 0, modified DATETIME DEFAULT CURRENT_TIMESTAMP, long_column INTEGER, FOREIGN KEY(long_column) REFERENCES user(_id) ON UPDATE SET DEFAULT ON DELETE SET DEFAULT);",
//                                "CREATE TRIGGER " + TABLE_NAME + "_updated_trigger AFTER UPDATE ON " + TABLE_NAME + " BEGIN UPDATE " + TABLE_NAME + " SET modified=CURRENT_TIMESTAMP WHERE _id=NEW._id; END;",
//                                "ALTER TABLE " + TABLE_NAME + " ADD COLUMN int_column INTEGER;",
//                                "INSERT INTO " + TABLE_NAME + " SELECT _id, created, deleted, modified, null AS long_column, int_column FROM temp_" + TABLE_NAME + ";",
//                                "DROP TABLE IF EXISTS temp_" + TABLE_NAME + ";"
//                        }
//                },
//                {   // 6 Add a foreign key with SET_DEFAULT as its delete action and SET_NULL as its update action
//                        table().columnMap(columnMapOf(longCol().foreignKeyInfo(setDefaultFKI("user")
//                                                .updateAction(ForeignKey.ChangeAction.SET_NULL)
//                                                .build())
//                                        .build(),
//                                        intCol().build()))
//                                .build(),
//                        Lists.newArrayList(longCol().foreignKeyInfo(setDefaultFKI("user").updateAction(ForeignKey.ChangeAction.SET_NULL).build()).build()),
//                        new String[]{
//                                "DROP TABLE IF EXISTS temp_" + TABLE_NAME + ";",
//                                "CREATE TEMP TABLE temp_" + TABLE_NAME + " AS SELECT _id, created, deleted, modified, int_column FROM " + TABLE_NAME + ";",
//                                "DROP TABLE IF EXISTS " + TABLE_NAME + ";",
//                                "CREATE TABLE " + TABLE_NAME + "(_id INTEGER PRIMARY KEY, created DATETIME DEFAULT CURRENT_TIMESTAMP, deleted INTEGER DEFAULT 0, modified DATETIME DEFAULT CURRENT_TIMESTAMP, long_column INTEGER, FOREIGN KEY(long_column) REFERENCES user(_id) ON UPDATE SET NULL ON DELETE SET DEFAULT);",
//                                "CREATE TRIGGER " + TABLE_NAME + "_updated_trigger AFTER UPDATE ON " + TABLE_NAME + " BEGIN UPDATE " + TABLE_NAME + " SET modified=CURRENT_TIMESTAMP WHERE _id=NEW._id; END;",
//                                "ALTER TABLE " + TABLE_NAME + " ADD COLUMN int_column INTEGER;",
//                                "INSERT INTO " + TABLE_NAME + " SELECT _id, created, deleted, modified, null AS long_column, int_column FROM temp_" + TABLE_NAME + ";",
//                                "DROP TABLE IF EXISTS temp_" + TABLE_NAME + ";"
//                        }
//                },
//                {   // 7 Add a foreign key to a basic table with one extra foreign key
//                        table().columnMap(columnMapOf(longCol().foreignKeyInfo(cascadeFKI("user")
//                                        .build())
//                                        .build(),
//                                intCol().foreignKeyInfo(cascadeFKI("profile_info").build()).build()))
//                                .build(),
//                        Lists.newArrayList(longCol().foreignKeyInfo(cascadeFKI("user").build()).build()),
//                        new String[]{
//                                "DROP TABLE IF EXISTS temp_" + TABLE_NAME + ";",
//                                "CREATE TEMP TABLE temp_" + TABLE_NAME + " AS SELECT _id, created, deleted, modified, int_column FROM " + TABLE_NAME + ";",
//                                "DROP TABLE IF EXISTS " + TABLE_NAME + ";",
//                                "CREATE TABLE " + TABLE_NAME + "(_id INTEGER PRIMARY KEY, created DATETIME DEFAULT CURRENT_TIMESTAMP, deleted INTEGER DEFAULT 0, modified DATETIME DEFAULT CURRENT_TIMESTAMP, int_column INTEGER, long_column INTEGER, FOREIGN KEY(int_column) REFERENCES profile_info(_id) ON UPDATE CASCADE ON DELETE CASCADE, FOREIGN KEY(long_column) REFERENCES user(_id) ON UPDATE CASCADE ON DELETE CASCADE);",
//                                "CREATE TRIGGER " + TABLE_NAME + "_updated_trigger AFTER UPDATE ON " + TABLE_NAME + " BEGIN UPDATE " + TABLE_NAME + " SET modified=CURRENT_TIMESTAMP WHERE _id=NEW._id; END;",
//                                "INSERT INTO " + TABLE_NAME + " SELECT _id, created, deleted, modified, int_column, null AS long_column FROM temp_" + TABLE_NAME + ";",
//                                "DROP TABLE IF EXISTS temp_" + TABLE_NAME + ";"
//                        }
//                },
//                {   // 8 Add more than one foreign key at a time
//                        table().columnMap(columnMapOf(longCol().foreignKeyInfo(cascadeFKI("user")
//                                        .build())
//                                        .build(),
//                                intCol().foreignKeyInfo(cascadeFKI("profile_info").build()).build()))
//                                .build(),
//                        Lists.newArrayList(longCol().foreignKeyInfo(cascadeFKI("user").build()).build(), stringCol().columnName("another_table_string_column").foreignKeyInfo(cascadeFKI("another_table").columnName("string_column").build()).build()),
//                        new String[]{
//                                "DROP TABLE IF EXISTS temp_" + TABLE_NAME + ";",
//                                "CREATE TEMP TABLE temp_" + TABLE_NAME + " AS SELECT _id, created, deleted, modified, int_column FROM " + TABLE_NAME + ";",
//                                "DROP TABLE IF EXISTS " + TABLE_NAME + ";",
//                                "CREATE TABLE " + TABLE_NAME + "(_id INTEGER PRIMARY KEY, created DATETIME DEFAULT CURRENT_TIMESTAMP, deleted INTEGER DEFAULT 0, modified DATETIME DEFAULT CURRENT_TIMESTAMP, int_column INTEGER, long_column INTEGER, another_table_string_column TEXT, FOREIGN KEY(int_column) REFERENCES profile_info(_id) ON UPDATE CASCADE ON DELETE CASCADE, FOREIGN KEY(long_column) REFERENCES user(_id) ON UPDATE CASCADE ON DELETE CASCADE, FOREIGN KEY(another_table_string_column) REFERENCES another_table(string_column) ON UPDATE CASCADE ON DELETE CASCADE);",
//                                "CREATE TRIGGER " + TABLE_NAME + "_updated_trigger AFTER UPDATE ON " + TABLE_NAME + " BEGIN UPDATE " + TABLE_NAME + " SET modified=CURRENT_TIMESTAMP WHERE _id=NEW._id; END;",
//                                "INSERT INTO " + TABLE_NAME + " SELECT _id, created, deleted, modified, int_column, null AS long_column FROM temp_" + TABLE_NAME + ";",
//                                "DROP TABLE IF EXISTS temp_" + TABLE_NAME + ";"
//                        }
//                }
        });
    }

    @Before
    public void setUp() {
        if (generatorUnderTest == null) {
            generatorUnderTest = new AddForeignKeyGenerator(table, newForeignKeyColumns, null);
        }
    }

    @Override
    protected QueryGenerator getGenerator() {
        return generatorUnderTest;
    }
}
