package com.ciscospark.androidsdk.utils.collection;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * Created by zhiyuliu on 02/09/2017.
 */

public class Maps {

    public static final Properties EMPTY_PROPERTIES = new Properties();

    public interface KVFilter<K, V> {
        boolean doFilter(K key, V value);
    }

    public static <K, V> void remove(Map<K, V> map, KVFilter<K, V> filter) {
        List<K> deleted = null;
        for (K key : map.keySet()) {
            V value = map.get(key);
            if (filter.doFilter(key, value)) {
                if (deleted == null) {
                    deleted = new ArrayList<K>(3);
                }
                deleted.add(key);
            }
        }
        if (deleted != null) {
            for (K key : deleted) {
                map.remove(key);
            }
            deleted.clear();
        }
        deleted = null;
    }

    public static <K, V> K keyForValue(Map<K, V> map, V value) {
        for (Map.Entry<K, V> entry : map.entrySet()) {
            if (value.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static Map<String, String> makeMap(String params[][]) {
        Map<String, String> map = new HashMap<String, String>(params.length);
        for (String[] param : params) {
            map.put(param[0], param[1]);
        }
        return map;
    }

    public static Map<String, String> makeMap(String input) {
        return makeMap(input, "=", ";");
    }

    public static Map<String, String> makeMap(String input, String separator, String delimiter) {
        Map<String, String> result = new LinkedHashMap<String, String>();
        StringTokenizer entryTokenizer = new StringTokenizer(input, delimiter);
        while (entryTokenizer.hasMoreElements()) {
            String entry = entryTokenizer.nextToken();
            int sepIndex;
            if ((sepIndex = entry.indexOf(separator)) > 0) {
                result.put(entry.substring(0, sepIndex), entry.substring(sepIndex + 1));
            }
            else {
                result.put(entry, null);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Map<?, ?>> T makeMap(Object... args) {
        if ((args.length % 2) != 0) {
            throw new IllegalArgumentException("expected even number of args");
        }
        Map<Object, Object> map = new HashMap<Object, Object>();
        for (int i = 0; i < args.length; i += 2) {
            map.put(args[i], args[i + 1]);
        }
        return (T) map;
    }

    @SuppressWarnings("unchecked")
    public static <K, V, MapType extends Map<? super K, ? super V>> MapType makeMap(K key, V value) {
        Map<K, V> map = new HashMap<K, V>();
        map.put(key, value);
        return (MapType) map;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Map<?, ?>, K extends Enum<K>> T makeEnumMap(Class<K> enumClazz, Object... args) {
        if ((args.length % 2) == 0) {
            throw new IllegalArgumentException("expected even number of args");
        }
        Map<K, Object> map = new EnumMap<K, Object>(enumClazz);
        for (int i = 0; i < args.length; i += 2) {
            K key = enumClazz.cast(args[i]);
            Object value = args[i + 1];
            map.put(key, value);
        }
        return (T) map;
    }

    public static <K, V> String join(Map<K, V> map) {
        return join(map, "=", ",");
    }

    public static <K, V> String join(Map<K, V> map, String separator, String delimiter) {
        String d = "";
        StringBuilder builder = new StringBuilder();
        for (Map.Entry entry : map.entrySet()) {
            builder.append(d);
            builder.append(entry.getKey());
            builder.append(separator);
            builder.append(entry.getValue());
            d = delimiter;
        }
        return builder.toString();
    }

    @SuppressWarnings("rawtypes")
    public static String join(Properties properties, String separator, String delimiter) {
        String d = "";
        StringBuilder builder = new StringBuilder();
        for (Map.Entry entry : properties.entrySet()) {
            builder.append(d);
            builder.append(entry.getKey());
            builder.append(separator);
            builder.append(entry.getValue());
            d = delimiter;
        }
        return builder.toString();
    }
}
