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

package com.ciscospark.auth;


import com.cisco.spark.android.authenticator.OAuth2AccessToken;
import com.ciscospark.CompletionHandler;
import com.ciscospark.SparkError;
import com.ciscospark.auth.ErrorHandlingAdapter.ErrorHandlingCall;
import com.ciscospark.auth.ErrorHandlingAdapter.ErrorHandlingCallAdapterFactory;
import com.ciscospark.auth.ErrorHandlingAdapter.ErrorHandlingCallback;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;

import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Header;
import retrofit2.http.POST;

import static com.ciscospark.Utils.checkNotNull;
import static com.ciscospark.auth.Authenticator.AuthError.CLIENT_ERROR;
import static com.ciscospark.auth.Authenticator.AuthError.NETWORK_ERROR;
import static com.ciscospark.auth.Authenticator.AuthError.SERVER_ERROR;
import static com.ciscospark.auth.Authenticator.AuthError.UNAUTHENTICATED;
import static com.ciscospark.auth.Authenticator.AuthError.UNEXPECTED_ERROR;

/**
 * JWT authorize strategy.
 * Reference http://www.jwt.io
 *
 * @author Allen Xiao<xionxiao@cisco.com>
 */
public class JWTAuthenticator implements Authenticator {
    private JwtToken mToken = null;
    private String mJwtAuthCode;
    private AuthService mAuthService;

    final String BASE_URL = "https://api.ciscospark.com/v1/jwt/";

    public JWTAuthenticator(String jwtAuthCode) {
        mJwtAuthCode = jwtAuthCode;

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addCallAdapterFactory(new ErrorHandlingCallAdapterFactory())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        mAuthService = retrofit.create(AuthService.class);
    }

    @Override
    public void authorize(CompletionHandler<String> handler) {
        checkNotNull(handler, "CompletionHandler should not be null");

        if (mJwtAuthCode == null || mJwtAuthCode.isEmpty()) {
            handler.onError(new SparkError(UNEXPECTED_ERROR, "jwt is null"));
            return;
        }

        mAuthService.getToken(mJwtAuthCode).enqueue(new ErrorHandlingCallback<JwtToken>() {
            @Override
            public void success(Response<JwtToken> response) {
                mToken = response.body();
                if (mToken == null)
                    handler.onError(new SparkError(CLIENT_ERROR, response.errorBody().toString()));
                else {
                    if (mToken.getAccessToken() != null && !mToken.getAccessToken().isEmpty()) {
                        handler.onComplete(mToken.getAccessToken());
                    } else {
                        handler.onError(new SparkError(UNEXPECTED_ERROR, "Access token is empty"));
                    }
                }
            }

            @Override
            public void unauthenticated(Response<?> response) {
                handler.onError(new SparkError(UNAUTHENTICATED, Integer.toString(response.code())));
            }

            @Override
            public void clientError(Response<?> response) {
                handler.onError(new SparkError(CLIENT_ERROR, Integer.toString(response.code())));
            }

            @Override
            public void serverError(Response<?> response) {
                handler.onError(new SparkError(SERVER_ERROR, Integer.toString(response.code())));
            }

            @Override
            public void networkError(IOException e) {
                handler.onError(new SparkError(NETWORK_ERROR, "network error"));
            }

            @Override
            public void unexpectedError(Throwable t) {
                handler.onError(new SparkError(UNEXPECTED_ERROR, "unknown error"));
            }
        });
    }

    @Override
    public void deauthorize() {
        mToken = null;
    }

    @Override
    public void getToken(CompletionHandler<String> handler) {
        checkNotNull(handler, "CompletionHandler should not be null");

        if (!isAuthorized()) {
            handler.onError(new SparkError(UNEXPECTED_ERROR, "Spark is not authorized"));
            return;
        }

        if (mToken.shoudlRefetchTokenNow()) {
            this.authorize(handler);
        } else {
            if (mToken.getAccessToken() != null && !mToken.getAccessToken().isEmpty()) {
                handler.onComplete(mToken.getAccessToken());
            } else {
                handler.onError(new SparkError(UNEXPECTED_ERROR, "Access token is empty"));
            }
        }
    }

    @Override
    public boolean isAuthorized() {
        return mToken != null;
    }

    interface AuthService {
        @POST("login")
        ErrorHandlingCall<JwtToken> getToken(@Header("Authorization") String authorization);
    }


    static class JwtToken extends OAuth2AccessToken {
        @SerializedName("expiresIn")
        protected long expiresIn;

        @SerializedName("token")
        protected String accessToken;

        @Override
        public long getExpiresIn() {
            return this.expiresIn;
        }

        @Override
        public String getAccessToken() {
            return this.accessToken;
        }

        @Override
        public boolean shouldRefreshNow() {
            return false;
        }

        /* refresh by Authenticator */
        boolean shoudlRefetchTokenNow() {
            return super.shouldRefreshNow();
        }
    }
}
