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
import com.fsryan.forsuredb.api.info.TableForeignKeyInfo;
import com.fsryan.forsuredb.api.info.TableInfo;
import com.fsryan.forsuredb.api.migration.Migration;

import java.util.*;

public class UpdateForeignKeysGenerator extends RecreateTableGenerator {

    public UpdateForeignKeysGenerator(String tableName,
                                      Set<TableForeignKeyInfo> currentForeignKeys,
                                      Set<String> currentColumns,
                                      Map<String, TableInfo> targetSchema) {
        super(tableName, currentColumns, targetSchema, Migration.Type.UPDATE_FOREIGN_KEYS);
    }
}
