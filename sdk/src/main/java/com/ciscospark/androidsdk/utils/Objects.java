package com.ciscospark.androidsdk.utils;

import com.ciscospark.androidsdk.utils.annotation.StringPart;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by zhiyuliu on 01/09/2017.
 */

public class Objects {

    public static final Null NULL = new Null();

    private Objects() {
        super();
    }

    public static <T> T defaultIfNull(T object, T defaultValue) {
        return object != null ? object : defaultValue;
    }

    public static int hashCode(Object obj) {
        return (obj == null) ? 0 : obj.hashCode();
    }

    public static String identityString(Object object) {
        if (object == null) {
            return null;
        }
        StringBuilder buffer = new StringBuilder();
        return buffer.append(object.getClass().getName()).append('@')
                .append(Integer.toHexString(System.identityHashCode(object))).toString();
    }

    public static String toString(Object obj) {
        return toString(obj, "");
    }

    public static String toString(Object obj, String nullStr) {
        return obj == null ? nullStr : obj.toString();
    }

    public static String toReadableString(final Object input) {
        return toReadableString(input, null);
    }

    public static String toReadableString(final Object input, final String separator) {
        final StringBuilder buffer = new StringBuilder();
        if (input instanceof Collection) {
            final Collection col = (Collection) input;
            for (final Object o : col) {
                buffer.append("{").append(toString(o, separator)).append("}").append(separator == null ? "," : separator);
            }
            if (buffer.length() > 0) {
                buffer.deleteCharAt(buffer.length() - 1);
            }
        }
        else if (input instanceof Map) {
            final Map map = (Map) input;
            for (final Object key : map.keySet()) {
                final Object value = map.get(key);
                buffer.append(toString(key, separator)).append("=").append("{").append(toString(value, separator)).append("}")
                        .append(separator == null ? "," : separator);
            }
            if (buffer.length() > 0) {
                buffer.deleteCharAt(buffer.length() - 1);
            }
        }
        else if (input != null && input.getClass().isArray()) {
            final Object[] array = (Object[]) input;
            buffer.append(toString(Arrays.asList(array), separator));
        }
        else {
            buffer.append(String.valueOf(input));
        }
        return buffer.toString();
    }

    public static String toStringByAnnotation(final Object o) {
        return toStringByAnnotation(o, o.getClass().getSimpleName());
    }

    @SuppressWarnings("rawtypes")
    public static String toStringByAnnotation(final Object o, final String prefix) {
        final StringBuilder printString = new StringBuilder();
        if (prefix != null) {
            printString.append(prefix).append("[");
        }
        else {
            printString.append("[");
        }
        final Class c = o.getClass();
        printString.append(getStringPartFieldValue(o, c));
        for (Class superClass = c.getSuperclass(); superClass != null; superClass = superClass.getSuperclass()) {
            printString.append(", ");
            printString.append(getStringPartFieldValue(o, superClass));
        }
        printString.append("]");
        return printString.toString();
    }

    @SuppressWarnings("rawtypes")
    private static String getStringPartFieldValue(final Object o, final Class c) {
        final StringBuilder retval = new StringBuilder();
        for (final Field f : c.getDeclaredFields()) {
            if (f.isAnnotationPresent(StringPart.class)) {
                final boolean accessible = f.isAccessible();
                try {
                    f.setAccessible(true);
                    // Method m = f.getType().getMethod("toString");
                    final Object val = f.get(o);
                    // String value = (String) m.invoke(val);
                    final StringPart annotation = f.getAnnotation(StringPart.class);
                    if (annotation.name() == null || annotation.name().length() == 0) {
                        retval.append(f.getName());
                    }
                    else {
                        retval.append(annotation.name());
                    }
                    retval.append("=").append(val).append(", ");
                }
                catch (final Exception e) {
                    e.printStackTrace();
                }
                finally {
                    f.setAccessible(accessible);
                }
            }
        }
        return retval.length() > 2 ? retval.substring(0, retval.length() - 2) : retval.toString();
    }

    public static class Null implements Serializable {

        private static final long serialVersionUID = 7092611880189329093L;

        Null() {
            super();
        }

        private Object readResolve() {
            return Objects.NULL;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T narrow(Class<T> clazz, Object obj) {
        if (obj == null || clazz.isAssignableFrom(obj.getClass())) {
            return (T) obj;
        }
        else {
            throw Exceptions.make(IllegalArgumentException.class, "Can't cast instance of %s to %s", obj.getClass(), clazz);
        }
    }

    public static Class<?>[] getAllInterfaces(Class<?> clazz) {
        List<Class<?>> interfaces = new LinkedList<Class<?>>();
        while (!clazz.equals(Object.class)) {
            Collections.addAll(interfaces, clazz.getInterfaces());
            clazz = clazz.getSuperclass();
        }
        return interfaces.toArray(new Class<?>[interfaces.size()]);
    }

    public static List<Class<?>> getAllSuperclasses(Class<?> cls) {
        if (cls == null) {
            return null;
        }
        List<Class<?>> classes = new ArrayList<Class<?>>();
        Class<?> superclass = cls.getSuperclass();
        while (superclass != null) {
            classes.add(superclass);
            superclass = superclass.getSuperclass();
        }
        return classes;
    }
}
