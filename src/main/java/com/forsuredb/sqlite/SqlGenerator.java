package com.forsuredb.sqlite;

import java.util.ArrayList;
import java.util.List;

import com.forsuredb.migration.Migration;
import com.forsuredb.migration.MigrationSet;

public class SqlGenerator {

    private final MigrationSet migrationSet;

    public SqlGenerator(MigrationSet migrationSet) {
        this.migrationSet = migrationSet;
    }

    public List<String> generate() {
        if (migrationSet == null || !migrationSet.containsMigrations() || migrationSet.getTargetSchema() == null) {
            return new ArrayList<>();
        }

        List<String> sqlList = new ArrayList<>();
        for (Migration m : migrationSet.getOrderedMigrations()) {
            sqlList.addAll(QueryGeneratorFactory.getFor(m, migrationSet.getTargetSchema()).generate());
        }

        return sqlList;
    }
}
