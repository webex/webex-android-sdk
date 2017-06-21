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
import com.cisco.spark.android.authenticator.OAuth2Tokens;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

import static com.ciscospark.auth.Constant.OAUTH_BASE_URL;


/**
 * @author Allen Xiao<xionxiao@cisco.com>
 * @version 0.1
 */
public class OAuthStrategy implements AuthorizationStrategy {
    public static final String AUTHORIZATION_CODE = "authorization_code";
    private String clientId;
    private String clientScret;
    private String scope;
    private String redirectUri;
    private String authCode;
    private String email;

    private Retrofit retrofit;
    private Call<OAuth2Tokens> call;
    private OAuth2Tokens token = null;

    /**
     * OAuth 2 authorize strategy.
     *
     * @param clientId
     * @param clientSecret
     * @param scope
     * @param redirectUri
     * @param authCode
     */
    public OAuthStrategy(String clientId,
                         String clientSecret,
                         String redirectUri,
                         String scope,
                         String email,
                         String authCode) {
        this.clientId = clientId;
        this.clientScret = clientSecret;
        this.scope = scope;
        this.redirectUri = redirectUri;
        this.email = email;
        this.authCode = authCode;

        retrofit = new Retrofit.Builder()
                .baseUrl(OAUTH_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        AuthService service = retrofit.create(AuthService.class);
        call = service.getToken(clientId, clientSecret,
                redirectUri, AUTHORIZATION_CODE, authCode);
    }

    /**
     * @param listener
     */
    @Override
    public void authorize(AuthorizeListener listener) {
        call.enqueue(new Callback<OAuth2Tokens>() {
            @Override
            public void onResponse(Call<OAuth2Tokens> call, Response<OAuth2Tokens> response) {
                token = response.body();
                if (token != null)
                    listener.onSuccess();
                else
                    listener.onFailed();
            }

            @Override
            public void onFailure(Call<OAuth2Tokens> call, Throwable t) {
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
        return token != null;
    }

    private interface AuthService {
        @FormUrlEncoded
        @POST("access_token")
        Call<OAuth2Tokens> getToken(@Field("client_id") String client_id,
                                    @Field("client_secret") String client_secret,
                                    @Field("redirect_uri") String redirect_uri,
                                    @Field("grant_type") String grant_type,
                                    @Field("code") String code);
    }
}
