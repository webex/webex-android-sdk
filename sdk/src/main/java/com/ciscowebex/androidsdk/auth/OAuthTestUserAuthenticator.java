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

package com.ciscowebex.androidsdk.auth;

import javax.inject.Inject;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import com.cisco.spark.android.BuildConfig;
import com.cisco.spark.android.authenticator.AuthenticatedUserTask;
import com.cisco.spark.android.authenticator.OAuth2Tokens;
import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.ApplicationController;
import com.cisco.spark.android.model.AuthenticatedUser;
import com.cisco.spark.android.model.LoginTestUserRequest;
import com.cisco.spark.android.model.conversation.ActorRecord;
import com.cisco.spark.android.util.LoggingLock;
import com.ciscowebex.androidsdk.CompletionHandler;
import com.ciscowebex.androidsdk.internal.ResultImpl;
import com.github.benoitdion.ln.Ln;

/**
 * Created by qimdeng on 12/7/17.
 */

public class OAuthTestUserAuthenticator extends OAuthAuthenticator {
    private LoggingLock lock = new LoggingLock(BuildConfig.DEBUG, "OAuthTestUserAuthenticator Lock");

    private String email;

    private String name;

    private String password;

    private String scope;

    @Inject
    ApiClientProvider apiClientProvider;

    @Inject
    ApplicationController applicationController;

    /**
     * Creates a new OAuth authentication strategy
     *
     * @param clientId     the OAuth client id
     * @param clientSecret the OAuth client secret
     * @param scope        space-separated string representing which permissions the application needs
     * @param redirectUri  the redirect URI that will be called when completing the authentication. This must match the redirect URI registered to your clientId.
     * @see <a href="https://developer.webex.com/authentication.html">Cisco Webex Integration</a>
     * @since 1.3.0
     */
    public OAuthTestUserAuthenticator(@NonNull String clientId, @NonNull String clientSecret, @NonNull String scope, @NonNull String redirectUri,
                                      @NonNull String email, @NonNull String name, @NonNull String password) {
        super(clientId, clientSecret, scope, redirectUri);
        this.email = email;
        this.name = name;
        this.password = password;
        this.scope = scope;
    }

    /**
     * Brings up a web-based authorization view and directs the user through the OAuth process.
     *
     * @param handler the completion handler will be called when authentication is complete, the error to indicate if the authentication process was successful.
     * @since 1.3.0
     */
    public void authorize(@NonNull CompletionHandler<Void> handler) {
        Ln.d("authorize: " + _provider + "   apiClientProvider: " + apiClientProvider + "  applicationController: " + applicationController);
        if (_provider != null && apiClientProvider != null && applicationController != null) {
            new AsyncTask() {
                @Override
                protected Object doInBackground(Object[] objects) {
                    boolean ret = authenticateDrone();
                    Ln.d("authenticateDrone: " + ret);
                    if (ret) {
                        handler.onComplete(ResultImpl.success(null));
                    } else {
                        handler.onComplete(ResultImpl.error("Not authorized"));
                    }
                    return null;
                }
            }.execute();
        } else {
            handler.onComplete(ResultImpl.error("Not authorized"));
        }
    }

    private boolean authenticateDrone() {
        this.lock.lock();
        boolean var4;
        try {
            if (email != null && name != null && password != null) {
                LoginTestUserRequest loginTestUserRequest = new LoginTestUserRequest(email, password);
                java.lang.reflect.Field privateStringField = LoginTestUserRequest.class.getDeclaredField("scopes");
                privateStringField.setAccessible(true);
                privateStringField.set(loginTestUserRequest, scope);

                OAuth2Tokens token = apiClientProvider.getUserClient().loginTestUser(loginTestUserRequest).execute().body();
                if (token != null) {
                    token.setExpiresIn(token.getExpiresIn() + System.currentTimeMillis() / 1000);
                    AuthenticatedUser authenticatedUser = new AuthenticatedUser(email, new ActorRecord.ActorKey(email), name, token, "Unknown", (String) null, 0L, (String) null);
                    _provider.setAuthenticatedUser(authenticatedUser);
                    (new AuthenticatedUserTask(applicationController)).execute();
                    return true;
                }
            }

            return _provider.getAuthenticatedUserOrNull() != null;
        } catch (Exception var16) {
            Ln.e(var16);
            var4 = false;
        } finally {
            this.lock.unlock();
        }

        return var4;
    }
}
