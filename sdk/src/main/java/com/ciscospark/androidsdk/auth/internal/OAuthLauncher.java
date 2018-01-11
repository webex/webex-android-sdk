/*
 * Copyright 2016-2017 Cisco Systems Inc
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

package com.ciscospark.androidsdk.auth.internal;

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
import com.ciscospark.androidsdk.internal.ResultImpl;
import com.github.benoitdion.ln.Ln;

import me.helloworld.utils.Checker;

/**
 * Created by zhiyuliu on 01/09/2017.
 */

public class OAuthLauncher {

    public void launchOAuthView(WebView view, String authorizationUrl, String redirectUri, CompletionHandler<String> handler) {
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
            Ln.d("Loading " + url);
            // redirect uri is returned from server by lowercase.
            if (url.startsWith(_redirectUri.toLowerCase())) {
                Uri uri = Uri.parse(url);
                String code = uri.getQueryParameter("code");
                if (Checker.isEmpty(code)) {
                    _handler.onComplete(ResultImpl.error("Auth code isn't exist"));
                    return false;
                }
                view.clearCache(true);
                view.loadUrl("about:blank");
                handleResult(ResultImpl.success(code));
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
            Ln.e("Receive: " + errorCode + ", " + description);
            handleResult(ResultImpl.error(errorCode + " " + description));
            super.onReceivedError(view, errorCode, description, failingUrl);
        }

        @Override
        public void onReceivedSslError(WebView view, final SslErrorHandler handler, SslError error) {
            Ln.e("Receive SSL Error: " + error);
            handler.cancel();
            if (error != null) {
                handleResult(ResultImpl.error(error.toString()));
            }
        }

        private void handleResult(Result<String> result) {
            if (_handler != null) {
                _handler.onComplete(result);
                _handler = null;
            }
        }
    }

}
