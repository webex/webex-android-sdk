package com.cisco.spark.android.sync.operationqueue;

import android.support.annotation.NonNull;

import com.cisco.spark.android.authenticator.ApiTokenProvider;
import com.cisco.spark.android.authenticator.AuthenticatedUserProvider;
import com.cisco.spark.android.authenticator.NotAuthenticatedException;
import com.cisco.spark.android.authenticator.OAuth2;
import com.cisco.spark.android.authenticator.OAuth2AccessToken;
import com.cisco.spark.android.authenticator.OAuth2Client;
import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.AuthenticatedUser;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.sync.ConversationContract;
import com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncState;
import com.cisco.spark.android.sync.operationqueue.core.Operation;
import com.cisco.spark.android.sync.operationqueue.core.OperationQueue;
import com.cisco.spark.android.sync.operationqueue.core.RetryPolicy;
import com.github.benoitdion.ln.Ln;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import retrofit2.Response;

public class TokenRefreshOperation extends AbstractOAuth2Operation {

    private final String reason;
    @Inject
    transient ApiTokenProvider apiTokenProvider;
    @Inject
    transient AuthenticatedUserProvider authenticatedUserProvider;
    @Inject
    transient OAuth2 oAuth2;
    @Inject
    transient ApiClientProvider apiClientProvider;
    @Inject
    transient OperationQueue operationQueue;
    @Inject
    transient EventBus bus;

    private String oldToken;

    public TokenRefreshOperation(Injector injector, String reason) {
        super(injector);
        this.reason = reason;

        populateOldToken();
    }

    @NonNull
    @Override
    public ConversationContract.SyncOperationEntry.OperationType getOperationType() {
        return ConversationContract.SyncOperationEntry.OperationType.REFRESH_TOKEN;
    }

    @NonNull
    @Override
    public SyncState onPrepare() {
        if (!apiTokenProvider.isAuthenticated() && oldToken == null)
            return SyncState.FAULTED;

        populateOldToken();

        return super.onPrepare();
    }

    @NonNull
    @Override
    protected SyncState doWork() throws IOException {
        if (!authenticatedUserProvider.isAuthenticated()) {
            return SyncState.FAULTED;
        }

        ln.i("Updating OAuth token. Trigger: " + reason);
        AuthenticatedUser user = authenticatedUserProvider.getAuthenticatedUser();

        try {

            //LM

            /*
            OAuth2Client oAuthClient = apiClientProvider.getOAuthClient();
            Response<OAuth2AccessToken> response = oAuth2.refreshTokens(oAuthClient, user.getOAuth2Tokens().getRefreshToken());
            OAuth2AccessToken newToken = getTokenFromResponse(response);
            if (newToken == null) {
                if (response.code() == HttpURLConnection.HTTP_BAD_REQUEST)
                    triggerLogout(response);
                return SyncState.READY;
            }

            user.setTokens(newToken);

            reduceScopeIfNeeded(user.getOAuth2Tokens());

            */

            return SyncState.SUCCEEDED;
        } catch (NotAuthenticatedException e) {
            Ln.w(e, "Failed refreshing tokens");
        }

        return SyncState.READY;
    }

    @Override
    public String toString() {
        return super.toString() + " [" + reason + "]";
    }

    @Override
    public boolean isOperationRedundant(Operation newOperation) {
        return newOperation.getOperationType() == getOperationType();
    }

    @Override
    protected void onNewOperationEnqueued(Operation newOperation) {
        if (isOperationRedundant(newOperation))
            newOperation.cancel();
    }

    @NonNull
    @Override
    protected SyncState checkProgress() {
        return getState();
    }

    private void populateOldToken() {
        if (oldToken == null && apiTokenProvider.isAuthenticated()) {
            AuthenticatedUser user = authenticatedUserProvider.getAuthenticatedUser();
            if (user != null && user.getOAuth2Tokens() != null) {
                oldToken = user.getOAuth2Tokens().getAccessToken();
            }
        }
    }

    @NonNull
    @Override
    public RetryPolicy buildRetryPolicy() {
        return RetryPolicy.newLimitAttemptsPolicy(5)
                .withAttemptTimeout(10, TimeUnit.SECONDS)
                .withExponentialBackoff();
    }
}
