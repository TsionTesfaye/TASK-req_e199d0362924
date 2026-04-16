package com.rescuehub.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unified response builder. All helpers tolerate null values (unlike Map.of which throws NPE).
 */
public class ApiResponse {

    public static Map<String, Object> data(Object item) {
        Map<String, Object> out = new HashMap<>();
        out.put("data", item);
        return out;
    }

    public static Map<String, Object> list(List<?> items, long total, int page, int size) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("page", page);
        meta.put("size", size);
        meta.put("total", total);
        Map<String, Object> out = new HashMap<>();
        out.put("data", items);
        out.put("meta", meta);
        return out;
    }

    /**
     * Null-safe replacement for Map.of. Accepts an even-length (key, value) argument list.
     */
    public static Map<String, Object> safeMap(Object... kv) {
        if (kv.length % 2 != 0) {
            throw new IllegalArgumentException("safeMap requires an even number of arguments");
        }
        Map<String, Object> out = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            Object k = kv[i];
            if (!(k instanceof String)) {
                throw new IllegalArgumentException("safeMap keys must be Strings");
            }
            out.put((String) k, kv[i + 1]);
        }
        return out;
    }
}
