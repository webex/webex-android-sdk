/*
 * Copyright 2016-2021 Cisco Systems Inc
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

package com.ciscowebex.androidsdk.auth;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.webkit.WebView;
import com.ciscowebex.androidsdk.CompletionHandler;
import com.ciscowebex.androidsdk.auth.internal.OAuthLauncher;
import com.ciscowebex.androidsdk.internal.ResultImpl;
import com.ciscowebex.androidsdk.internal.Service;
import com.github.benoitdion.ln.Ln;
import me.helloworld.utils.Checker;

/**
 * An <a href="https://oauth.net/2/">OAuth</a> based authentication with a WebView.
 * This is for authenticating a user on Cisco Webex.
 *
 * @see <a href="https://developer.webex.com/docs/integrations">Cisco Webex Integration</a>
 * @since 0.1
 */
public class OAuthWebViewAuthenticator implements Authenticator {

    private OAuthAuthenticator authenticator;

    private OAuthLauncher launcher;

    /**
     * Creates a new OAuth authentication strategy
     *
     * @param clientId     the OAuth client id
     * @param clientSecret the OAuth client secret
     * @param scope        space-separated string representing which permissions the application needs
     * @param redirectUri  the redirect URI that will be called when completing the authentication. This must match the redirect URI registered to your clientId.
     * @see <a href="https://developer.webex.com/docs/integrations">Cisco Webex Integration</a>
     * @since 0.1
     */
    public OAuthWebViewAuthenticator(@NonNull String clientId, @NonNull String clientSecret, @NonNull String scope, @NonNull String redirectUri) {
        super();
        authenticator = new OAuthAuthenticator(clientId, clientSecret, scope, redirectUri);
        launcher = new OAuthLauncher();
    }

    public void afterAssociated() {
        authenticator.afterAssociated();
    }

    /**
     * @see Authenticator
     */
    @Override
    public boolean isAuthorized() {
        return authenticator.isAuthorized();
    }

    /**
     * @see Authenticator
     */
    @Override
    public void deauthorize() {
        authenticator.deauthorize();
    }

    /**
     * Brings up a web-based authorization view and directs the user through the OAuth process.
     *
     * @param view    the web view for the authorization by the end user.
     * @param handler the completion handler will be called when authentication is complete, the error to indicate if the authentication process was successful.
     * @since 0.1
     */
    public void authorize(@NonNull WebView view, @NonNull CompletionHandler<Void> handler) {
        launcher.launchOAuthView(view, buildCodeGrantUrl(), authenticator.getRedirectUri(), result -> {
            Ln.d("Authorize: " + result);
            String code = result.getData();
            if (!Checker.isEmpty(code)) {
                authenticator.authorize(code, handler);
            } else {
                ResultImpl.errorInMain(handler, result);
            }
        });
    }

    /**
     * @see Authenticator
     */
    @Override
    public void getToken(CompletionHandler<String> handler) {
        authenticator.getToken(handler);
    }

    @Override
    public void refreshToken(CompletionHandler<String> handler) {
        authenticator.refreshToken(handler);
    }
    
    private String buildCodeGrantUrl() {
        Uri.Builder builder = Uri.parse(Service.Hydra.baseUrl(null)).buildUpon();
        builder.appendPath("authorize")
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter("client_id", authenticator.getClientId())
                .appendQueryParameter("redirect_uri", authenticator.getRedirectUri())
                .appendQueryParameter("scope", authenticator.getScope())
                .appendQueryParameter("state", "androidsdkstate");
        return builder.toString();
    }

}
