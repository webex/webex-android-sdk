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


import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.cisco.spark.android.authenticator.OAuth2AccessToken;
import com.cisco.spark.android.authenticator.OAuth2Tokens;
import com.ciscospark.common.SparkError;

import java.math.BigInteger;
import java.security.SecureRandom;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

import static com.ciscospark.auth.AuthorizeListener.AuthError.SERVER_ERROR;
import static com.ciscospark.auth.Constant.OAUTH_BASE_URL;

/**
 * OAuth2 authorization strategy using android WebView.
 *
 * @author Allen Xiao<xionxiao@cisco.com>
 * @version 0.1
 */
public class OAuthWebViewAuthenticator implements Authenticator {
    static final String AUTHORIZATION_CODE = "authorization_code";
    static final String REFRESH_TOKEN_CODE = "refresh_code";
    private String mBaseUrl = OAUTH_BASE_URL;
    private WebView mWebView;
    private String mState;
    private String mClientId;
    private String mClientSecret;
    private String mScope;
    private String mRedirectUri;
    private String mAuthCode;

    private OAuth2Tokens mToken = null;
    private AuthService mAuthService;
    private AuthorizeListener mAuthListener;
    private final AuthListenerDelegate delegate = new AuthListenerDelegate();


    /**
     * @param clientId
     * @param clientSecret
     * @param redirectUri
     * @param scope
     */
    public OAuthWebViewAuthenticator(String clientId, String clientSecret, String redirectUri,
                                     String scope) {
        mClientId = clientId;
        mClientSecret = clientSecret;
        mRedirectUri = redirectUri;
        mScope = scope;

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(OAUTH_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        mAuthService = retrofit.create(AuthService.class);
    }

    public void authorize(WebView webView, @NonNull AuthorizeListener listener) {
        this.mWebView = webView;
        mAuthListener = listener;
        initWebView();
        String url = buildCodeGrantUrl();
        CookieManager.getInstance().removeAllCookie();
        mWebView.setVisibility(View.VISIBLE);
        mWebView.loadUrl(url);
    }

    @Override
    public void deauthorize() {
        this.mState = "";
        mToken = null;
    }

    @Override
    public void accessToken(AuthorizeListener listener) {
        if (isAuthorized()) {
            listener.onSuccess(mToken);
        } else {
            if (mToken.shouldRefreshNow()) {
                // refresh token
                mAuthService.refreshToken(mClientId, mClientSecret, mToken.getRefreshToken(), REFRESH_TOKEN_CODE)
                        .enqueue(new Callback<OAuth2Tokens>() {
                            @Override
                            public void onResponse(Call<OAuth2Tokens> call, Response<OAuth2Tokens> response) {
                                mToken = response.body();
                                if (mToken != null)
                                    listener.onSuccess(mToken);
                                else
                                    listener.onFailed(new SparkError());
                            }

                            @Override
                            public void onFailure(Call<OAuth2Tokens> call, Throwable t) {
                                listener.onFailed(new SparkError());
                            }
                        });
            }
        }
    }

    @Override
    public boolean isAuthorized() {
        return (mToken != null) && (mToken.getAccessToken() != null) && !mToken.getAccessToken().isEmpty();
    }

    private String buildCodeGrantUrl() {
        SecureRandom random = new SecureRandom();
        mState = new BigInteger(130, random).toString(32);
        Uri.Builder builder = Uri.parse(mBaseUrl).buildUpon();

        builder.appendPath("authorize")
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter("client_id", mClientId)
                .appendQueryParameter("redirect_uri", mRedirectUri)
                .appendQueryParameter("scope", mScope)
                .appendQueryParameter("mState", mState);
        return builder.toString();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initWebView() {
        mWebView.clearCache(true);
        BrowserWebViewClient client = new BrowserWebViewClient();
        mWebView.setWebViewClient(client);
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setSupportZoom(true);
        webSettings.setJavaScriptEnabled(true);
        // Required for javascript to work with HTML5
        webSettings.setDomStorageEnabled(true);
        webSettings.setSaveFormData(false);
        webSettings.setAllowFileAccess(false);
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);

        CookieManager.getInstance().setAcceptCookie(true);
    }


    private class BrowserWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            // redirect uri is returned from server by lowercase.
            if (url.startsWith(mRedirectUri.toLowerCase())) {
                Uri uri = Uri.parse(url);
                String code = uri.getQueryParameter("code");
                if (code == null || code.isEmpty()) {
                    delegate.onFailed(new SparkError(SERVER_ERROR, "invalid auth code"));
                    return false;
                }
                mAuthCode = code;
                Log.d("OAUTH2", code);

                mWebView.clearCache(true);
                mWebView.loadUrl("about:blank");

                mAuthService.getToken(mClientId, mClientSecret, mRedirectUri, AUTHORIZATION_CODE, mAuthCode)
                        .enqueue(new Callback<OAuth2Tokens>() {
                            @Override
                            public void onResponse(Call<OAuth2Tokens> call, Response<OAuth2Tokens> response) {
                                mToken = response.body();
                                if (isAuthorized()) {
                                    Log.d("OAUTH2", mToken.getAccessToken());
                                    mToken.recordOptimisticRefreshTime();
                                    delegate.onSuccess(mToken);
                                } else
                                    Log.d("OAUTH2", response.errorBody().toString());
                                    delegate.onFailed(new SparkError(SERVER_ERROR, "" + response.code()));
                            }

                            @Override
                            public void onFailure(Call<OAuth2Tokens> call, Throwable t) {
                                delegate.onFailed(new SparkError(SERVER_ERROR, "Unknown error"));
                            }
                        });
                return false;
            }
            return super.shouldOverrideUrlLoading(view, url);
        }

        @Override
        @TargetApi(Build.VERSION_CODES.M)
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            this.onReceivedError(view, error.getErrorCode(), error.getDescription().toString(), request.getUrl().toString());
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            delegate.onFailed(new SparkError(SERVER_ERROR, Integer.toString(errorCode)));
            super.onReceivedError(view, errorCode, description, failingUrl);
        }

        @Override
        public void onReceivedSslError(WebView view, final SslErrorHandler handler, SslError error) {
            handler.cancel();
            if (error != null) {
                delegate.onFailed(new SparkError(SERVER_ERROR, error.toString()));
            } else {
                delegate.onFailed(new SparkError(SERVER_ERROR, "unknown error"));
            }
        }
    }

    private final class AuthListenerDelegate implements AuthorizeListener {

        @Override
        public void onSuccess(OAuth2AccessToken accessToken) {
            if (mAuthListener != null) {
                mAuthListener.onSuccess(accessToken);
                mAuthListener = null;
            }
        }

        @Override
        public void onFailed(SparkError<AuthError> error) {
            if (mAuthListener != null) {
                mAuthListener.onFailed(error);
                mAuthListener = null;
            }
        }
    }

    private interface AuthService {
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
