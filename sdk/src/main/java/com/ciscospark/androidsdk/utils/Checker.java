package com.ciscospark.androidsdk.utils;

import com.ciscospark.androidsdk.utils.collection.ArrayUtils;
import com.ciscospark.androidsdk.utils.reflect.Members;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.Map;

/**
 * Created by zhiyuliu on 01/09/2017.
 */

public class Checker {

    public static boolean isSameLength(Object[] array1, Object[] array2) {
        return !((array1 == null && array2 != null && array2.length > 0) ||
                (array2 == null && array1 != null && array1.length > 0) ||
                (array1 != null && array2 != null && array1.length != array2.length));
    }

    public static boolean isEmpty(final Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    public static boolean isEmpty(final Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    public static <T> boolean isEmpty(T array[]) {
        return array == null || array.length == 0;
    }

    public static <T> boolean isEmpty(byte[] array) {
        return array == null || array.length == 0;
    }

    public static <T> boolean isEmpty(char[] array) {
        return array == null || array.length == 0;
    }

    public static boolean isEmpty(CharSequence s) {
        return (s == null) || (s.length() == 0);
    }

    public static boolean isBlank(final CharSequence s) {
        int strLen;
        if (s == null || (strLen = s.length()) == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isAnyBlank(String[] strs) {
        if (Checker.isEmpty(strs)) {
            return true;
        }
        for (String str : strs) {
            if (isBlank(str)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isEqual(Object lhs, Object rhs) {
        return lhs == rhs || (lhs != null && lhs.equals(rhs));
    }

    public static boolean isEquals(final byte b1[], final byte b2[]) {
        if (b1 == null && b2 == null) {
            return true;
        }
        if (b1 == null || b2 == null) {
            return false;
        }
        final int len1 = b1.length;
        final int len2 = b2.length;
        if (len2 != len1) {
            return false;
        }
        for (int i = 0; i < len1; i++) {
            if (b1[i] != b2[i]) {
                return false;
            }
        }
        return true;
    }

    public static boolean isAnyNull(Object... args) {
        for (Object o : args) {
            if (o == null) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAllNotNull(Object... args) {
        return !isAnyNull(args);
    }

    public static boolean isAssignable(Class<?>[] classArray, Class<?>[] toClassArray) {
        if (!isSameLength(classArray, toClassArray)) {
            return false;
        }
        if (classArray == null) {
            classArray = ArrayUtils.EMPTY_CLASS_ARRAY;
        }
        if (toClassArray == null) {
            toClassArray = ArrayUtils.EMPTY_CLASS_ARRAY;
        }
        for (int i = 0; i < classArray.length; i++) {
            if (!isAssignable(classArray[i], toClassArray[i])) {
                return false;
            }
        }
        return true;
    }

    public static boolean isAssignable(Class<?> cls, Class<?> toClass) {
        if (toClass == null) {
            return false;
        }
        if (cls == null) {
            return !toClass.isPrimitive();
        }
        if (cls.isPrimitive() && !toClass.isPrimitive()) {
            cls = Members.primitiveToWrapper(cls);
            if (cls == null) {
                return false;
            }
        }
        if (toClass.isPrimitive() && !cls.isPrimitive()) {
            cls = Members.wrapperToPrimitive(cls);
            if (cls == null) {
                return false;
            }
        }

        if (cls.equals(toClass)) {
            return true;
        }
        if (cls.isPrimitive()) {
            if (!toClass.isPrimitive()) {
                return false;
            }
            if (Integer.TYPE.equals(cls)) {
                return Long.TYPE.equals(toClass)
                        || Float.TYPE.equals(toClass)
                        || Double.TYPE.equals(toClass);
            }
            if (Long.TYPE.equals(cls)) {
                return Float.TYPE.equals(toClass)
                        || Double.TYPE.equals(toClass);
            }
            if (Boolean.TYPE.equals(cls)) {
                return false;
            }
            if (Double.TYPE.equals(cls)) {
                return false;
            }
            if (Float.TYPE.equals(cls)) {
                return Double.TYPE.equals(toClass);
            }
            if (Character.TYPE.equals(cls)) {
                return Integer.TYPE.equals(toClass)
                        || Long.TYPE.equals(toClass)
                        || Float.TYPE.equals(toClass)
                        || Double.TYPE.equals(toClass);
            }
            if (Short.TYPE.equals(cls)) {
                return Integer.TYPE.equals(toClass)
                        || Long.TYPE.equals(toClass)
                        || Float.TYPE.equals(toClass)
                        || Double.TYPE.equals(toClass);
            }
            if (Byte.TYPE.equals(cls)) {
                return Short.TYPE.equals(toClass)
                        || Integer.TYPE.equals(toClass)
                        || Long.TYPE.equals(toClass)
                        || Float.TYPE.equals(toClass)
                        || Double.TYPE.equals(toClass);
            }
            // should never get here
            return false;
        }
        return toClass.isAssignableFrom(cls);
    }

    public static boolean isTrue(Boolean bool) {
        return Boolean.TRUE.equals(bool);
    }

    public static boolean isNumber(String str) {
        if (isEmpty(str)) {
            return false;
        }
        char[] chars = str.toCharArray();
        int sz = chars.length;
        boolean hasExp = false;
        boolean hasDecPoint = false;
        boolean allowSigns = false;
        boolean foundDigit = false;
        int start = (chars[0] == '-') ? 1 : 0;
        if (sz > start + 1 && chars[start] == '0' && chars[start + 1] == 'x') {
            int i = start + 2;
            if (i == sz) {
                return false;
            }
            for (; i < chars.length; i++) {
                if ((chars[i] < '0' || chars[i] > '9')
                        && (chars[i] < 'a' || chars[i] > 'f')
                        && (chars[i] < 'A' || chars[i] > 'F')) {
                    return false;
                }
            }
            return true;
        }
        sz--;
        int i = start;
        while (i < sz || (i < sz + 1 && allowSigns && !foundDigit)) {
            if (chars[i] >= '0' && chars[i] <= '9') {
                foundDigit = true;
                allowSigns = false;

            }
            else if (chars[i] == '.') {
                if (hasDecPoint || hasExp) {
                    return false;
                }
                hasDecPoint = true;
            }
            else if (chars[i] == 'e' || chars[i] == 'E') {
                if (hasExp) {
                    return false;
                }
                if (!foundDigit) {
                    return false;
                }
                hasExp = true;
                allowSigns = true;
            }
            else if (chars[i] == '+' || chars[i] == '-') {
                if (!allowSigns) {
                    return false;
                }
                allowSigns = false;
                foundDigit = false;
            }
            else {
                return false;
            }
            i++;
        }
        if (i < chars.length) {
            if (chars[i] >= '0' && chars[i] <= '9') {
                return true;
            }
            if (chars[i] == 'e' || chars[i] == 'E') {
                return false;
            }
            if (chars[i] == '.') {
                return !(hasDecPoint || hasExp) && foundDigit;
            }
            if (!allowSigns
                    && (chars[i] == 'd'
                    || chars[i] == 'D'
                    || chars[i] == 'f'
                    || chars[i] == 'F')) {
                return foundDigit;
            }
            return (chars[i] == 'l' || chars[i] == 'L') && foundDigit && !hasExp && !hasDecPoint;
        }
        return !allowSigns && foundDigit;
    }

    public static boolean isAlpha(CharSequence cs, char... exclusions) {
        if (cs == null || cs.length() == 0) {
            return false;
        }
        int sz = cs.length();
        for (int i = 0; i < sz; i++) {
            if (!Chars.isAlpha(cs.charAt(i))) {
                if (isEmpty(exclusions)) {
                    return false;
                }
                else {
                    if (ArrayUtils.indexOf(exclusions, cs.charAt(i)) == -1) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static boolean isAlphanumeric(CharSequence cs, char... exclusions) {
        if (cs == null || cs.length() == 0) {
            return false;
        }
        int sz = cs.length();
        for (int i = 0; i < sz; i++) {
            if (!Chars.isAlphaNum(cs.charAt(i))) {
                if (isEmpty(exclusions)) {
                    return false;
                }
                else {
                    if (ArrayUtils.indexOf(exclusions, cs.charAt(i)) == -1) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static boolean isNumeric(CharSequence cs, char... exclusions) {
        if (cs == null || cs.length() == 0) {
            return false;
        }
        int sz = cs.length();
        for (int i = 0; i < sz; i++) {
            if (!Chars.isDigit(cs.charAt(i))) {
                if (isEmpty(exclusions)) {
                    return false;
                }
                else {
                    if (ArrayUtils.indexOf(exclusions, cs.charAt(i)) == -1) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static boolean isAsciiPrintable(CharSequence cs) {
        if (cs == null) {
            return false;
        }
        int sz = cs.length();
        for (int i = 0; i < sz; i++) {
            if (!Chars.isAsciiPrintable(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * RFC 3296:
     * <p/>
     * <pre>
     * scheme = alpha * (alpha | digit | &quot;+&quot; | &quot;-&quot; | &quot;.&quot;)
     * </pre>
     */
    public static boolean isValidURIScheme(final String s) {
        try {
            if (!Chars.isAlpha(s.charAt(0))) {
                return false;
            }
            for (int i = 1; i < s.length(); i++) {
                final int c = s.charAt(i);
                if (!Chars.isAlphaNum(c) && c != '+' && c != '-' && c != '.') {
                    return false;
                }
            }
            return true;
        }
        catch (final Exception e) {
            return false;
        }
    }

    public static boolean isSQLString(final Object o) {
        return o instanceof CharSequence || o instanceof Timestamp;
    }

}
