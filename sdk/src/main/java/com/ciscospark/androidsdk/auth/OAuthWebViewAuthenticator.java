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

package com.ciscospark.androidsdk.auth;


import javax.inject.Inject;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.util.Log;
import android.webkit.WebView;

import com.cisco.spark.android.core.Injector;
import com.ciscospark.androidsdk.CompletionHandler;
import com.ciscospark.androidsdk.Result;
import com.ciscospark.androidsdk.core.SparkInjectable;
import com.ciscospark.androidsdk.utils.Checker;
import com.ciscospark.androidsdk.utils.http.ServiceBuilder;

/**
 * OAuth2 authorization strategy using android WebView.
 *
 * @author Allen Xiao<xionxiao@cisco.com>
 * @version 0.1
 */
public class OAuthWebViewAuthenticator implements Authenticator, SparkInjectable {

    private static final String TAG = OAuthWebViewAuthenticator.class.getSimpleName();
    
    private CompletionHandler<Void> _callback;

    private OAuthAuthenticator _authenticator;

    private OAuthLauncher _launcher;
    
    @Inject
    Injector _injector;
        
    /**
     * @param clientId
     * @param clientSecret
     * @param scope
     * @param redirectUri
     */
    public OAuthWebViewAuthenticator(String clientId, String clientSecret, String scope, String redirectUri) {
        super();
        _authenticator = new OAuthAuthenticator(clientId, clientSecret, redirectUri, scope);
        _launcher = new OAuthLauncher();
    }

    @Override
    public boolean isAuthorized() {
        return _authenticator.isAuthorized();
    }

    @Override
    public void deauthorize() {
        _authenticator.deauthorize();
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void authorize(WebView view, CompletionHandler<Void> handler) {
        _launcher.launchOAuthView(view, buildCodeGrantUrl(), _authenticator.getRedirectUri(), result -> {
            Log.d(TAG, "authorize: " + result);
            String code = result.getData();
            if (!Checker.isEmpty(code)) {
                _authenticator.authorize(code, handler);
            }
            else {
                handler.onComplete(Result.error(result.getError()));
            }
        });
    }

    @Override
    public void getToken(CompletionHandler<String> handler) {
        _authenticator.getToken(handler);
    }

    private String buildCodeGrantUrl() {
        Uri.Builder builder = Uri.parse(ServiceBuilder.HYDRA_URL).buildUpon();
        builder.appendPath("authorize")
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter("client_id", _authenticator.getClientId())
                .appendQueryParameter("redirect_uri", _authenticator.getRedirectUri())
                .appendQueryParameter("scope", _authenticator.getScope())
                .appendQueryParameter("state", "androidsdkstate");
        return builder.toString();
    }

    @Override
    public void injected() {
        Log.d(TAG, "Inject authenticator after self injected");
        _injector.inject(_authenticator);
    }
}
