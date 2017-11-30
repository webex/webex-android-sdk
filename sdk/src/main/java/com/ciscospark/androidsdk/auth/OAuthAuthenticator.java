/*
 * Copyright 2016-2017 Cisco Systems Inc
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


import javax.inject.Inject;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.cisco.spark.android.authenticator.ApiTokenProvider;
import com.cisco.spark.android.authenticator.OAuth2Tokens;
import com.cisco.spark.android.core.AuthenticatedUser;
import com.cisco.spark.android.sync.ActorRecord;
import com.ciscospark.androidsdk.CompletionHandler;
import com.ciscospark.androidsdk.internal.ResultImpl;
import com.ciscospark.androidsdk.internal.SparkInjector;
import com.ciscospark.androidsdk.utils.http.ServiceBuilder;
import com.github.benoitdion.ln.Ln;
import me.helloworld.utils.Checker;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

import static com.ciscospark.androidsdk.utils.Utils.checkNotNull;

/**
 * An <a href="https://oauth.net/2/">OAuth</a> based authentication strategy for authenticating a user on Cisco Spark.
 * 
 * @since 0.1
 * @see <a href="https://developer.ciscospark.com/authentication.html">Cisco Spark Integration</a>
 */
public class OAuthAuthenticator implements Authenticator {
	
    private String _clientId;
    private String _clientSecret;
    private String _scope;
    private String _redirectUri;

    private OAuth2Tokens _token;
    private AuthService _authService;

    @Inject
    ApiTokenProvider _provider;

    /**
     * Creates a new OAuth authentication strategy
     *
     * @param clientId the OAuth client id
     * @param clientSecret the OAuth client secret
     * @param scope space-separated string representing which permissions the application needs
     * @param redirectUri the redirect URI that will be called when completing the authentication. This must match the redirect URI registered to your clientId.
     * @see <a href="https://developer.ciscospark.com/authentication.html">Cisco Spark Integration</a>
     * @since 0.1
     */
    public OAuthAuthenticator(@NonNull String clientId, @NonNull String clientSecret, @NonNull String scope, @NonNull String redirectUri) {
        _clientId = clientId;
        _clientSecret = clientSecret;
        _redirectUri = redirectUri;
        _scope = scope;
        _authService = new ServiceBuilder().build(AuthService.class);
    }

	/**
	 * @see Authenticator
	 */
    @Override
    public boolean isAuthorized() {
        return getToken() != null;
    }

	/**
	 * @see Authenticator
	 */
    @Override
    public void deauthorize() {
        _token = null;
        if ( _provider != null) {
            _provider.clearAccount();
        }
    }

	/**
	 * Authorize with the OAuth authorization code
	 * 
	 * @param code OAuth authorization code
	 * @param handler the completion handler will be called when authentication is complete, the error to indicate if the authentication process was successful.
	 * @since 0.1   
	 */
	public void authorize(@NonNull String code, @NonNull CompletionHandler<Void> handler) {
        checkNotNull(handler, "CompletionHandler is null");
        Ln.d("Authorize: " + code);
        _authService.getToken(_clientId, _clientSecret, _redirectUri, "authorization_code", code).enqueue(new Callback<OAuth2Tokens>() {
            @Override
            public void onResponse(Call<OAuth2Tokens> call, Response<OAuth2Tokens> response) {
                _token = response.body();
                Ln.d("Authorize: " + _token + ", " + _provider);
                if (_token == null || _token.getAccessToken() == null || _token.getAccessToken().isEmpty()) {
                    handler.onComplete(ResultImpl.error(response));
                }
                else {
                    _token.setExpiresIn(_token.getExpiresIn() + System.currentTimeMillis() / 1000);
                    if (_provider != null) {
                        AuthenticatedUser authenticatedUser = new AuthenticatedUser("", new ActorRecord.ActorKey(""), "", _token, "Unknown", null, 0, null);
                        _provider.setAuthenticatedUser(authenticatedUser);
                    }
                    handler.onComplete(ResultImpl.success(null));
                }
            }

            @Override
            public void onFailure(Call<OAuth2Tokens> call, Throwable t) {
                handler.onComplete(ResultImpl.error(t));
            }
        });
    }

	/**
	 * @see Authenticator
	 */
    @Override
    public void getToken(@NonNull CompletionHandler<String> handler) {
        checkNotNull(handler, "CompletionHandler is null");
        OAuth2Tokens token = getToken();
        Ln.d("GetToken: " + token + ", " + _provider);
        if (token == null) {
            handler.onComplete(ResultImpl.error("Not authorized"));
            return;
        }
        if (!Checker.isEmpty(token.getAccessToken()) && token.getExpiresIn() > (System.currentTimeMillis() / 1000) + (15 * 60)) {
            handler.onComplete(ResultImpl.success(token.getAccessToken()));
            return;
        }
        _authService.refreshToken(_clientId, _clientSecret, token.getRefreshToken(), "refresh_token").enqueue(new Callback<OAuth2Tokens>() {
            @Override
            public void onResponse(Call<OAuth2Tokens> call, Response<OAuth2Tokens> response) {
                _token = response.body();
                if (_token == null || Checker.isEmpty(_token.getAccessToken())) {
                    handler.onComplete(ResultImpl.error(response));
                }
                else {
                    _token.setExpiresIn(_token.getExpiresIn() + System.currentTimeMillis() / 1000);
                    if (_provider != null) {
                        AuthenticatedUser authenticatedUser = new AuthenticatedUser("", new ActorRecord.ActorKey(""), "", _token, "Unknown", null, 0, null);
                        _provider.setAuthenticatedUser(authenticatedUser);
                    }
                    handler.onComplete(ResultImpl.success(_token.getAccessToken()));
                }
            }

            @Override
            public void onFailure(Call<OAuth2Tokens> call, Throwable t) {
                handler.onComplete(ResultImpl.error(t));
            }
        });
    }

    private @Nullable OAuth2Tokens getToken() {
        if (_token == null && _provider != null) {
            AuthenticatedUser user = _provider.getAuthenticatedUserOrNull();
            Ln.d("Get user: " + user + ", " + _provider);
            if (user != null) {
                _token = user.getOAuth2Tokens();
            }
        }
        if (_token == null || _token.getExpiresIn() <= (System.currentTimeMillis() / 1000) + (15 * 60)) {
	        Ln.d("Check token: " + _token + ", " + _provider);
            if (_token != null) {
	            Ln.d("Check token: " + _token.getExpiresIn() + ", " + (System.currentTimeMillis() / 1000) + (15 * 60) + ", " + _provider);
            }
            return null;
        }
        return _token;
    }
    
    @SparkInjector.AfterInjected
    private void afterInjected() {
        if (_provider != null && _token != null) {
            AuthenticatedUser authenticatedUser = new AuthenticatedUser("", new ActorRecord.ActorKey(""), "", _token, "Unknown", null, 0, null);
            _provider.setAuthenticatedUser(authenticatedUser);
        }
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
