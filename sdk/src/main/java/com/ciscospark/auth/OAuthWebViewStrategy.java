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

import java.math.BigInteger;
import java.security.SecureRandom;

import static com.ciscospark.auth.Constant.OAUTH_BASE_URL;


/**
 * OAuth2 authorization strategy using android WebView.
 */
public class OAuthWebViewStrategy implements AuthorizationStrategy {
    String baseUrl = OAUTH_BASE_URL;
    private WebView webView;
    private String clientId;
    private String clientSecret;
    private String scope;
    private String redirectUri;
    private String code;
    private String state;
    private String email;
    private AuthorizeListener listener;
    private OAuthStrategy delegated_strategy = null;
    static final String TAG = "OAuthWebViewStrategy";

    private OAuth2AccessToken token;

    @Override
    public void authorize(AuthorizeListener listener) {
        this.listener = listener;
        String url = buildCodeGrantUrl(email);
        Log.d(TAG, "authorize" + url);
        CookieManager.getInstance().removeAllCookie();
        webView.loadUrl(url);
    }

    @Override
    public void deauthorize() {
        this.state = "";
        delegated_strategy.deauthorize();
        delegated_strategy = null;
    }

    @Override
    public OAuth2AccessToken getToken() {
        if (delegated_strategy != null)
            return delegated_strategy.getToken();
        return null;
    }

    @Override
    public boolean isAuthorized() {
        return delegated_strategy != null && delegated_strategy.isAuthorized();
    }

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
        this.webView = webView;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.scope = scope;
        this.email = email;
        init();
    }

    private String buildCodeGrantUrl(String email) {
        SecureRandom random = new SecureRandom();
        state = new BigInteger(130, random).toString(32);
        Uri.Builder builder = Uri.parse(baseUrl).buildUpon();

        builder.appendPath("authorize")
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter("client_id", clientId)
                .appendQueryParameter("redirect_uri", redirectUri)
                .appendQueryParameter("scope", scope)
                .appendQueryParameter("state", state);
        if (!email.isEmpty())
                builder.appendQueryParameter("email", email);
        return builder.toString();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void init() {
        webView.clearCache(true);
        BrowserWebViewClient client = new BrowserWebViewClient();
        webView.setWebViewClient(client);
        WebSettings webSettings = webView.getSettings();
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
            Log.d(TAG,  url);
            if (url.startsWith(redirectUri.toLowerCase())) {
                Uri uri = Uri.parse(url);
                code = uri.getQueryParameter("code");
                Log.d(TAG, "access code: " +  code);

                webView.clearCache(true);
                webView.loadUrl("about:blank");

                delegated_strategy= new OAuthStrategy(clientId, clientSecret, redirectUri, scope, email, code);
                delegated_strategy.authorize(listener);

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
            super.onReceivedError(view, errorCode, description, failingUrl);
        }

        @Override
        public void onReceivedSslError(WebView view, final SslErrorHandler handler, SslError error) {
            Log.e(TAG, error.toString());
            handler.cancel();
            if (error != null) {
                listener.onFailed();
            } else
                Log.w(TAG, new Exception("SSLError unknown"));
        }

    }
}
