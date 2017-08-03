package com.cisco.spark.android.provisioning;

import android.content.Context;

import com.cisco.spark.android.authenticator.OAuth2Tokens;
import com.cisco.spark.android.client.TrackingIdGenerator;
import com.cisco.spark.android.client.UrlProvider;
import com.cisco.spark.android.core.Settings;
import com.cisco.spark.android.sync.operationqueue.core.OperationQueue;
import com.cisco.spark.android.util.UserAgentProvider;
import com.github.benoitdion.ln.Ln;
import com.google.gson.Gson;

import javax.inject.Provider;

import dagger.Lazy;
import de.greenrobot.event.EventBus;
import okhttp3.OkHttpClient;

public class ProvisioningClientWithUserTokenProvider extends ProvisioningClientProvider {

    private OAuth2Tokens userToken;

    public ProvisioningClientWithUserTokenProvider(UserAgentProvider userAgentProvider, TrackingIdGenerator trackingIdGenerator, Gson gson, EventBus bus, Settings settings, Context context, Ln.Context lnContext, Provider<OkHttpClient.Builder> okHttpClientBuilderProvider, Lazy<OperationQueue> operationQueue, UrlProvider urlProvider) {
        super(userAgentProvider, trackingIdGenerator, gson, bus, settings, context, lnContext, okHttpClientBuilderProvider, operationQueue, urlProvider);
    }

    public void setUserToken(OAuth2Tokens userToken) {
        this.userToken = userToken;
        setAccessToken(userToken.getAccessToken());
    }

    public OAuth2Tokens getUserToken() {
        return userToken;
    }
}
