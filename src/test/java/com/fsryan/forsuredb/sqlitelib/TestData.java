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

import com.fsryan.forsuredb.annotations.ForeignKey;
import com.fsryan.forsuredb.api.info.ColumnInfo;
import com.fsryan.forsuredb.api.info.ForeignKeyInfo;
import com.fsryan.forsuredb.api.info.TableInfo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class TestData {

    public static final String TEST_RES = "src" + File.separator + "test" + File.separator + "resources";

    // Convenience constants
    public static final String TABLE_NAME = "test_table";

    public static String resourceText(String resourceName) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(TEST_RES + File.separator + resourceName));
        String line;
        StringBuilder out = new StringBuilder();
        while (null != (line = br.readLine())) {
            out.append(line).append("\n");
        }
        br.close();
        return out.toString();
    }

    public static <T> Set<T> setOf(T... ts) {
        return new HashSet<>(Arrays.asList(ts));
    }

    public static Set<String> defaultColumnNameSet() {
        return setOf("created", "deleted", "modified", "_id");
    }

    public static Set<String> defaultColumnNamesWith(String... columnNames) {
        Set<String> ret = defaultColumnNameSet();
        ret.addAll(Arrays.asList(columnNames));
        return ret;
    }

    public static Map<String, ColumnInfo> columnMapOf(ColumnInfo... columns) {
        Map<String, ColumnInfo> retMap = new HashMap<>();
        for (ColumnInfo column : columns) {
            retMap.put(column.getColumnName(), column);
        }
        return retMap;
    }

    public static Map<String, TableInfo> tableMapOf(TableInfo... tables) {
        Map<String, TableInfo> ret = new HashMap<>();
        for (TableInfo table : tables) {
            ret.put(table.getTableName(), table);
        }
        return ret;
    }

    // Convenience methods for making data to go into the tests
    public static TableInfo.Builder table() {
        return TableInfo.builder().tableName(TABLE_NAME);
    }

    public static ColumnInfo idCol() {
        return ColumnInfo.builder().columnName("_id")
                .qualifiedType(TypeTranslator.LONG.getQualifiedType())
                .primaryKey(true)
                .build();
    }

    public static ColumnInfo createdCol() {
        return ColumnInfo.builder().columnName("created")
                .qualifiedType(TypeTranslator.DATE.getQualifiedType())
                .defaultValue("CURRENT_TIMESTAMP")
                .build();
    }

    public static ColumnInfo deletedCol() {
        return ColumnInfo.builder().columnName("deleted")
                .qualifiedType(TypeTranslator.BOOLEAN.getQualifiedType())
                .defaultValue("0")
                .build();
    }

    public static ColumnInfo modifiedCol() {
        return ColumnInfo.builder().columnName("modified")
                .qualifiedType(TypeTranslator.DATE.getQualifiedType())
                .defaultValue("CURRENT_TIMESTAMP")
                .build();
    }

    public static ColumnInfo.Builder stringCol() {
        return columnFrom(TypeTranslator.STRING);
    }

    public static ColumnInfo.Builder intCol() {
        return columnFrom(TypeTranslator.INT);
    }

    public static ColumnInfo.Builder longCol() {
        return columnFrom(TypeTranslator.LONG);
    }

    public static ColumnInfo.Builder doubleCol() {
        return columnFrom(TypeTranslator.DOUBLE);
    }

    public static ColumnInfo.Builder booleanCol() {
        return columnFrom(TypeTranslator.BOOLEAN);
    }

    public static ColumnInfo.Builder bigDecimalCol() {
        return columnFrom(TypeTranslator.BIG_DECIMAL);
    }

    public static ColumnInfo.Builder dateCol() {
        return columnFrom(TypeTranslator.DATE);
    }

    public static ForeignKeyInfo.Builder cascadeFKI(String foreignKeyTableName) {
        return ForeignKeyInfo.builder().updateAction(ForeignKey.ChangeAction.CASCADE)
                .deleteAction(ForeignKey.ChangeAction.CASCADE)
                .columnName("_id")
                .tableName(foreignKeyTableName);
    }

    public static ForeignKeyInfo.Builder noActionFKI(String foreignKeyTableName) {
        return ForeignKeyInfo.builder().updateAction(ForeignKey.ChangeAction.NO_ACTION)
                .deleteAction(ForeignKey.ChangeAction.NO_ACTION)
                .columnName("_id")
                .tableName(foreignKeyTableName);
    }

    public static ForeignKeyInfo.Builder setNullFKI(String foreignKeyTableName) {
        return ForeignKeyInfo.builder().updateAction(ForeignKey.ChangeAction.SET_NULL)
                .deleteAction(ForeignKey.ChangeAction.SET_NULL)
                .columnName("_id")
                .tableName(foreignKeyTableName);
    }

    public static ForeignKeyInfo.Builder setDefaultFKI(String foreignKeyTableName) {
        return ForeignKeyInfo.builder().updateAction(ForeignKey.ChangeAction.SET_DEFAULT)
                .deleteAction(ForeignKey.ChangeAction.SET_DEFAULT)
                .columnName("_id")
                .tableName(foreignKeyTableName);
    }

    public static ForeignKeyInfo.Builder restrictFKI(String foreignKeyTableName) {
        return ForeignKeyInfo.builder().updateAction(ForeignKey.ChangeAction.RESTRICT)
                .deleteAction(ForeignKey.ChangeAction.RESTRICT)
                .columnName("_id")
                .tableName(foreignKeyTableName);
    }

    // Helpers for covenience methods

    private static ColumnInfo.Builder columnFrom(TypeTranslator tt) {
        return ColumnInfo.builder().columnName(nameFrom(tt)).qualifiedType(tt.getQualifiedType());
    }

    private static String nameFrom(TypeTranslator tt) {
        return tt.name().toLowerCase() + "_column";
    }
}
