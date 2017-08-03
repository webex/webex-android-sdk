package com.cisco.spark.android.voicemail;


import android.content.Context;
import android.net.Uri;

import com.cisco.spark.android.authenticator.AuthenticatedUserProvider;
import com.cisco.spark.android.authenticator.NotAuthenticatedException;
import com.cisco.spark.android.client.TrackingIdGenerator;
import com.cisco.spark.android.client.UrlProvider;
import com.cisco.spark.android.core.AuthenticatedUser;
import com.cisco.spark.android.core.BaseApiClientProvider;
import com.cisco.spark.android.core.Settings;
import com.cisco.spark.android.core.SquaredCertificatePinner;
import com.cisco.spark.android.sync.operationqueue.core.OperationQueue;
import com.cisco.spark.android.util.UserAgentProvider;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.github.benoitdion.ln.Ln;
import com.github.benoitdion.ln.NaturalLog;
import com.google.gson.Gson;

import java.util.Arrays;
import java.util.HashSet;

import javax.inject.Provider;

import dagger.Lazy;
import de.greenrobot.event.EventBus;
import okhttp3.OkHttpClient;

public class VoicemailClientProvider extends BaseApiClientProvider {
    protected final DeviceRegistration deviceRegistration;
    protected final NaturalLog ln;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    private VoicemailClient voicemailClient;


    public VoicemailClientProvider(UserAgentProvider userAgentProvider, TrackingIdGenerator trackingIdGenerator, Gson gson, EventBus bus, Settings settings, Context context, Ln.Context lnContext, Provider<OkHttpClient.Builder> okHttpClientBuilderProvider, Lazy<OperationQueue> operationQueue, UrlProvider urlProvider, DeviceRegistration deviceRegistration, AuthenticatedUserProvider authenticatedUserProvider) {
        super(userAgentProvider, trackingIdGenerator, gson, bus, settings, context, lnContext, okHttpClientBuilderProvider, operationQueue, new SquaredCertificatePinner(context, new HashSet<>(Arrays.asList(urlProvider.getAtlasUrl())), bus, urlProvider));

        this.deviceRegistration = deviceRegistration;
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.ln = Ln.get(lnContext, "Voicemail");
    }

    public synchronized VoicemailClient getVoicemailClient() {
        if (voicemailClient == null) {
            Uri url = deviceRegistration.getVoicemailServiceUrl();
            if (url == null) {
                ln.w("Voicmail service url is null");
                throw new NotAuthenticatedException();
            }
            url = Uri.withAppendedPath(url, "");
            voicemailClient = retrofit(url, deviceRegistration).baseUrl(url.toString()).build().create(VoicemailClient.class);
        }

        return voicemailClient;
    }

    @Override
    protected String getAuthHeader() {
        AuthenticatedUser user = authenticatedUserProvider.getAuthenticatedUserOrNull();
        if (user != null)
            return user.getVoicemailAuthorizationHeader();
        return null;
    }

    @Override
    protected boolean shouldRefreshTokensNow() {
        if (authenticatedUserProvider.isAuthenticated()) {
            AuthenticatedUser user = authenticatedUserProvider.getAuthenticatedUser();
            if (user != null && user.getOAuth2Tokens() != null) {
                return user.getOAuth2Tokens().shouldRefreshNow();
            }
        }
        return false;
    }
}
