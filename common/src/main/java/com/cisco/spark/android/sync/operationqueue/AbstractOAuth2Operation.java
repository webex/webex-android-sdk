package com.cisco.spark.android.sync.operationqueue;

import android.support.annotation.NonNull;

import com.cisco.spark.android.authenticator.ApiTokenProvider;
import com.cisco.spark.android.authenticator.AuthenticatedUserProvider;
import com.cisco.spark.android.authenticator.NotAuthenticatedException;
import com.cisco.spark.android.authenticator.OAuth2;
import com.cisco.spark.android.authenticator.OAuth2AccessToken;
import com.cisco.spark.android.authenticator.OAuth2Client;
import com.cisco.spark.android.authenticator.OAuth2Tokens;
import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.events.OAuth2ErrorResponseEvent;
import com.cisco.spark.android.sdk.SdkClient;
import com.cisco.spark.android.sync.ConversationContract;
import com.cisco.spark.android.sync.operationqueue.core.Operation;
import com.github.benoitdion.ln.Ln;

import java.net.HttpURLConnection;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import retrofit2.Response;

public abstract class AbstractOAuth2Operation extends Operation {

    @Inject
    transient EventBus bus;
    @Inject
    transient ApiTokenProvider apiTokenProvider;
    @Inject
    transient AuthenticatedUserProvider authenticatedUserProvider;
    @Inject
    transient OAuth2 oAuth2;
    @Inject
    transient ApiClientProvider apiClientProvider;
    @Inject
    transient SdkClient sdkClient;

    public AbstractOAuth2Operation(Injector injector) {
        super(injector);
    }

    @NonNull
    @Override
    protected ConversationContract.SyncOperationEntry.SyncState onEnqueue() {
        return ConversationContract.SyncOperationEntry.SyncState.READY;
    }


    protected void reduceScopeIfNeeded(OAuth2Tokens newToken) {
        OAuth2Client oAuthClient = apiClientProvider.getOAuthClient();

        updateReducedScopeTokens(oAuthClient, newToken);
    }

    protected void updateReducedScopeTokens(OAuth2Client oAuthClient, OAuth2AccessToken newToken) {
        OAuth2AccessToken kmsTokens = null;
        OAuth2AccessToken conversationTokens = null;
        OAuth2AccessToken voicemailTokens = null;
        try {
            if (sdkClient.supportsHybridKms()) {
                kmsTokens = getTokenFromResponse(oAuth2.getKmsTokens(oAuthClient, newToken));
                conversationTokens = getTokenFromResponse(oAuth2.getConversationTokens(oAuthClient, newToken));

                if (sdkClient.supportsVoicemailScopes()) {
                    voicemailTokens = getTokenFromResponse(oAuth2.getVoicemailTokens(oAuthClient, newToken));

                    if (voicemailTokens == null) {
                        Ln.w("Voicemail Scope-reduction failed");
                    }
                }
            }

        } catch (Exception e) {
            Ln.e(e);
        }
        Ln.w("Scope-reduction: " + (conversationTokens == null) + " " + (kmsTokens == null) + " " + (voicemailTokens == null));
        authenticatedUserProvider.updateTokens(newToken, conversationTokens, kmsTokens, voicemailTokens);
    }

    protected OAuth2AccessToken getTokenFromResponse(Response<OAuth2AccessToken> response) {
        if (response.isSuccessful())
            return response.body();

        if (response.code() == HttpURLConnection.HTTP_UNAUTHORIZED
                || response.code() == HttpURLConnection.HTTP_FORBIDDEN) {
            return triggerLogout(response);
        }

        return null;
    }

    protected OAuth2AccessToken triggerLogout(Response<OAuth2AccessToken> response) {
        bus.post(new OAuth2ErrorResponseEvent(response));
        setFailureReason(ConversationContract.SyncOperationEntry.SyncStateFailureReason.EXCEPTION);
        setErrorMessage("401 Unauthorized requesting new token");
        throw new NotAuthenticatedException();
    }

    @Override
    public boolean requiresAuth() {
        return false;
    }

    @Override
    public boolean shouldPersist() {
        return false;
    }
}
