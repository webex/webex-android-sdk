package com.ciscospark.androidsdk.utils.reflect;

import com.ciscospark.androidsdk.utils.Objects;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Created by zhiyuliu on 01/09/2017.
 */

public class Fields {

    public static Field findField(Class<?> cls, String fieldName) {
        Field field = findField(cls, fieldName, false);
        Members.setAccessibleWorkaround(field);
        return field;
    }

    public static Field findField(final Class<?> cls, String fieldName, boolean forceAccess) {
        if (cls == null) {
            return null;
        }
        if (fieldName == null) {
            return null;
        }
        for (Class<?> acls = cls; acls != null; acls = acls.getSuperclass()) {
            try {
                Field field = acls.getDeclaredField(fieldName);
                if (!Modifier.isPublic(field.getModifiers())) {
                    if (forceAccess) {
                        field.setAccessible(true);
                    }
                    else {
                        continue;
                    }
                }
                return field;
            }
            catch (NoSuchFieldException ignored) {
            }
        }
        Field match = null;
        for (Class<?> class1 : Objects.getAllInterfaces(cls)) {
            try {
                Field test = class1.getField(fieldName);
                if (match != null) {
                    throw new IllegalArgumentException("Reference to field " + fieldName + " is ambiguous relative to " + cls
                            + "; a matching field exists on two or more implemented interfaces.");
                }
                match = test;
            }
            catch (NoSuchFieldException ex) { // NOPMD
                // ignore
            }
        }
        return match;
    }

    public static Field findDeclaredField(Class<?> cls, String fieldName) {
        return findDeclaredField(cls, fieldName, false);
    }

    public static Field findDeclaredField(Class<?> cls, String fieldName, boolean forceAccess) {
        if (cls == null) {
            return null;
        }
        if (fieldName == null) {
            return null;
        }
        try {
            Field field = cls.getDeclaredField(fieldName);
            if (!Members.isAccessible(field)) {
                if (forceAccess) {
                    field.setAccessible(true);
                }
                else {
                    return null;
                }
            }
            return field;
        }
        catch (NoSuchFieldException e) { // NOPMD
            // ignore
        }
        return null;
    }
}
