package com.ciscospark.androidsdk.utils.http;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by zhiyuliu on 31/08/2017.
 */

public class DefaultHeadersInterceptor implements Interceptor {
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();
        Request request = original.newBuilder()
                .header("User-Agent", userAgent())
                .header("Spark-User-Agent", userAgent())
                .header("Content-Type", "application/json; charset=utf-8")
                .method(original.method(), original.body())
                .build();

        return chain.proceed(request);
    }

    // TODO
    private static String userAgent() {
        return "spark_android_sdk/0.9";
    }
}
