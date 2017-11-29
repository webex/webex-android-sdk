/*
 * Copyright 2016-2017 Cisco Systems Inc
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

package com.ciscospark.androidsdk.utils;

import android.os.Build;
import android.support.annotation.Nullable;
import com.cisco.spark.android.util.Strings;
import com.ciscospark.androidsdk.Spark;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 * An utility class.
 *
 * @since 0.1
 */

public class Utils {
    public static <T> T checkNotNull(@Nullable T object, String message) {
        if (object == null) {
            throw new NullPointerException(message);
        }
        return object;
    }
    
    public static String timestampUTC() {
        return DateTime.now( DateTimeZone.UTC ).toString();
    }
    
    public static String versionInfo() {
        String tempUserAgent = String.format("%s/%s (Android %s; %s %s / %s %s;)",
            Spark.APP_NAME, Spark.APP_VERSION,
            Build.VERSION.RELEASE,
            Strings.capitalize(Build.MANUFACTURER),
            Strings.capitalize(Build.DEVICE),
            Strings.capitalize(Build.BRAND),
            Strings.capitalize(Build.MODEL)
        );
        return Strings.stripInvalidHeaderChars(tempUserAgent);
    }
    
}
