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

import android.support.annotation.NonNull;
import com.ciscowebex.androidsdk.message.Before;
import com.ciscowebex.androidsdk.message.Mention;

import java.util.Objects;

/**
 * A data type represents an email address with validation and equatable implementation.
 *
 * @since 2.1.0
 */
public class EmailAddress {

    private String address = null;

    private EmailAddress(String address) {
        this.address = address;
    }

    /**
     * Creates an *EmailAddress* object from a string

     * @param address The email address string.
     * @return EmailAddress
     * @since 2.1
     */
    public static EmailAddress fromString(@NonNull String address) {
        if (isValid(address)) {
            return new EmailAddress(address);
        }
        return null;
    }

    @Override
    public String toString() {
        return address;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof EmailAddress) {
            return address.equals(((EmailAddress) o).address);
        }
        else if (o instanceof String) {
            return address.equals(o);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(address);
    }

    private static boolean isValid(String address) {
        String ePattern = "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}])|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(ePattern);
        java.util.regex.Matcher m = p.matcher(address);
        return m.matches();
    }
}
