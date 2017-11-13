package com.fsryan.forsuredb.sqlitelib;

import com.fsryan.forsuredb.api.migration.QueryGenerator;
import com.fsryan.forsuredb.info.ColumnInfo;
import com.fsryan.forsuredb.info.TableInfo;
import com.fsryan.forsuredb.migration.Migration;

import java.util.*;

import static com.fsryan.forsuredb.sqlitelib.ApiInfo.DEFAULT_COLUMN_MAP;

public abstract class RecreateTableGenerator extends QueryGenerator {

    protected final TableInfo table;
    protected final Map<String, TableInfo> targetSchema;
    protected final List<ColumnInfo> tmpTableExcludedColumns = new ArrayList<>();

    /**
     * <p>
     *     Use this when it is possible for target columns to have not yet been added to the current schema
     * </p>
     * @param tableName the name of the table
     * @param currentColumnNames the set of current column names. If you pass in an null/empty set, then the assumption
     *                           is that all columns already exist
     * @param targetSchema the target schema
     * @param type the {@link Migration.Type} of this migration
     */
    public RecreateTableGenerator(String tableName, Set<String> currentColumnNames, Map<String, TableInfo> targetSchema, Migration.Type type) {
        super(tableName, type);
        this.table = targetSchema.get(tableName);
        this.targetSchema = targetSchema;

        if (currentColumnNames != null && !currentColumnNames.isEmpty()) {
            for (ColumnInfo targetColumn : table.getColumns()) {
                if (!currentColumnNames.contains(targetColumn.getColumnName())) {
                    tmpTableExcludedColumns.add(targetColumn);
                }
            }
        }
    }

    protected RecreateTableGenerator(String tableName, Map<String, TableInfo> targetSchema, Migration.Type type) {
        this(tableName, null, targetSchema, type);
    }

    @Override
    public List<String> generate() {
        List<String> retList = new ArrayList<>();

        retList.addAll(new CreateTempTableFromExisting(table, tmpTableExcludedColumns).generate());
        retList.addAll(new DropTableGenerator(getTableName()).generate());
        retList.addAll(new CreateTableGenerator(getTableName(), targetSchema).generate());
        for (ColumnInfo columnInfo : table.getNonForeignKeyColumns()) { // TODO: update this to filter based upon TableForeignKeyInfo
            if (DEFAULT_COLUMN_MAP.containsKey(columnInfo.getColumnName())
                    || table.getPrimaryKey().contains(columnInfo.getColumnName())
                    || columnInfo.unique()) {
                continue;   // <-- these columns were added in the CREATE TABLE query
            }
            retList.addAll(new AddColumnGenerator(getTableName(), columnInfo).generate());
        }
        retList.add(reinsertDataQuery());
        retList.addAll(new DropTableGenerator(tempTableName()).generate());

        return retList;
    }

    private String reinsertDataQuery() {
        StringBuilder buf = new StringBuilder("INSERT INTO ").append(getTableName()).append(" SELECT ");
        List<ColumnInfo> tableColumns = new ArrayList<>(table.getColumns());
        Collections.sort(tableColumns);
        for (ColumnInfo tableColumn : tableColumns) {
            if (tmpTableExcludedColumns.contains(tableColumn)) {
                buf.append(", null AS ").append(tableColumn.getColumnName());
            } else {
                buf.append("_id".equals(tableColumn.getColumnName()) ? "" : ", ").append(tableColumn.getColumnName());
            }
        }
        return buf.append(" FROM ").append(tempTableName()).append(";").toString();
    }

    private String tempTableName() {
        return "temp_" + getTableName();
    }
}
