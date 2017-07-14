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
import com.ciscospark.common.SparkError;

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
 * OAuth2 strategy using granted auth code.
 *
 * @author Allen Xiao<xionxiao@cisco.com>
 * @version 0.1
 */
public class OAuthStrategy implements Authenticator {
    static final String AUTHORIZATION_CODE = "authorization_code";
    private String mClientId;
    private String mClientSecret;
    private String mScope;
    private String mRedirectUri;
    private String mAuthCode;
    private String mEmail;

    private OAuth2Tokens mToken = null;
    private AuthService mAuthService;

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
        this.setClientId(clientId);
        this.setClientSecret(clientSecret);
        this.setScope(scope);
        this.setRedirectUri(redirectUri);
        this.setEmail(email);
        this.setAuthCode(authCode);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(OAUTH_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        mAuthService = retrofit.create(AuthService.class);
    }

    /**
     * @param listener
     */
    @Override
    public void authorize(AuthorizeListener listener) {
        mAuthService.getToken(mClientId, mClientSecret, mRedirectUri, AUTHORIZATION_CODE, mAuthCode)
                .enqueue(new Callback<OAuth2Tokens>() {
                    @Override
                    public void onResponse(Call<OAuth2Tokens> call, Response<OAuth2Tokens> response) {
                        mToken = response.body();
                        if (mToken != null)
                            listener.onSuccess();
                        else
                            listener.onFailed(new SparkError());
                    }

                    @Override
                    public void onFailure(Call<OAuth2Tokens> call, Throwable t) {
                        listener.onFailed(new SparkError());
                    }
                });
    }

    @Override
    public void deauthorize() {
        mToken = null;
    }

    @Override
    public String accessToken() {
        return mToken.getAccessToken();
    }

    @Override
    public boolean isAuthorized() {
        return mToken != null;
    }

    protected String getClientId() {
        return mClientId;
    }

    protected void setClientId(String clientId) {
        this.mClientId = clientId;
    }

    protected String getClientSecret() {
        return mClientSecret;
    }

    protected void setClientSecret(String clientSecret) {
        this.mClientSecret = clientSecret;
    }

    protected String getScope() {
        return mScope;
    }

    protected void setScope(String scope) {
        this.mScope = scope;
    }

    protected String getRedirectUri() {
        return mRedirectUri;
    }

    protected void setRedirectUri(String redirectUri) {
        this.mRedirectUri = redirectUri;
    }

    protected String getAuthCode() {
        return mAuthCode;
    }

    protected void setAuthCode(String authCode) {
        this.mAuthCode = authCode;
    }

    protected String getEmail() {
        return mEmail;
    }

    protected void setEmail(String email) {
        this.mEmail = email;
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
