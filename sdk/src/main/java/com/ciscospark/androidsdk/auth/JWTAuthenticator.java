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
import android.util.Base64;
import android.util.Log;

import com.cisco.spark.android.authenticator.ApiTokenProvider;
import com.cisco.spark.android.authenticator.OAuth2Tokens;
import com.cisco.spark.android.core.AuthenticatedUser;
import com.cisco.spark.android.sync.ActorRecord;
import com.ciscospark.androidsdk.CompletionHandler;
import com.ciscospark.androidsdk.Result;
import com.ciscospark.androidsdk.utils.Converter;
import com.ciscospark.androidsdk.utils.http.ServiceBuilder;
import com.ciscospark.androidsdk.utils.reflect.Fields;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;


import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.http.Header;
import retrofit2.http.POST;

import static com.ciscospark.androidsdk.utils.Utils.checkNotNull;

/**
 * JWT authorize strategy.
 * Reference http://www.jwt.io
 *
 * @author Allen Xiao<xionxiao@cisco.com>
 */
public class JWTAuthenticator implements Authenticator {

    private OAuth2Tokens _token = null;

    private String _jwt;

    private AuthService _authService;

    @Inject
    ApiTokenProvider _provider;

    public JWTAuthenticator() {
        _authService = new ServiceBuilder().build(AuthService.class);
    }

    @Override
    public boolean isAuthorized() {
        return getUnexpiredJwt() != null;
    }

    public void authorize(String jwt) {
        deauthorize();
        _jwt = jwt;
    }

    @Override
    public void deauthorize() {
        _jwt = null;
        _token = null;
        if ( _provider != null) {
            _provider.clearAccount();
        }
    }

    @Override
    public void getToken(CompletionHandler<String> handler) {
        checkNotNull(handler, "CompletionHandler should not be null");
        String jwt = getUnexpiredJwt();
        if (jwt == null) {
            handler.onComplete(Result.error("jwt is null"));
            return;
        }
        String token = getUnexpiredAccessToken();
        if (token != null) {
            handler.onComplete(Result.success(token));
            return;
        }
        _authService.getToken(jwt).enqueue(new Callback<JwtToken>() {
            @Override
            public void onResponse(Call<JwtToken> call, Response<JwtToken> response) {
                JwtToken token = response.body();
                if (token == null || token.getAccessToken() == null || token.getAccessToken().isEmpty()) {
                    handler.onComplete(Result.error(response));
                }
                else {
                    _token = token.toOAuthToken(jwt);
                    if (_provider != null) {
                        AuthenticatedUser authenticatedUser = new AuthenticatedUser("", new ActorRecord.ActorKey(""), "", _token, "Unknown", null, 0, null);
                        _provider.setAuthenticatedUser(authenticatedUser);
                    }
                    handler.onComplete(Result.success(_token.getAccessToken()));
                }
            }

            @Override
            public void onFailure(Call<JwtToken> call, Throwable t) {
                handler.onComplete(Result.error(t));
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
        }
        catch (Throwable ignored) {
        }
        return _jwt;
    }

    private @Nullable String getUnexpiredAccessToken() {
        if (!isAuthorized()) {
            return null;
        }
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
            Map<String,Object> map = new HashMap<String,Object>();
            return gson.fromJson(json, map.getClass());
        } catch (UnsupportedEncodingException e) {
            return null;
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

        OAuth2Tokens toOAuthToken(String jwt) {
            OAuth2Tokens tokens = new OAuth2Tokens();
            try {
                tokens.setAccessToken(this.accessToken);
                Field expiresIn = Fields.findField(OAuth2Tokens.class, "expiresIn", true);
                expiresIn.setLong(tokens, this.expiresIn);
                Field refreshToken = Fields.findField(OAuth2Tokens.class, "refreshToken", true);
                refreshToken.set(tokens, jwt);
                return tokens;
            }
            catch (Throwable t) {
                Log.e("", "" ,t);
                return null;
            }
        }
    }
}
