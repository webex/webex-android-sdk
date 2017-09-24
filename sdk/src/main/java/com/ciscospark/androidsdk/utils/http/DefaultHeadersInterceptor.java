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

package com.ciscospark.androidsdk.utils.http;

import java.io.IOException;

import android.os.Build;
import com.cisco.spark.android.util.Strings;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class DefaultHeadersInterceptor implements Interceptor {
	
	public static final String APP_NAME = "spark_android_sdk";

	public static final String APP_VERSION = "0.0.1";
	
    protected String _userAgent;
    
    public DefaultHeadersInterceptor() {
        String tempUserAgent = String.format("%s/%s (Android %s; %s %s / %s %s;)",
	        APP_NAME, APP_VERSION,
            Build.VERSION.RELEASE,
            Strings.capitalize(Build.MANUFACTURER),
            Strings.capitalize(Build.DEVICE),
            Strings.capitalize(Build.BRAND),
            Strings.capitalize(Build.MODEL)
        );
        _userAgent = Strings.stripInvalidHeaderChars(tempUserAgent);
    }
    
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();
        Request request = original.newBuilder()
                .header("User-Agent", _userAgent)
                .header("Spark-User-Agent", _userAgent)
                .header("Content-Type", "application/json; charset=utf-8")
                .method(original.method(), original.body())
                .build();

        return chain.proceed(request);
    }
}
