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
import com.google.gson.annotations.SerializedName;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Header;
import retrofit2.http.POST;

import static com.ciscospark.auth.Constant.JWT_BASE_URL;

/**
 * @author      Allen Xiao<xionxiao@cisco.com>
 * @version     0.1
 */
public class JWTStrategy implements AuthorizationStrategy {
    private JwtToken token = null;
    Call<JwtToken> call;

    class JwtToken extends OAuth2AccessToken {
        @SerializedName("expiresIn")
        protected long expiresIn;

        @SerializedName("token")
        protected String accessToken;

        @Override
        public long getExpiresIn() {
            return this.expiresIn;
        }

        @Override
        public String getAccessToken() { return this.accessToken; }

        @Override
        public boolean shouldRefreshNow() {
            return false;
        }
    }


    public JWTStrategy(String authcode) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(JWT_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        AuthService service = retrofit.create(AuthService.class);
        call = service.getToken(authcode);
    }

    @Override
    public void authorize(AuthorizeListener listener) {
        call.enqueue(new Callback<JwtToken>() {
            @Override
            public void onResponse(Call<JwtToken> call, Response<JwtToken> response) {
                token = response.body();
                listener.onSuccess();
            }

            @Override
            public void onFailure(Call<JwtToken> call, Throwable t) {
                listener.onFailed();
            }
        });

    }

    @Override
    public void deauthorize() {
        token = null;
    }

    @Override
    public OAuth2AccessToken getToken() {
        return token;
    }

    @Override
    public boolean isAuthorized() {
        return (token != null);
    }

    private interface AuthService {
        @POST("login")
        Call<JwtToken> getToken(@Header("Authorization") String authorization);
    }
}
