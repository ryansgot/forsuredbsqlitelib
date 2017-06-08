package com.fsryan.forsuredb.sqlitelib;

import java.util.HashMap;
import java.util.Map;

public class CollectionUtil {

    public static Map<String, String> stringMapOf(String... kvPairs) {
        if (kvPairs == null || kvPairs.length % 2 == 1) {
            throw new IllegalArgumentException("Must not send in null kvPairs array");
        }

        Map<String, String> ret = new HashMap<>();
        for (int i = 0; i < kvPairs.length; i += 2) {
            ret.put(kvPairs[i], kvPairs[i + 1]);
        }
        return ret;
    }
}