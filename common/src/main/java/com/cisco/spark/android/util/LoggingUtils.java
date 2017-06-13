package com.cisco.spark.android.util;

import java.util.Locale;

import retrofit2.Response;

public final class LoggingUtils {

    private LoggingUtils() {
    }

    /**
     * Returns a string with information on where the current method call originated from
     */
    public static String getCaller() {
        final Exception exception = new Exception();
        final StackTraceElement element = exception.getStackTrace()[2];
        return String.format(Locale.US, "%s.%s:L%d", element.getClassName(), element.getMethodName(), element.getLineNumber());
    }

    public static String toString(Response response) {
        if (response == null)
            return null;

        return response.code() + ":" + response.message();
    }
}
