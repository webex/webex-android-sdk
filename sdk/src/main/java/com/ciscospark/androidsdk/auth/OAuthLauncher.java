package com.ciscospark.androidsdk.auth;

import android.annotation.TargetApi;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.ciscospark.androidsdk.CompletionHandler;
import com.ciscospark.androidsdk.Result;
import com.ciscospark.androidsdk.utils.Checker;

/**
 * Created by zhiyuliu on 01/09/2017.
 */

public class OAuthLauncher {

    void launchOAuthView(WebView view, String authorizationUrl, String redirectUri, CompletionHandler<String> handler) {
        BrowserWebViewClient client = new BrowserWebViewClient(authorizationUrl, redirectUri, handler);
        view.clearCache(true);
        view.setWebViewClient(client);
        WebSettings webSettings = view.getSettings();
        webSettings.setSupportZoom(true);
        webSettings.setJavaScriptEnabled(true);
        // Required for javascript to work with HTML5
        webSettings.setDomStorageEnabled(true);
        webSettings.setSaveFormData(false);
        webSettings.setAllowFileAccess(false);
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().removeAllCookie();
        view.loadUrl(authorizationUrl);
    }

    private class BrowserWebViewClient extends WebViewClient {

        private String _url;

        private String _redirectUri;

        private CompletionHandler<String> _handler;

        BrowserWebViewClient(String url, String redirectUri, CompletionHandler<String> handler) {
            _url = url;
            _redirectUri = redirectUri;
            _handler = handler;
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            // redirect uri is returned from server by lowercase.
            if (url.startsWith(_redirectUri.toLowerCase())) {
                Uri uri = Uri.parse(url);
                String code = uri.getQueryParameter("code");
                if (Checker.isEmpty(code)) {
                    _handler.onComplete(Result.error("Auth code isn't exist"));
                    return false;
                }
                view.clearCache(true);
                view.loadUrl("about:blank");
                _handler.onComplete(Result.success(code));
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
            _handler.onComplete(Result.error(errorCode + " " + description));
            super.onReceivedError(view, errorCode, description, failingUrl);
        }

        @Override
        public void onReceivedSslError(WebView view, final SslErrorHandler handler, SslError error) {
            handler.cancel();
            if (error != null) {
                _handler.onComplete(Result.error(error.toString()));
            }
        }
    }

}
