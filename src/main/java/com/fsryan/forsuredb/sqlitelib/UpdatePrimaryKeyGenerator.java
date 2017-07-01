package com.fsryan.forsuredb.sqlitelib;

import com.fsryan.forsuredb.api.info.TableInfo;
import com.fsryan.forsuredb.api.migration.Migration;

import java.util.*;

public class UpdatePrimaryKeyGenerator extends RecreateTableGenerator {

    public UpdatePrimaryKeyGenerator(String tableName, Set<String> currentColumnNames, Map<String, TableInfo> targetSchema) {
        super(tableName, currentColumnNames, targetSchema, Migration.Type.UPDATE_PRIMARY_KEY);
    }
}
