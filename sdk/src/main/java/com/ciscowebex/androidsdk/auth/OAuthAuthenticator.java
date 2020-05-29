/*
 * Copyright 2016-2020 Cisco Systems Inc
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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.ciscowebex.androidsdk.CompletionHandler;
import com.ciscowebex.androidsdk.WebexError;
import com.ciscowebex.androidsdk.internal.Closure;
import com.ciscowebex.androidsdk.internal.Settings;
import com.ciscowebex.androidsdk.internal.model.TokenModel;
import com.ciscowebex.androidsdk.internal.queue.Queue;
import com.ciscowebex.androidsdk.utils.Json;
import com.ciscowebex.androidsdk.internal.Service;
import com.ciscowebex.androidsdk.internal.ResultImpl;
import com.github.benoitdion.ln.Ln;
import me.helloworld.utils.Checker;
import okhttp3.FormBody;

/**
 * An <a href="https://oauth.net/2/">OAuth</a> based authentication strategy for authenticating a user on Cisco Webex.
 *
 * @see <a href="https://developer.webex.com/authentication.html">Cisco Webex Integration</a>
 * @since 0.1
 */
public class OAuthAuthenticator implements Authenticator {

    private String clientId;
    private String clientSecret;
    private String scope;
    private String redirectUri;

    private TokenModel tokenModel;

    /**
     * Creates a new OAuth authentication strategy
     *
     * @param clientId     the OAuth client id
     * @param clientSecret the OAuth client secret
     * @param scope        space-separated string representing which permissions the application needs
     * @param redirectUri  the redirect URI that will be called when completing the authentication. This must match the redirect URI registered to your clientId.
     * @see <a href="https://developer.webex.com/authentication.html">Cisco Webex Integration</a>
     * @since 0.1
     */
    public OAuthAuthenticator(@NonNull String clientId, @NonNull String clientSecret, @NonNull String scope, @NonNull String redirectUri) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.scope = scope;
    }

    public void afterAssociated() {
        if (tokenModel != null) {
            Settings.shared.store(TokenModel.AUTHENTICATION_INFO, Json.get().toJson(tokenModel));
        }
    }
    /**
     * @see Authenticator
     */
    @Override
    public boolean isAuthorized() {
        return getToken() != null;
    }

    /**
     * @see Authenticator
     */
    @Override
    public void deauthorize() {
        tokenModel = null;
        Settings.shared.clear();
    }

    /**
     * Authorize with the OAuth authorization code
     *
     * @param code    OAuth authorization code
     * @param handler the completion handler will be called when authentication is complete, the error to indicate if the authentication process was successful.
     * @since 0.1
     */
    public void authorize(@NonNull String code, @NonNull CompletionHandler<Void> handler) {
        deauthorize();
        Ln.d("Authorize: " + code);
        FormBody formBody = new FormBody.Builder()
                .add("client_id", clientId)
                .add("client_secret", clientSecret)
                .add("redirect_uri", redirectUri)
                .add("grant_type", "authorization_code")
                .add("code", code)
                .build();
        Service.Hydra.post(formBody).to("access_token").queue(Queue.main).model(TokenModel.class).error(handler)
                .async((Closure<TokenModel>) model -> {
            Ln.d("Authorized: " + model);
            tokenModel = model;
            tokenModel.setExpiresIn(tokenModel.getExpiresIn() + System.currentTimeMillis() / 1000);
            Settings.shared.store(TokenModel.AUTHENTICATION_INFO, Json.get().toJson(tokenModel));
            handler.onComplete(ResultImpl.success(null));
        });
    }

    /**
     * @see Authenticator
     */
    @Override
    public void getToken(@NonNull CompletionHandler<String> handler) {
        TokenModel token = getToken();
        Ln.d("GetToken: " + token);
        if (token == null) {
            ResultImpl.errorInMain(handler, WebexError.from("Not authorized"));
            return;
        }
        if (!Checker.isEmpty(token.getAccessToken()) && token.getExpiresIn() > (System.currentTimeMillis() / 1000) + (15 * 60)) {
            ResultImpl.inMain(handler, token.getAccessToken());
            return;
        }
        refreshToken(handler);
    }

    @Override
    public void refreshToken(CompletionHandler<String> handler) {
        TokenModel saved = getToken();
        Ln.d("RefreshToken: " + saved);
        if (saved == null) {
            ResultImpl.errorInMain(handler, WebexError.from("Not authorized"));
            return;
        }
        FormBody formBody = new FormBody.Builder()
                .add("client_id", clientId)
                .add("client_secret", clientSecret)
                .add("refresh_token", saved.getRefreshToken())
                .add("grant_type", "refresh_token")
                .build();
        Service.Hydra.post(formBody).to("access_token").queue(Queue.main).model(TokenModel.class).error(handler).async((Closure<TokenModel>) model -> {
            Ln.d("Refreshed: " + model);
            tokenModel = model;
            tokenModel.setExpiresIn(tokenModel.getExpiresIn() + System.currentTimeMillis() / 1000);
            Settings.shared.store(TokenModel.AUTHENTICATION_INFO, Json.get().toJson(tokenModel));
            handler.onComplete(ResultImpl.success(tokenModel.getAccessToken()));
        });
    }

    private @Nullable TokenModel getToken() {
        if (tokenModel == null) {
            tokenModel = Settings.shared.get(TokenModel.AUTHENTICATION_INFO, TokenModel.class, null);
        }
        if (tokenModel == null || tokenModel.getExpiresIn() <= (System.currentTimeMillis() / 1000) + (15 * 60)) {
            return null;
        }
        return tokenModel;
    }

    protected String getClientId() {
        return clientId;
    }

    protected String getClientSecret() {
        return clientSecret;
    }

    protected String getScope() {
        return scope;
    }

    protected String getRedirectUri() {
        return redirectUri;
    }
}
