package com.fsryan.forsuredb.sqlitelib;

import com.fsryan.forsuredb.api.info.ColumnInfo;
import com.fsryan.forsuredb.api.info.TableInfo;
import com.fsryan.forsuredb.api.migration.Migration;
import com.fsryan.forsuredb.api.migration.QueryGenerator;

import java.util.*;

import static com.fsryan.forsuredb.sqlitelib.QueryGeneratorSelector.createTableGeneratorFor;

public class UpdatePrimaryKeyGenerator extends QueryGenerator {

    private final Map<String, TableInfo> targetSchema;

    public UpdatePrimaryKeyGenerator(String tableName, Map<String, TableInfo> targetSchema) {
        super(tableName, Migration.Type.UPDATE_PRIMARY_KEY);
        this.targetSchema = targetSchema;
    }

    @Override
    public List<String> generate() {
        final TableInfo targetTable = targetSchema.get(getTableName());
        final Set<String> primaryKey = targetTable.getPrimaryKey();

        final List<String> ret = new ArrayList<>();
        ret.addAll(new CreateTempTableFromExisting(targetTable).generate());
        ret.addAll(new DropTableGenerator(getTableName()).generate());
        ret.addAll(createTableGeneratorFor(targetTable, targetSchema).generate());
        for (ColumnInfo column : targetTable.getNonForeignKeyColumns()) {
            if (TableInfo.DEFAULT_COLUMNS.containsKey(column.getColumnName()) || column.isUnique() || primaryKey.contains(column.getColumnName())) {
                continue;
            }
            ret.addAll(new AddColumnGenerator(getTableName(), column).generate());
        }
        ret.add(reinsertDataQuery(targetTable));
        ret.addAll(new DropTableGenerator(tempTableName()).generate());
        return ret;
    }

    private String reinsertDataQuery(TableInfo table) {
        StringBuffer buf = new StringBuffer("INSERT INTO ").append(getTableName()).append(" SELECT ");
        List<ColumnInfo> tableColumns = new LinkedList<>(table.getColumns());
        Collections.sort(tableColumns);
        for (ColumnInfo tableColumn : tableColumns) {
            buf.append("_id".equals(tableColumn.getColumnName()) ? "" : ", ").append(tableColumn.getColumnName());
        }
        return buf.append(" FROM ").append(tempTableName()).append(";").toString();
    }

    private String tempTableName() {
        return "temp_" + getTableName();
    }
}
