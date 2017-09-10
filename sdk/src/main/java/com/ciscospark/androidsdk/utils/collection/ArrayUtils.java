package com.ciscospark.androidsdk.utils.collection;

import com.ciscospark.androidsdk.utils.Checker;
import com.ciscospark.androidsdk.utils.Consts;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Created by zhiyuliu on 01/09/2017.
 */

public class ArrayUtils {

    public static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];
    public static final Class<?>[] EMPTY_CLASS_ARRAY = new Class[0];
    public static final String[] EMPTY_STRING_ARRAY = new String[0];

    @SuppressWarnings("unchecked")
    public static <T> T[] copy(T[] orig) {
        if (orig != null) {
            T r[] = (T[]) Array.newInstance(orig.getClass().getComponentType(), orig.length);
            System.arraycopy(orig, 0, r, 0, orig.length);
            return r;
        }
        else {
            return null;
        }
    }

    public static <T, V extends T> int indexOf(T array[], V value) {
        int r = -1;
        int idx = 0;
        for (T element : array) {
            if (Checker.isEqual(element, value)) {
                r = idx;
                break;
            }
            idx++;
        }
        return r;
    }

    public static int indexOf(char[] chars, char c) {
        int r = -1;
        int idx = 0;
        for (char ex : chars) {
            if (c == ex) {
                r = idx;
                break;
            }
            idx++;
        }
        return r;
    }

    public static <T> void fill(T array[], T value) {
        for (int i = 0; i < array.length; i++) {
            array[i] = value;
        }
    }

    public static void fill(boolean array[], boolean value) {
        for (int i = 0; i < array.length; i++) {
            array[i] = value;
        }
    }

    public static void fill(int array[], int value) {
        for (int i = 0; i < array.length; i++) {
            array[i] = value;
        }
    }

    public static void fill(long array[], long value) {
        for (int i = 0; i < array.length; i++) {
            array[i] = value;
        }
    }

    public static void fill(short array[], short value) {
        for (int i = 0; i < array.length; i++) {
            array[i] = value;
        }
    }

    public static void fill(float array[], float value) {
        for (int i = 0; i < array.length; i++) {
            array[i] = value;
        }
    }

    public static void fill(double array[], double value) {
        for (int i = 0; i < array.length; i++) {
            array[i] = value;
        }
    }

    public static <T> String join(T[] array, String separator) {
        if (array == null) {
            return null;
        }
        return join(array, separator, 0, array.length);
    }

    public static <T> String join(T[] array, String separator, int startIndex, int endIndex) {
        if (array == null) {
            return null;
        }
        if (separator == null) {
            separator = Consts.EMPTY;
        }
        int noOfItems = endIndex - startIndex;
        if (noOfItems <= 0) {
            return Consts.EMPTY;
        }
        StringBuilder buf = new StringBuilder(noOfItems * 16);
        for (int i = startIndex; i < endIndex; i++) {
            if (i > startIndex) {
                buf.append(separator);
            }
            if (array[i] != null) {
                buf.append(array[i]);
            }
        }
        return buf.toString();
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] append(final T[] array, final T e) {
        final T[] newArr = (T[]) Array.newInstance(e.getClass(), array == null ? 1 : array.length + 1);
        if (array != null && array.length > 0) {
            System.arraycopy(array, 0, newArr, 0, array.length);
        }
        newArr[newArr.length - 1] = e;
        return newArr;
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] concatArrays(final T[]... arrays) {
        Class<?> clazz = null;
        int length = 0;
        for (T[] array : arrays) {
            if (array != null) {
                if (clazz == null) {
                    clazz = array.getClass().getComponentType();
                }
                length = length + array.length;
            }
        }
        if (clazz == null) {
            return null;
        }
        final T[] newArr = (T[]) Array.newInstance(clazz, length);
        int pos = 0;
        for (T[] array : arrays) {
            if (array != null) {
                System.arraycopy(array, 0, newArr, pos, array.length);
                pos = pos + array.length;
            }
        }
        return newArr;
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] mergeArrays(final T[]... arrays) {
        Class<?> clazz = null;
        final List<T> result = new ArrayList<T>();
        for (T[] array : arrays) {
            if (array != null) {
                if (clazz == null) {
                    clazz = array.getClass().getComponentType();
                }
                for (T e : array) {
                    if (!result.contains(e)) {
                        result.add(e);
                    }
                }
            }
        }
        if (clazz == null) {
            return null;
        }
        return result.toArray((T[]) Array.newInstance(clazz, result.size()));
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] removeDuplicate(final T[] array) {
        if (Checker.isEmpty(array)) {
            return array;
        }
        final Set<T> result = new TreeSet<T>();
        Collections.addAll(result, array);
        return result.toArray((T[]) Array.newInstance(array.getClass().getComponentType(), result.size()));
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] sort(final T[] array) {
        if (Checker.isEmpty(array)) {
            return (T[]) Array.newInstance(array.getClass().getComponentType(), 0);
        }
        java.util.Arrays.sort(array);
        return array;
    }

}