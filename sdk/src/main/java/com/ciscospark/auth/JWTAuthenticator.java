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


import com.cisco.spark.android.authenticator.OAuth2;
import com.cisco.spark.android.authenticator.OAuth2AccessToken;
import com.ciscospark.common.SparkError;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;

import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Header;
import retrofit2.http.POST;

import com.ciscospark.auth.ErrorHandlingAdapter.ErrorHandlingCallAdapterFactory;
import com.ciscospark.auth.ErrorHandlingAdapter.ErrorHandlingCall;
import com.ciscospark.auth.ErrorHandlingAdapter.ErrorHandlingCallback;

import static com.ciscospark.auth.AuthorizeListener.AuthError.CLIENT_ERROR;
import static com.ciscospark.auth.AuthorizeListener.AuthError.NETWORK_ERROR;
import static com.ciscospark.auth.AuthorizeListener.AuthError.SERVER_ERROR;
import static com.ciscospark.auth.AuthorizeListener.AuthError.UNAUTHENTICATED;
import static com.ciscospark.auth.AuthorizeListener.AuthError.UNEXPECTED_ERROR;
import static com.ciscospark.auth.Constant.JWT_BASE_URL;

/**
 * JWT authorize strategy.
 * Reference http://www.jwt.io
 *
 * @author Allen Xiao<xionxiao@cisco.com>
 * @version 0.1
 */
public class JWTAuthenticator implements Authenticator {
    private JwtToken mToken = null;
    private String mAuthCode;
    private AuthService mAuthService;

    public void authorize(String jwt) {
        mAuthCode = jwt;

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(JWT_BASE_URL)
                .addCallAdapterFactory(new ErrorHandlingCallAdapterFactory())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        mAuthService = retrofit.create(AuthService.class);
    }

    @Override
    public void deauthorize() {
        mAuthCode = null;
        mToken = null;
    }

    @Override
    public void accessToken(AuthorizeListener listener) {
        if (!isAuthorized()) {
            listener.onFailed(new SparkError(UNAUTHENTICATED, "no jwt token"));
            return;
        }

        if (mToken == null || mToken.shoudlRefetchTokenNow()) {
            mAuthService.getToken(mAuthCode).enqueue(new ErrorHandlingCallback<JwtToken>() {
                @Override
                public void success(Response<JwtToken> response) {
                    mToken = response.body();
                    if (mToken == null) {
                        deauthorize();
                        listener.onFailed(new SparkError(CLIENT_ERROR, response.errorBody().toString()));
                    } else {
                        mToken.recordOptimisticRefreshTime();
                        listener.onSuccess(mToken);
                    }
                }

                @Override
                public void unauthenticated(Response<?> response) {
                    deauthorize();
                    listener.onFailed(new SparkError(UNAUTHENTICATED, Integer.toString(response.code())));
                }

                @Override
                public void clientError(Response<?> response) {
                    deauthorize();
                    listener.onFailed(new SparkError(CLIENT_ERROR, Integer.toString(response.code())));
                }

                @Override
                public void serverError(Response<?> response) {
                    deauthorize();
                    listener.onFailed(new SparkError(SERVER_ERROR, Integer.toString(response.code())));
                }

                @Override
                public void networkError(IOException e) {
                    deauthorize();
                    listener.onFailed(new SparkError(NETWORK_ERROR, "network error"));
                }

                @Override
                public void unexpectedError(Throwable t) {
                    deauthorize();
                    listener.onFailed(new SparkError(UNEXPECTED_ERROR, "unknown error"));
                }
            });
        } else {
            listener.onSuccess(mToken);
        }
    }

    @Override
    public boolean isAuthorized() {
        return mAuthCode != null && !mAuthCode.isEmpty();
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

        boolean shoudlRefetchTokenNow() {
            return super.shouldRefreshNow();
        }
    }
}
