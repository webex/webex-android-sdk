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
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.cisco.spark.android.authenticator.OAuth2AccessToken;
import com.ciscospark.common.SparkError;

import java.math.BigInteger;
import java.security.SecureRandom;

import static com.ciscospark.auth.AuthorizeListener.AuthError.SERVER_ERROR;
import static com.ciscospark.auth.Constant.OAUTH_BASE_URL;

/**
 * OAuth2 authorization strategy using android WebView.
 *
 * @author Allen Xiao<xionxiao@cisco.com>
 * @version 0.1
 */
public class OAuthWebViewStrategy implements AuthorizationStrategy {
    private String mBaseUrl = OAUTH_BASE_URL;
    private WebView mWebView;
    private String mState;
    private AuthorizeListener mAuthListener;
    private OAuthStrategy mOAuthStategyDelegate = null;
    static final String TAG = "OAuthWebViewStrategy";


    /**
     * @param clientId
     * @param clientSecret
     * @param redirectUri
     * @param scope
     * @param email
     * @param webView
     */
    public OAuthWebViewStrategy(String clientId, String clientSecret, String redirectUri,
                                String scope, String email, WebView webView) {
        super();
        this.mWebView = webView;
        mOAuthStategyDelegate = new OAuthStrategy(clientId, clientSecret, redirectUri, scope, email, "");

        initWebView();
    }

    @Override
    public void authorize(AuthorizeListener listener) {
        this.mAuthListener = listener;
        String url = buildCodeGrantUrl(getEmail());
        Log.d(TAG, "authorize" + url);
        CookieManager.getInstance().removeAllCookie();
        mWebView.loadUrl(url);
    }

    @Override
    public void deauthorize() {
        this.mState = "";
        if (mOAuthStategyDelegate != null) {
            mOAuthStategyDelegate.deauthorize();
            mOAuthStategyDelegate = null;
        }
    }

    @Override
    public OAuth2AccessToken getToken() {
        if (mOAuthStategyDelegate != null)
            return mOAuthStategyDelegate.getToken();
        return null;
    }

    @Override
    public boolean isAuthorized() {
        return mOAuthStategyDelegate != null && mOAuthStategyDelegate.isAuthorized();
    }

    private String buildCodeGrantUrl(String email) {
        SecureRandom random = new SecureRandom();
        mState = new BigInteger(130, random).toString(32);
        Uri.Builder builder = Uri.parse(mBaseUrl).buildUpon();

        builder.appendPath("authorize")
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter("client_id", getClientId())
                .appendQueryParameter("redirect_uri", getRedirectUri())
                .appendQueryParameter("scope", getScope())
                .appendQueryParameter("mState", mState);
        if (!email.isEmpty())
            builder.appendQueryParameter("email", email);
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

    public String getClientId() {
        return mOAuthStategyDelegate.getClientId();
    }

    public void setClientId(String clientId) {
        mOAuthStategyDelegate.setClientId(clientId);
    }

    public String getClientSecret() {
        return mOAuthStategyDelegate.getClientSecret();
    }

    public void setClientSecret(String clientSecret) {
        mOAuthStategyDelegate.setClientSecret(clientSecret);
    }

    public String getScope() {
        return mOAuthStategyDelegate.getScope();
    }

    public void setScope(String scope) {
        mOAuthStategyDelegate.setScope(scope);
    }

    public String getRedirectUri() {
        return mOAuthStategyDelegate.getRedirectUri();
    }

    public void setRedirectUri(String redirectUri) {
        mOAuthStategyDelegate.setRedirectUri(redirectUri);
    }

    public String getAuthCode() {
        return mOAuthStategyDelegate.getAuthCode();
    }

    public void setAuthCode(String code) {
        mOAuthStategyDelegate.setAuthCode(code);
    }

    public String getEmail() {
        return mOAuthStategyDelegate.getEmail();
    }

    public void setEmail(String email) {
        mOAuthStategyDelegate.setEmail(email);
    }


    private class BrowserWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Log.d(TAG, url);
            // redirect uri is returned from server by lowercase.
            if (url.startsWith(getRedirectUri().toLowerCase())) {
                Uri uri = Uri.parse(url);
                String code = uri.getQueryParameter("code");
                if (code == null || code.isEmpty()) {
                    mAuthListener.onFailed(new SparkError());
                    return false;
                }
                setAuthCode(code);
                Log.d(TAG, "access code: " + getAuthCode());

                mWebView.clearCache(true);
                mWebView.loadUrl("about:blank");

                mOAuthStategyDelegate.setAuthCode(getAuthCode());
                mOAuthStategyDelegate.authorize(mAuthListener);

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
            Log.d(TAG, "" + errorCode + " " + description + " " + failingUrl);
            mAuthListener.onFailed(new SparkError(SERVER_ERROR, Integer.toString(errorCode)));
            super.onReceivedError(view, errorCode, description, failingUrl);
        }

        @Override
        public void onReceivedSslError(WebView view, final SslErrorHandler handler, SslError error) {
            Log.e(TAG, error.toString());
            handler.cancel();
            if (error != null) {
                mAuthListener.onFailed(new SparkError(SERVER_ERROR, error.toString()));
            } else
                Log.w(TAG, new Exception("SSLError unknown"));
        }
    }
}
