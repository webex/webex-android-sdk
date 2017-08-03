package com.cisco.spark.android.authenticator;

import android.content.Context;
import android.net.Uri;

import com.cisco.spark.android.client.IdentityClient;
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

public class IdentityClientPreLoginProvider extends BaseApiClientProvider {
    private UrlProvider urlProvider;
    private OAuth2Tokens oAuth2Tokens;
    private IdentityClient identityClient;

    public IdentityClientPreLoginProvider(UserAgentProvider userAgentProvider, TrackingIdGenerator trackingIdGenerator, Gson gson, EventBus bus, Settings settings, Context context, Ln.Context lnContext, Provider<OkHttpClient.Builder> okHttpClientBuilderProvider, Lazy<OperationQueue> operationQueue, UrlProvider urlProvider) {
        super(userAgentProvider, trackingIdGenerator, gson, bus, settings, context, lnContext, okHttpClientBuilderProvider, operationQueue, new SquaredCertificatePinner(context, new HashSet<>(Arrays.asList(urlProvider.getAtlasUrl())), bus, urlProvider));

        this.urlProvider = urlProvider;
    }

    public void setOAuth2Tokens(OAuth2Tokens oAuth2Tokens) {
        this.oAuth2Tokens = oAuth2Tokens;
    }

    public synchronized IdentityClient getIdentityClient() {
        if (oAuth2Tokens == null) {
            throw new IllegalStateException("Access token not set");
        }

        if (identityClient == null) {
            identityClient = retrofit(buildOkHttpClient(Uri.parse(urlProvider.getIdentityApiUrl()), null))
                    .baseUrl(urlProvider.getIdentityApiUrl())
                    .build()
                    .create(IdentityClient.class);
        }
        return identityClient;
    }

    @Override
    protected String getAuthHeader() {
        return String.format("Bearer %s", oAuth2Tokens.getAccessToken());
    }

    @Override
    protected boolean shouldRefreshTokensNow() {
        return false;
    }
}
