package com.ciscowebex.androidsdk.auth.internal;

import android.support.annotation.NonNull;
import com.cisco.spark.android.authenticator.ApiTokenProvider;
import com.cisco.spark.android.authenticator.OAuth2Tokens;
import com.cisco.spark.android.core.ApplicationController;
import com.cisco.spark.android.model.AuthenticatedUser;
import com.cisco.spark.android.model.conversation.ActorRecord;
import com.ciscowebex.androidsdk.CompletionHandler;
import com.ciscowebex.androidsdk.auth.Authenticator;
import com.ciscowebex.androidsdk.internal.ResultImpl;
import com.ciscowebex.androidsdk_commlib.AfterInjected;
import com.github.benoitdion.ln.Ln;

import javax.inject.Inject;

public class TokenAuthenticator implements Authenticator {

    @Inject
    ApiTokenProvider _provider;

    @Inject
    ApplicationController _applicationController;

    private OAuth2Tokens _token = null;

    public TokenAuthenticator() {
    }

    public void authorize(@NonNull String token, int expire) {
        deauthorize();
        _token = new OAuth2Tokens();
        _token.setAccessToken(token);
        _token.setExpiresIn(expire + (System.currentTimeMillis() / 1000));
        _token.setRefreshToken(token);
        if (_provider != null) {
            AuthenticatedUser authenticatedUser = new AuthenticatedUser("", new ActorRecord.ActorKey(""), "", _token, "Unknown", null, 0, null);
            _provider.setAuthenticatedUser(authenticatedUser);
        }
    }

    @Override
    public boolean isAuthorized() {
        return _token != null && _token.getAccessToken() != null && System.currentTimeMillis() < (_token.getExpiresIn() * 1000);
    }

    @Override
    public void deauthorize() {
        _token = null;
        if (_applicationController != null) {
            Ln.d("deauthorize() clear all!");
            _applicationController.clear();
        }
    }

    @Override
    public void getToken(CompletionHandler<String> handler) {
        handler.onComplete(ResultImpl.success(_token.getAccessToken()));
    }

    @Override
    public void refreshToken(CompletionHandler<String> handler) {
        handler.onComplete(ResultImpl.success(_token.getRefreshToken()));
    }

    @AfterInjected
    private void afterInjected() {
        if (_provider != null && _token != null) {
            AuthenticatedUser authenticatedUser = new AuthenticatedUser("", new ActorRecord.ActorKey(""), "", _token, "Unknown", null, 0, null);
            _provider.setAuthenticatedUser(authenticatedUser);
        }
    }

}
