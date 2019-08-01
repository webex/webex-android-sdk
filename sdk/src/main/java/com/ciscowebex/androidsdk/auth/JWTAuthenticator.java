/*
 * Copyright 2016-2019 Cisco Systems Inc
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

package com.ciscowebex.androidsdk.auth;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Base64;
import com.cisco.spark.android.authenticator.ApiTokenProvider;
import com.cisco.spark.android.authenticator.OAuth2Tokens;
import com.cisco.spark.android.core.ApplicationController;
import com.cisco.spark.android.model.AuthenticatedUser;
import com.cisco.spark.android.model.conversation.ActorRecord;
import com.ciscowebex.androidsdk.CompletionHandler;
import com.ciscowebex.androidsdk.internal.ResultImpl;
import com.ciscowebex.androidsdk.utils.http.ServiceBuilder;
import com.ciscowebex.androidsdk_commlib.AfterInjected;
import com.github.benoitdion.ln.Ln;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import me.helloworld.utils.Converter;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.http.Header;
import retrofit2.http.POST;

import static com.ciscowebex.androidsdk.utils.Utils.checkNotNull;

/**
 * A <a href="https://jwt.io/introduction">JSON Web Token</a> (JWT) based authentication strategy is to be used to authenticate a user on Cisco Webex.
 *
 * @since 0.1
 */
public class JWTAuthenticator implements Authenticator {

    private OAuth2Tokens _token = null;

    private String _jwt;

    private AuthService _authService;

    @Inject
    ApiTokenProvider _provider;

    @Inject
    ApplicationController _applicationController;
    /**
     * Creates a new JWT authentication strategy
     *
     * @since 0.1
     */
    public JWTAuthenticator() {
        _authService = new ServiceBuilder().build(AuthService.class);
    }

    /**
     * @see Authenticator
     */
    @Override
    public boolean isAuthorized() {
        // Changed by Orel
        return getUnexpiredAccessToken() != null;
    }

    /**
     * Sets the JWT access token on the authorization strategy, overriting any existing access token.
     *
     * @param jwt the new JSON Web Token to use
     * @since 0.1
     */
    public void authorize(@NonNull String jwt) {
        deauthorize();
        _jwt = jwt;
    }

    /**
     * @see Authenticator
     */
    @Override
    public void deauthorize() {
        _jwt = null;
        _token = null;
        if (_applicationController != null) {
            Ln.d("deauthorize() clear all!");
            _applicationController.clear();
        }
    }

    /**
     * @see Authenticator
     */
    @Override
    public void getToken(@NonNull CompletionHandler<String> handler) {
        checkNotNull(handler, "getToken: CompletionHandler should not be null");
        String token = getUnexpiredAccessToken();
        if (token != null) {
            handler.onComplete(ResultImpl.success(token));
            return;
        }
        String jwt = getUnexpiredJwt();
        if (jwt == null) {
            handler.onComplete(ResultImpl.error("JWT is null"));
            return;
        }
        refreshToken(handler);
    }

    public void getTokenExpiresIn(@NonNull CompletionHandler<Long> handler) {

        checkNotNull(handler, "getToken: CompletionHandler should not be null");
        long expiresIn = getUnexpiredInAccessToken();
        if (expiresIn!=0) {
            handler.onComplete(ResultImpl.success(expiresIn));
            return;
        }

        handler.onComplete(ResultImpl.error("token is expired"));

    }

    private  @Nullable long getUnexpiredInAccessToken() {

        if (_token == null && _provider != null) {
            AuthenticatedUser user = _provider.getAuthenticatedUserOrNull();
            if (user != null) {
                _token = user.getOAuth2Tokens();
            }
        }
        if (_token == null || _token.getExpiresIn() <= (System.currentTimeMillis() / 1000) + (15 * 60)) {
            return 0;
        }
        return _token.getExpiresIn();
    }


