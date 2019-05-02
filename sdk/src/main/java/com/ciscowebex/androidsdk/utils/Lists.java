package com.ciscowebex.androidsdk.utils;

import java.util.List;

public final class Lists {

    private Lists() {
    }

    /**
     * Returns the first item in the given list, or null if not found.
     *
     * @param <T> The generic list type.
     * @param list The list that may have a first item.
     *
     * @return null if the list is null or there is no first item.
     */
    public static <T> T getFirst( final List<T> list ) {
        return getFirst( list, null );
    }

    /**
     * Returns the last item in the given list, or null if not found.
     *
     * @param <T> The generic list type.
     * @param list The list that may have a last item.
     *
     * @return null if the list is null or there is no last item.
     */
    public static <T> T getLast( final List<T> list ) {
        return getLast( list, null );
    }

    /**
     * Returns the first item in the given list, or t if not found.
     *
     * @param <T> The generic list type.
     * @param list The list that may have a first item.
     * @param t The default return value.
     *
     * @return null if the list is null or there is no first item.
     */
    public static <T> T getFirst( final List<T> list, final T t ) {
        return isEmpty( list ) ? t : list.get( 0 );
    }

    /**
     * Returns the last item in the given list, or t if not found.
     *
     * @param <T> The generic list type.
     * @param list The list that may have a last item.
     * @param t The default return value.
     *
     * @return null if the list is null or there is no last item.
     */
    public static <T> T getLast( final List<T> list, final T t ) {
        return isEmpty( list ) ? t : list.get( list.size() - 1 );
    }

    /**
     * Returns true if the given list is null or empty.
     *
     * @param <T> The generic list type.
     * @param list The list that has a last item.
     *
     * @return true The list is empty.
     */
    public static <T> boolean isEmpty( final List<T> list ) {
        return list == null || list.isEmpty();
    }
}