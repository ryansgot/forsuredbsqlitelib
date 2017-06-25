package com.fsryan.forsuredb.sqlitelib;

import com.fsryan.forsuredb.api.info.TableInfo;
import com.fsryan.forsuredb.api.migration.QueryGenerator;

import java.util.Map;

/*package*/ class QueryGeneratorSelector {

    // TODO: remove this after removing the legacy version
    public static QueryGenerator createTableGeneratorFor(TableInfo table, Map<String, TableInfo> targetSchema) {
        return table.getForeignKeys() == null || table.getForeignKeys().isEmpty()
                ? new LegacyCreateTableGenerator(table.getTableName(), targetSchema)
                : new CreateTableGenerator(table.getTableName(), targetSchema);
    }
}
