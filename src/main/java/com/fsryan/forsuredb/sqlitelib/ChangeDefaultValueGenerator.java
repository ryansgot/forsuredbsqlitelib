package com.fsryan.forsuredb.sqlitelib;

import com.fsryan.forsuredb.info.TableInfo;
import com.fsryan.forsuredb.migration.Migration;

import java.util.Map;

/**
 * <p>
 *     Should only be used with existing columns. If the column is a new column, then you can
 *     send it through
 * </p>
 */
public class ChangeDefaultValueGenerator extends RecreateTableGenerator {

    public ChangeDefaultValueGenerator(String tableName, Map<String, TableInfo> targetSchema) {
        super(tableName, targetSchema, Migration.Type.CHANGE_DEFAULT_VALUE);
    }
}