    @Override
    public void refreshToken(CompletionHandler<String> handler) {
        checkNotNull(handler, "refreshToken: CompletionHandler should not be null");
        String jwt = getUnexpiredJwt();
        if (jwt == null) {
            handler.onComplete(ResultImpl.error("JWT is null"));
            return;
        }
        _authService.getToken(jwt).enqueue(new Callback<JwtToken>() {
            @Override
            public void onResponse(Call<JwtToken> call, Response<JwtToken> response) {
                JwtToken token = response.body();
                if (token == null || token.getAccessToken() == null || token.getAccessToken().isEmpty()) {
                    handler.onComplete(ResultImpl.error(response));
                } else {
                    _token = token.toOAuthToken();
                    if (_provider != null) {
                        AuthenticatedUser authenticatedUser = new AuthenticatedUser("", new ActorRecord.ActorKey(""), "", _token, "Unknown", null, 0, null);
                        _provider.setAuthenticatedUser(authenticatedUser);
                    }
                    handler.onComplete(ResultImpl.success(_token.getAccessToken()));
                }
            }

            @Override
            public void onFailure(Call<JwtToken> call, Throwable t) {
                handler.onComplete(ResultImpl.error(t));
            }
        });
    }

    private @Nullable String getUnexpiredJwt() {
        if (_jwt == null && _token != null) {
            _jwt = _token.getRefreshToken();
        }
        if (_jwt == null && _provider != null) {
            AuthenticatedUser user = _provider.getAuthenticatedUserOrNull();
            if (user != null) {
                _token = user.getOAuth2Tokens();
                _jwt = _token.getRefreshToken();
            }
        }
        if (_jwt == null) {
            return null;
        }
        Map<String, Object> map = parseJWT(_jwt);
        if (map == null) {
            return null;
        }
        try {
            long exp = Converter.toLong(map.get("exp"), -1);
            if (exp > 0 && exp <= (System.currentTimeMillis() / 1000)) {
                return null;
            }
        } catch (Exception ignored) {
            Ln.e(ignored);
        }
        return _jwt;
    }

    private @Nullable String getUnexpiredAccessToken() {
     //   if (!isAuthorized()) {
     //       return null;
     //   }
        if (_token == null && _provider != null) {
            AuthenticatedUser user = _provider.getAuthenticatedUserOrNull();
            if (user != null) {
                _token = user.getOAuth2Tokens();
            }
        }
        if (_token == null || _token.getExpiresIn() <= (System.currentTimeMillis() / 1000) + (15 * 60)) {
            return null;
        }
        return _token.getAccessToken();
    }

    private @Nullable Map<String, Object> parseJWT(String jwt) {
        String[] split = jwt.split("\\.");
        if (split.length != 3) {
            return null;
        }
        try {
            String json = new String(Base64.decode(split[1], Base64.URL_SAFE), "UTF-8");
            Gson gson = new Gson();
            Type strObjType = new TypeToken<HashMap<String, Object>>(){}.getType();
            return gson.fromJson(json, strObjType);
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    @AfterInjected
    private void afterInjected() {
        if (_provider != null && _token != null) {
            AuthenticatedUser authenticatedUser = new AuthenticatedUser("", new ActorRecord.ActorKey(""), "", _token, "Unknown", null, 0, null);
            _provider.setAuthenticatedUser(authenticatedUser);
        }
    }

    interface AuthService {
        @POST("jwt/login")
        Call<JwtToken> getToken(@Header("Authorization") String authorization);
    }

    private class JwtToken {

        @SerializedName("expiresIn")
        long expiresIn;

        @SerializedName("token")
        String accessToken;

        public long getExpiresIn() {
            return this.expiresIn;
        }

        String getAccessToken() {
            return this.accessToken;
        }

        OAuth2Tokens toOAuthToken() {
            OAuth2Tokens tokens = new OAuth2Tokens();
            tokens.setAccessToken(this.getAccessToken());
            tokens.setExpiresIn(this.getExpiresIn() + (System.currentTimeMillis() / 1000));
            tokens.setRefreshToken(this.getAccessToken());
            return tokens;
        }
    }
}
