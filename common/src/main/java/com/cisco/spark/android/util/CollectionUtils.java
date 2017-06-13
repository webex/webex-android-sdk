package com.cisco.spark.android.util;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class CollectionUtils {
    public static <T> ArrayList<T> asList(T... array) {
        ArrayList<T> list = new ArrayList<T>();
        Collections.addAll(list, array);
        return list;
    }

    public static <T> Set<T> asSet(T... array) {
        Set<T> set = new HashSet<T>();
        Collections.addAll(set, array);
        return set;
    }

    public static <T> Set<T> merge(Collection<T> main, Collection<T> secondary, int maxItems) {
        // Preserve ordering
        Set<T> result = new LinkedHashSet<T>();
        for (T item : main) {
            if (result.size() == maxItems) {
                return result;
            }
            result.add(item);
        }
        if (secondary != null) {
            for (T item : secondary) {
                if (result.size() == maxItems) {
                    return result;
                }
                result.add(item);
            }
        }
        return result;
    }

    public static <T> T[] merge(T[] listA, T[] listB) {
        T[] merged = (T[]) Array.newInstance(listA.getClass().getComponentType(), listA.length + listB.length);
        System.arraycopy(listA, 0, merged, 0, listA.length);
        System.arraycopy(listB, 0, merged, listA.length, listB.length);
        return merged;
    }

    public static <T> List<T> merge(Collection<T> a, Collection<T> b) {
        ArrayList<T> list = new ArrayList<T>(a.size() + b.size());
        list.addAll(a);
        list.addAll(b);
        return list;
    }

    @SuppressWarnings("unchecked")
    public static <T> LinkedHashSet<T> takeLast(Set<T> collection, int count) {
        LinkedHashSet<T> result = new LinkedHashSet<T>();

        int startPosition = Math.max(collection.size() - count, 0);
        Object[] collectionArray = collection.toArray();

        for (int i = startPosition; i < collectionArray.length; i++) {
            result.add((T) collectionArray[i]);
        }
        return result;
    }

    public static <T> T getLast(Collection<T> collection) {
        Iterator<T> iterator = collection.iterator();
        T element = null;

        while (iterator.hasNext()) {
            element = iterator.next();
        }
        return element;
    }

    public static <T> boolean containsInstanceOfType(Collection<T> collection, Class type) {
        if (collection == null)
            return false;
        for (T item : collection) {
            if (type.equals(item.getClass()))
                return true;
        }
        return false;
    }
}
