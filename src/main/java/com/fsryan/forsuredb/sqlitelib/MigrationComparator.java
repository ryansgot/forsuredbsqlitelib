package com.fsryan.forsuredb.sqlitelib;

import com.fsryan.forsuredb.info.ColumnInfo;
import com.fsryan.forsuredb.info.ForeignKeyInfo;
import com.fsryan.forsuredb.info.TableForeignKeyInfo;
import com.fsryan.forsuredb.info.TableInfo;
import com.fsryan.forsuredb.migration.Migration;

import java.util.Comparator;
import java.util.Map;

import static com.fsryan.forsuredb.sqlitelib.SqlGenerator.TYPES_REQUIRING_TABLE_RECREATION;

/*package*/ class MigrationComparator implements Comparator<Migration> {

    private final Map<String, TableInfo> targetSchema;

    public MigrationComparator(Map<String, TableInfo> targetSchema) {
        this.targetSchema = targetSchema;
    }

    @Override
    public int compare(Migration m1, Migration m2) {
        final boolean m1RequiresTableRecreation = TYPES_REQUIRING_TABLE_RECREATION.contains(m1.type());
        final boolean m2RequiresTableRecreation = TYPES_REQUIRING_TABLE_RECREATION.contains(m2.type());
        return m1RequiresTableRecreation && m2RequiresTableRecreation
                ? compareMigrationsRequiringMigration(m1, m2)
                : m1RequiresTableRecreation ? -1 : m2RequiresTableRecreation ? 1 : 0;
    }

    private int compareMigrationsRequiringMigration(Migration m1, Migration m2) {
        if (m1.type() == Migration.Type.CREATE_TABLE ^ m2.type() == Migration.Type.CREATE_TABLE) {
            return m1.type() == Migration.Type.CREATE_TABLE ? -1 : 1;
        }
        if (m1.type() != Migration.Type.CREATE_TABLE) {
            return 0;   // <-- neither is a create table migration
        }

        if (firstDependsUponSecond(m1, m2)) {
            return 1;
        }
        if (firstDependsUponSecond(m2, m1)) {
            return -1;
        }
        return 0;
    }

    private boolean firstDependsUponSecond(Migration first, Migration second) {
        final TableInfo firstTable = targetSchema.get(first.tableName());
        if (firstTable.foreignKeys() == null) {
            for (ColumnInfo fkColumn : firstTable.getForeignKeyColumns()) {
                final ForeignKeyInfo fk = fkColumn.foreignKeyInfo();
                if (fk != null && fk.tableName().equals(second.tableName())) {
                    return true;
                }
            }
            return false;
        }

        for (TableForeignKeyInfo firstTableForeignKey : firstTable.foreignKeys()) {
            if (firstTableForeignKey.foreignTableName().equals(second.tableName())) {
                return true;   // <-- m1Table is dependent upon m2Table
            }
        }
        return false;
    }
}
