package com.ciscospark.androidsdk.auth;

import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;
import android.webkit.WebView;

import com.cisco.spark.android.BuildConfig;
import com.cisco.spark.android.authenticator.AuthenticatedUserTask;
import com.cisco.spark.android.authenticator.OAuth2Tokens;
import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.ApplicationController;
import com.cisco.spark.android.core.AuthenticatedUser;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.model.LoginTestUserRequest;
import com.cisco.spark.android.sync.ActorRecord;
import com.cisco.spark.android.util.FileUtils;
import com.cisco.spark.android.util.LoggingLock;
import com.ciscospark.androidsdk.CompletionHandler;
import com.ciscospark.androidsdk.auth.internal.OAuthLauncher;
import com.ciscospark.androidsdk.internal.ResultImpl;
import com.ciscospark.androidsdk.internal.SparkInjector;
import com.ciscospark.androidsdk.utils.http.ServiceBuilder;
import com.github.benoitdion.ln.Ln;

import java.util.Properties;

import javax.inject.Inject;

import me.helloworld.utils.Checker;

/**
 * Created by qimdeng on 12/7/17.
 */

public class OAuthTestUserAuthenticator extends OAuthAuthenticator{
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
     * @param clientId the OAuth client id
     * @param clientSecret the OAuth client secret
     * @param scope space-separated string representing which permissions the application needs
     * @param redirectUri the redirect URI that will be called when completing the authentication. This must match the redirect URI registered to your clientId.
     * @see <a href="https://developer.ciscospark.com/authentication.html">Cisco Spark Integration</a>
     * @since 0.1
     */
    public OAuthTestUserAuthenticator(@NonNull String clientId, @NonNull String clientSecret, @NonNull String scope, @NonNull String redirectUri,
                                    @NonNull String email, @NonNull String name, @NonNull String password)
    {
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
     * @since 0.1
     */
    public void authorize(@NonNull CompletionHandler<Void> handler) {
        Ln.d("authorize: " + _provider + "   apiClientProvider: " + apiClientProvider + "  applicationController: " + applicationController);
        if (_provider != null && apiClientProvider != null && applicationController != null){
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
        }else{
            handler.onComplete(ResultImpl.error("Not authorized"));
        }
    }

    private boolean authenticateDrone() {
        this.lock.lock();
        boolean var4;
        try {
            if(email != null && name != null && password != null) {
                LoginTestUserRequest loginTestUserRequest = new LoginTestUserRequest(email, password);
                java.lang.reflect.Field privateStringField = LoginTestUserRequest.class.getDeclaredField("scopes");
                privateStringField.setAccessible(true);
                privateStringField.set(loginTestUserRequest, scope);

                OAuth2Tokens token = (OAuth2Tokens)apiClientProvider.getUserClient().loginTestUser(loginTestUserRequest).execute().body();
                if (token != null) {
                    token.setExpiresIn(token.getExpiresIn() + System.currentTimeMillis() / 1000);
                    AuthenticatedUser authenticatedUser = new AuthenticatedUser(email, new ActorRecord.ActorKey(email), name, token, "Unknown", (String) null, 0L, (String) null);
                    _provider.setAuthenticatedUser(authenticatedUser);
                    (new AuthenticatedUserTask(applicationController)).execute();
                    return true;
                }
            }

            boolean var18 = _provider.getAuthenticatedUserOrNull() != null;
            return var18;
        } catch (Exception var16) {
            Ln.e(var16);
            var4 = false;
        } finally {
            this.lock.unlock();
        }

        return var4;
    }
}
