package com.fsryan.forsuredb.sqlitelib;

import com.fsryan.forsuredb.api.info.ColumnInfo;
import com.fsryan.forsuredb.api.migration.Migration;
import com.fsryan.forsuredb.api.migration.QueryGenerator;

import java.util.ArrayList;
import java.util.List;

public class MakeExistingColumnUniqueGenerator extends QueryGenerator {

    private final ColumnInfo column;

    public MakeExistingColumnUniqueGenerator(String tableName, ColumnInfo column) {
        super(tableName, Migration.Type.MAKE_COLUMN_UNIQUE);
        this.column = column;
    }

    @Override
    public List<String> generate() {
        List<String> ret = new ArrayList<>(1);
        ret.add("ALTER TABLE " + getTableName()
                + " MODIFY " + column.getColumnName() + " " + TypeTranslator.from(column.getQualifiedType()).getSqlString()
                + " UNIQUE" + (column.hasDefaultValue() ? " DEFAULT " + column.getDefaultValue() : "") + ";");
        return ret;
    }
}
