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

package com.ciscospark.androidsdk.auth;


import javax.inject.Inject;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;
import android.webkit.WebView;

import com.cisco.spark.android.core.Injector;
import com.ciscospark.androidsdk.CompletionHandler;
import com.ciscospark.androidsdk.auth.internal.OAuthLauncher;
import com.ciscospark.androidsdk.internal.ResultImpl;
import com.ciscospark.androidsdk.internal.SparkInjector;
import com.ciscospark.androidsdk.utils.http.ServiceBuilder;

import me.helloworld.utils.Checker;

/**
 * An <a href="https://oauth.net/2/">OAuth</a> based authentication with a WebView.
 * This is for authenticating a user on Cisco Spark.
 *
 * @see <a href="https://developer.ciscospark.com/authentication.html">Cisco Spark Integration</a>
 * @since 0.1
 */
public class OAuthWebViewAuthenticator implements Authenticator {

    private static final String TAG = OAuthWebViewAuthenticator.class.getSimpleName();

    private CompletionHandler<Void> _callback;

    private OAuthAuthenticator _authenticator;

    private OAuthLauncher _launcher;

    @Inject
    Injector _injector;

    /**
     * Creates a new OAuth authentication strategy
     *
     * @param clientId     the OAuth client id
     * @param clientSecret the OAuth client secret
     * @param scope        space-separated string representing which permissions the application needs
     * @param redirectUri  the redirect URI that will be called when completing the authentication. This must match the redirect URI registered to your clientId.
     * @see <a href="https://developer.ciscospark.com/authentication.html">Cisco Spark Integration</a>
     * @since 0.1
     */
    public OAuthWebViewAuthenticator(@NonNull String clientId, @NonNull String clientSecret, @NonNull String scope, @NonNull String redirectUri) {
        super();
        _authenticator = new OAuthAuthenticator(clientId, clientSecret, redirectUri, scope);
        _launcher = new OAuthLauncher();
    }

    /**
     * @see Authenticator
     */
    @Override
    public boolean isAuthorized() {
        return _authenticator.isAuthorized();
    }

    /**
     * @see Authenticator
     */
    @Override
    public void deauthorize() {
        _authenticator.deauthorize();
    }

    /**
     * Brings up a web-based authorization view and directs the user through the OAuth process.
     *
     * @param view    the web view for the authorization by the end user.
     * @param handler the completion handler will be called when authentication is complete, the error to indicate if the authentication process was successful.
     * @since 0.1
     */
    public void authorize(@NonNull WebView view, @NonNull CompletionHandler<Void> handler) {
        _launcher.launchOAuthView(view, buildCodeGrantUrl(), _authenticator.getRedirectUri(), result -> {
            Log.d(TAG, "authorize: " + result);
            String code = result.getData();
            if (!Checker.isEmpty(code)) {
                _authenticator.authorize(code, handler);
            } else {
                handler.onComplete(ResultImpl.error(result.getError()));
            }
        });
    }

    /**
     * @see Authenticator
     */
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

    @SparkInjector.AfterInjected
    private void afterInjected() {
        Log.d(TAG, "Inject authenticator after self injected");
        _injector.inject(_authenticator);
    }
}
