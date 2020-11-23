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

package com.ciscowebex.androidsdk.utils.http;

import android.support.annotation.NonNull;

import com.ciscowebex.androidsdk.internal.ServiceReqeust;
import com.ciscowebex.androidsdk.utils.NetworkUtils;
import com.github.benoitdion.ln.Ln;

import okhttp3.*;
import okhttp3.logging.HttpLoggingInterceptor;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class HttpClient {

    private static final int MAX_LENGTH = 1024;

    private static HttpLoggingInterceptor LOGGING_INTERCEPTOR = new HttpLoggingInterceptor(message -> {
        if (message.length() > MAX_LENGTH) {
            int chunkCount = message.length() / MAX_LENGTH;
            for (int i = 0; i <= chunkCount; i++) {
                int max = MAX_LENGTH * (i + 1);
                if (max >= message.length()) {
                    Ln.d("[HTTP] " + message.substring(MAX_LENGTH * i));
                } else {
                    Ln.d("[HTTP] " + message.substring(MAX_LENGTH * i, max));
                }
            }
        } else {
            Ln.d("[HTTP] " + message);
        }
    });

    public static ConnectionSpec TLS_SPEC = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
            .tlsVersions(TlsVersion.TLS_1_2)
            .cipherSuites(CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256, CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256, CipherSuite.TLS_DHE_RSA_WITH_AES_128_GCM_SHA256)
            .build();

    public static void setLogLevel(HttpLoggingInterceptor.Level level) {
        LOGGING_INTERCEPTOR.setLevel(level);
    }

    public static @NonNull
    OkHttpClient defaultClient = newClient().build();

    public static OkHttpClient.Builder newClient() {
        return new OkHttpClient.Builder()
                .addInterceptor(new DefaultHeadersInterceptor())
                .followRedirects(false)
                .followSslRedirects(false)
                .addInterceptor(chain -> {
                    Request request = chain.request();
                    Response response = chain.proceed(request);
                    if (response.code() >= 300 && response.code() <= 399) {
                        String url = Objects.requireNonNull(response.header("Location"));
                        okhttp3.Request.Builder requestBuilder = request.newBuilder().url(url);
                        if (!url.contains("wbx2.com") && !url.contains("ciscospark.com") && !url.contains("webex.com")) {
                            requestBuilder.removeHeader("Spark-User-Agent");
                            requestBuilder.removeHeader("Cisco-Request-ID");
                            requestBuilder.removeHeader(ServiceReqeust.HEADER_TRACKING_ID);
                        }
                        request = requestBuilder.build();
                        Ln.i("Handling redirect, url = " + request.url());
                        response = chain.proceed(request);
                    }
                    return response;
                })
                .addInterceptor(new Interceptor() {

                    private final Object lock = new Object();

                    @Override
                    public Response intercept(@NotNull Chain chain) throws IOException {
                        Request request = chain.request();
                        Response response = chain.proceed(request);
                        if (response.code() == 429) {
                            int retrySeconds = NetworkUtils.get429RetryAfterSeconds(response, 5, 3600);
                            if (retrySeconds > 0) {
                                synchronized (lock) {
                                    try {
                                        lock.wait(retrySeconds * 1000);
                                    } catch (Throwable ignored) {
                                    }
                                }
                                response = chain.proceed(request);
                            }
                        }
                        return response;
                    }
                })
                .addInterceptor(HttpClient.LOGGING_INTERCEPTOR)
                .readTimeout(3, TimeUnit.MINUTES)
                .writeTimeout(60, TimeUnit.SECONDS)
                .connectTimeout(3, TimeUnit.MINUTES);
    }

}
