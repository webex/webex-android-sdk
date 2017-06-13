package com.cisco.spark.android.provisioning;

import android.content.Context;

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
import retrofit2.Retrofit;

public class ProvisioningClientProvider extends BaseApiClientProvider {

    protected String accessToken;

    private UrlProvider urlProvider;

    private ProvisioningClient provisioningClient;

    public ProvisioningClientProvider(UserAgentProvider userAgentProvider, TrackingIdGenerator trackingIdGenerator, Gson gson, EventBus bus, Settings settings, Context context, Ln.Context lnContext, Provider<OkHttpClient.Builder> okHttpClientBuilderProvider, Lazy<OperationQueue> operationQueue, UrlProvider urlProvider) {
        super(userAgentProvider, trackingIdGenerator, gson, bus, settings, context, lnContext, okHttpClientBuilderProvider, operationQueue, new SquaredCertificatePinner(context, new HashSet<>(Arrays.asList(urlProvider.getAtlasUrl())), bus, urlProvider));

        this.urlProvider = urlProvider;
    }


    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public synchronized ProvisioningClient getProvisioningClient() {
        if (accessToken == null) {
            throw new IllegalStateException("Access token not set");
        }

        if (provisioningClient == null) {
            Retrofit retrofit = retrofit(buildOkHttpClient(null)).baseUrl(urlProvider.getAtlasUrl()).build();
            provisioningClient = retrofit.create(ProvisioningClient.class);
        }

        return provisioningClient;
    }

    @Override
    protected String getAuthHeader() {
        return String.format("Bearer %s", getAccessToken());
    }

    @Override
    protected boolean shouldRefreshTokensNow() {
        return false;
    }


}
