package com.cisco.spark.android.authenticator.model;

import android.content.Context;
import android.net.Uri;

import com.cisco.spark.android.authenticator.IdbrokerTokenClient;
import com.cisco.spark.android.client.TrackingIdGenerator;
import com.cisco.spark.android.client.UrlProvider;
import com.cisco.spark.android.core.BaseApiClientProvider;
import com.cisco.spark.android.core.Settings;
import com.cisco.spark.android.core.SquaredCertificatePinner;
import com.cisco.spark.android.sync.operationqueue.core.OperationQueue;
import com.cisco.spark.android.util.UserAgentProvider;
import com.github.benoitdion.ln.Ln;
import com.google.gson.Gson;

import java.util.Arrays;
import java.util.HashSet;

import javax.inject.Provider;

import dagger.Lazy;
import de.greenrobot.event.EventBus;
import okhttp3.OkHttpClient;

public class IdbrokerTokenClientProvider extends BaseApiClientProvider {
    private String authHeader;
    private UrlProvider urlProvider;
    private IdbrokerTokenClient idbrokerTokenClient;

    public IdbrokerTokenClientProvider(UserAgentProvider userAgentProvider, TrackingIdGenerator trackingIdGenerator, Gson gson, EventBus bus, Settings settings, Context context, Ln.Context lnContext, Provider<OkHttpClient.Builder> okHttpClientBuilderProvider, Lazy<OperationQueue> operationQueue, UrlProvider urlProvider) {
        super(userAgentProvider, trackingIdGenerator, gson, bus, settings, context, lnContext, okHttpClientBuilderProvider, operationQueue, new SquaredCertificatePinner(context, new HashSet<>(Arrays.asList(urlProvider.getAtlasUrl())), bus, urlProvider));

        this.urlProvider = urlProvider;
    }

    public void setAuthHeader(String authHeader) {
        this.authHeader = authHeader;
    }

    public synchronized IdbrokerTokenClient getIdbrokerTokenClient() {
        if (authHeader == null) {
            throw new IllegalStateException("Auth header not set");
        }

        if (idbrokerTokenClient == null) {
            idbrokerTokenClient = retrofit(buildOkHttpClient(Uri.parse(urlProvider.getIdbrokerTokenUrl()), null))
                    .baseUrl(urlProvider.getIdbrokerTokenUrl())
                    .build()
                    .create(IdbrokerTokenClient.class);
        }

        return idbrokerTokenClient;
    }

    @Override
    protected String getAuthHeader() {
        return authHeader;
    }

    @Override
    protected boolean shouldRefreshTokensNow() {
        return false;
    }
}
