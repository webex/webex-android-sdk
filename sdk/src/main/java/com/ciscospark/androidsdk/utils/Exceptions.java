package com.ciscospark.androidsdk.utils;

import android.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;

/**
 * Created by zhiyuliu on 01/09/2017.
 */

public class Exceptions {

    private static final String BAD_SIGNATURE = "Could not find Constructor (%s) [class=%s]";

    private static final String BAD_CREATE = "Failed to make Exception with signature (%s) [class=%s]";

    public static String printStackTrace(final Throwable t) {
        if (t == null) {
            return "";
        }
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        final String trace = sw.toString();
        return trace.replace("\r", "\\r").replace("\n", "\\n").replace("\t", " ");
    }

    @SuppressWarnings("unchecked")
    public static <T extends Throwable> T getCauseOfType(Throwable throwable, Class<T> causeClazz) {
        Throwable cause = throwable;
        final int maxDepth = 20;
        int depth = 0;
        do {
            if (causeClazz.isAssignableFrom(cause.getClass())) {
                return (T) cause;
            }
            cause = cause.getCause();
            depth++;
        }
        while (cause != null && depth < maxDepth);
        return null;
    }

    public static <E extends RuntimeException> void fling(Class<E> clazz, String message, Object... args) {
        throw make(clazz, message, args);
    }

    public static <E extends RuntimeException> E make(Class<E> clazz, String message, Object... args) {
        message = Strings.format(message, args);
        Throwable wrappedThrowable = extractThrowable(args);
        E instance = null;
        Constructor<E> constructor = null;

        if (wrappedThrowable != null) {
            constructor = findConstructor(clazz, String.class, Throwable.class);
            if (constructor != null) {
                try {
                    instance = constructor.newInstance(message, wrappedThrowable);
                }
                catch (Exception e) {
                    Log.w("", e);
                }
                if (instance != null) {
                    return instance;
                }
            }
            else {
                Log.w("", "Could not find Constructor " + clazz.getName());
            }
        }

        constructor = findConstructor(clazz, String.class);
        if (constructor != null) {
            try {
                instance = constructor.newInstance(message);
            }
            catch (Exception e) {
                Log.w("", e);
            }
            if (instance != null) {
                return instance;
            }
        }
        else {
            Log.w("", "Could not find Constructor " + clazz.getName());
        }

        constructor = findConstructor(clazz);
        if (constructor != null) {
            try {
                instance = constructor.newInstance(message);
            }
            catch (Exception e) {
                Log.w("", e);
            }
            if (instance != null) {
                return instance;
            }
        }
        else {
            Log.w("", "Could not find Constructor " + clazz.getName());
        }

        Log.w("", "Could not find a suitable contructor for Exception. Throwing VException instead.");
        throw new RuntimeException(message, wrappedThrowable);
    }

    private static <T> Constructor<T> findConstructor(Class<T> clazz, Class<?>... args) {
        try {
            return clazz.getConstructor(args);
        }
        catch (Exception e) {
            return null;
        }
    }

    private static Throwable extractThrowable(Object... args) {
        Throwable throwable = null;
        if (args.length > 0) {
            Object o = args[args.length - 1];
            if (o instanceof Throwable) {
                throwable = (Throwable) o;
            }
        }
        return throwable;
    }

}
