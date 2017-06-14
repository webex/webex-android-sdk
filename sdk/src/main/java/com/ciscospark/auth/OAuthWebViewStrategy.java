package com.ciscospark.auth;


import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.math.BigInteger;
import java.security.SecureRandom;


/**
 * OAuth2 authorization strategy using android WebView.
 */
public class OAuthWebViewStrategy implements AuthorizationStrategy {
    String baseUrl = "https://api.ciscospark.com/v1/";
    private WebView webView;
    private String clientId;
    private String clientSecret;
    private String scope;
    private String redirectUri;
    private String code;
    private String state;
    private String email;
    private AuthorizeListener listener;
    static final String TAG = "OAuthWebViewStrategy";

    @Override
    public void authorize(AuthorizeListener listener) {
        this.listener = listener;
        String url = buildCodeGrantUrl(email);
        Log.d(TAG, "authorize" + url);
        webView.loadUrl(url);
    }

    @Override
    public void deauthorize() {
        this.state = "";
    }

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
                .appendQueryParameter("state", state)
                .appendQueryParameter("email", email);
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

        webView.requestFocus(View.FOCUS_DOWN);
    }


    private class BrowserWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Log.d(TAG,  url);
            if (url.startsWith(redirectUri.toLowerCase())) {
                Uri uri = Uri.parse(url);
                code = uri.getQueryParameter("code");
                Log.d(TAG, "access code: " +  code);
                webView.loadUrl("about:blank");

                OAuthStrategy strategy = new OAuthStrategy(clientId, clientSecret, redirectUri, scope, code);
                strategy.authorize(listener);

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
