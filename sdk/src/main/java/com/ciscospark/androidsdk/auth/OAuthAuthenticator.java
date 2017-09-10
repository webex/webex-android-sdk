/*
 * Copyright (c) 2016-2017 Cisco Systems Inc
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

package com.ciscospark.androidsdk.auth;


import android.support.annotation.Nullable;

import com.cisco.spark.android.authenticator.ApiTokenProvider;
import com.cisco.spark.android.authenticator.OAuth2Tokens;
import com.cisco.spark.android.core.AuthenticatedUser;
import com.cisco.spark.android.sync.ActorRecord;
import com.ciscospark.androidsdk.CompletionHandler;
import com.ciscospark.androidsdk.Result;
import com.ciscospark.androidsdk.utils.Checker;
import com.ciscospark.androidsdk.utils.http.ServiceBuilder;

import javax.inject.Inject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

import static com.ciscospark.androidsdk.utils.Utils.checkNotNull;

/**
 * OAuth2 strategy using granted auth code.
 *
 * @author Allen Xiao<xionxiao@cisco.com>
 * @version 0.1
 */
public class OAuthAuthenticator implements Authenticator {

    private static final String AUTHORIZATION_CODE = "authorization_code";

    private String _clientId;
    private String _clientSecret;
    private String _scope;
    private String _redirectUri;

    private OAuth2Tokens _token;
    private AuthService _authService;

    @Inject
    ApiTokenProvider _provider;

    /**
     * OAuth 2 authorize strategy.
     *
     * @param clientId
     * @param clientSecret
     * @param scope
     * @param redirectUri
     */
    public OAuthAuthenticator(String clientId, String clientSecret, String scope, String redirectUri) {
        _clientId = clientId;
        _clientSecret = clientSecret;
        _redirectUri = redirectUri;
        _scope = scope;
        _authService = new ServiceBuilder().build(AuthService.class);
    }

    @Override
    public boolean isAuthorized() {
        return getToken() != null;
    }

    @Override
    public void deauthorize() {
        _token = null;
        if ( _provider != null) {
            _provider.clearAccount();
        }
    }

    public void authorize(String code, CompletionHandler<Void> handler) {
        checkNotNull(handler, "CompletionHandler is null");
        _authService.getToken(_clientId, _clientSecret, _redirectUri, AUTHORIZATION_CODE, code).enqueue(new Callback<OAuth2Tokens>() {
            @Override
            public void onResponse(Call<OAuth2Tokens> call, Response<OAuth2Tokens> response) {
                _token = response.body();
                if (_token == null || _token.getAccessToken() == null || _token.getAccessToken().isEmpty()) {
                    handler.onComplete(Result.error(response));
                }
                else {
                    if (_provider != null) {
                        AuthenticatedUser authenticatedUser = new AuthenticatedUser("", new ActorRecord.ActorKey(""), "", _token, "Unknown", null, 0, null);
                        _provider.setAuthenticatedUser(authenticatedUser);
                    }
                    handler.onComplete(Result.success(null));
                }
            }

            @Override
            public void onFailure(Call<OAuth2Tokens> call, Throwable t) {
                handler.onComplete(Result.error(t));
            }
        });
    }

    @Override
    public void getToken(CompletionHandler<String> handler) {
        checkNotNull(handler, "CompletionHandler is null");
        OAuth2Tokens token = getToken();
        if (token == null) {
            handler.onComplete(Result.error("Not authorized"));
            return;
        }
        if (Checker.isEmpty(token.getAccessToken()) && token.getExpiresIn() > (System.currentTimeMillis() / 1000) + (15 * 60)) {
            handler.onComplete(Result.success(token.getAccessToken()));
            return;
        }
        _authService.refreshToken(_clientId, _clientSecret, token.getRefreshToken(), AUTHORIZATION_CODE).enqueue(new Callback<OAuth2Tokens>() {
            @Override
            public void onResponse(Call<OAuth2Tokens> call, Response<OAuth2Tokens> response) {
                _token = response.body();
                if (_token == null || Checker.isEmpty(_token.getAccessToken())) {
                    handler.onComplete(Result.error(response));
                }
                else {
                    if (_provider != null) {
                        AuthenticatedUser authenticatedUser = new AuthenticatedUser("", new ActorRecord.ActorKey(""), "", _token, "Unknown", null, 0, null);
                        _provider.setAuthenticatedUser(authenticatedUser);
                    }
                    handler.onComplete(Result.success(null));
                }
            }

            @Override
            public void onFailure(Call<OAuth2Tokens> call, Throwable t) {
                handler.onComplete(Result.error(t));
            }
        });
    }

    private @Nullable OAuth2Tokens getToken() {
        if (_token == null && _provider != null) {
            AuthenticatedUser user = _provider.getAuthenticatedUserOrNull();
            if (user != null) {
                _token = user.getOAuth2Tokens();
            }
        }
        if (_token == null || _token.getExpiresIn() <= (System.currentTimeMillis() / 1000) + (15 * 60)) {
            return null;
        }
        return _token;
    }

    protected String getClientId() {
        return _clientId;
    }

    protected String getClientSecret() {
        return _clientSecret;
    }

    protected String getScope() {
        return _scope;
    }

    protected String getRedirectUri() {
        return _redirectUri;
    }

    interface AuthService {
        @FormUrlEncoded
        @POST("access_token")
        Call<OAuth2Tokens> getToken(@Field("client_id") String client_id,
                                    @Field("client_secret") String client_secret,
                                    @Field("redirect_uri") String redirect_uri,
                                    @Field("grant_type") String grant_type,
                                    @Field("code") String code);

        @FormUrlEncoded
        @POST("access_token")
        Call<OAuth2Tokens> refreshToken(@Field("client_id") String client_id,
                                        @Field("client_secret") String client_secret,
                                        @Field("refresh_token") String refresh_token,
                                        @Field("grant_type") String grant_type);
    }
}
