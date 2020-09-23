/*
 * Copyright 2016-2020 Cisco Systems Inc
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.ciscowebex.androidsdk.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class Lists {

    private Lists() {
    }

    @SafeVarargs
    public static <T> ArrayList<T> asList(T... array) {
        ArrayList<T> list = new ArrayList<>();
        Collections.addAll(list, array);
        return list;
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

    public static <T> boolean isEquals(Collection<T> left, Collection<T> right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        if (left.size() != right.size()) {
            return false;
        }
        for (T e : left) {
            if (!right.contains(e)) {
                return false;
            }
        }
        return true;
    }
}