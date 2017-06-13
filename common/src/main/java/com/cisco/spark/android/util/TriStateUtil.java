package com.cisco.spark.android.util;

/**
 * This should really be an object that wraps Boolean or a decorator for Boolean
 */
public final class TriStateUtil {

    private TriStateUtil() {
    }

    public static boolean isSet(Boolean triState) {
        return triState != null;
    }

    public static boolean isNotSet(Boolean triState) {
        return triState == null;
    }

    public static boolean getBoolean(Boolean triState, boolean defaultValue) {
        return isNotSet(triState) ? defaultValue : triState;
    }

    public static Integer integerValueOf(Boolean booleanValue) {
        return booleanValue == null ? null : (booleanValue ? 1 : 0);
    }

}
