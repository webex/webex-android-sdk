package com.cisco.spark.android.authenticator;

import com.cisco.spark.android.BuildConfig;
import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.ApplicationController;
import com.cisco.spark.android.core.AuthenticatedUser;
import com.cisco.spark.android.core.Settings;
import com.cisco.spark.android.model.LoginTestUserRequest;
import com.cisco.spark.android.model.User;
import com.cisco.spark.android.sync.ActorRecord;
import com.cisco.spark.android.sync.ActorRecordProvider;
import com.cisco.spark.android.util.FileUtils;
import com.cisco.spark.android.util.LoggingLock;
import com.github.benoitdion.ln.Ln;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

import retrofit2.Response;

public class ApiTokenProvider implements AuthenticatedUserProvider {

    private final Gson gson;
    private final Settings settings;
    private final OAuth2 oAuth2;
    private ActorRecordProvider actorRecordProvider;
    protected AuthenticatedUser authenticatedUser;
    private LoggingLock lock = new LoggingLock(BuildConfig.DEBUG, "ApiTokenProvider Lock");
    private Condition authCondition = lock.newCondition();

    public ApiTokenProvider(Settings settings, Gson gson, OAuth2 oAuth2, ActorRecordProvider actorRecordProvider) {
        this.settings = settings;
        this.gson = gson;
        this.oAuth2 = oAuth2;
        this.actorRecordProvider = actorRecordProvider;
    }

    /**
     * Ask the user to authenticate if necessary This call blocks, so shouldn't be called on the UI
     * thread.
     * <p/>
     * NOTE even though the Drone code has been removed, leaving this in because it's handy for
     * automatically logging in test users.
     *
     * @return whether the user is authenticated
     */
    public boolean authenticateDrone(ApplicationController applicationController, ApiClientProvider apiClientProvider) {
        lock.lock();
        try {
            if (BuildConfig.DEBUG) {
                Properties loginInfo = FileUtils.readPropertiesFileFromExternalStorage("drone_login_info.txt");

                if (loginInfo.size() > 0) {
                    String email = loginInfo.getProperty("email");
                    String name = loginInfo.getProperty("name");
                    String password = loginInfo.getProperty("password");
                    String clientId = loginInfo.getProperty("clientId");
                    String clientSecret = loginInfo.getProperty("clientSecret");
                    if (clientId != null & clientSecret != null) {
                        oAuth2.setClientId(clientId);
                        oAuth2.setClientSecret(clientSecret);
                    }

                    if (email != null && name != null && password != null) {
                        settings.setSuppressCoachmarks(true);

                        LoginTestUserRequest loginTestUserRequest = new LoginTestUserRequest(email, password);
                        OAuth2Tokens token = apiClientProvider.getUserClient().loginTestUser(loginTestUserRequest).execute().body();

                        AuthenticatedUser authenticatedUser = new AuthenticatedUser(email, new ActorRecord.ActorKey(email), name, token, "Unknown", null, 0, null);
                        setAuthenticatedUser(authenticatedUser);
                        new AuthenticatedUserTask(applicationController).execute();
                        return true;
                    }
                }
            }

            return authenticatedUser != null;
        } catch (Exception e) {
            Ln.e(e);
            return false;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Clear the account of the authenticated user.
     */
    public void clearAccount() {
        storeAuthenticatedUser(null);
    }

    public void setAuthenticatedUser(AuthenticatedUser authenticatedUser) {
        lock.lock();
        try {
            this.authenticatedUser = authenticatedUser;
            actorRecordProvider.monitor(ActorRecord.newInstance(authenticatedUser), false);
            storeAuthenticatedUser(authenticatedUser);
        } finally {
            lock.unlock();
        }
    }

    protected void storeAuthenticatedUser(AuthenticatedUser authenticatedUser) {
        lock.lock();
        try {
            this.authenticatedUser = authenticatedUser;
            settings.setAuthenticatedUser(authenticatedUser);
            if (authenticatedUser != null)
                authCondition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public AuthenticatedUser getAuthenticatedUser() {
        lock.lock();
        try {
            AuthenticatedUser user = getAuthenticatedUserOrNull();
            if (user == null) {
                throw new NotAuthenticatedException();
            }
            return user;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public AuthenticatedUser getAuthenticatedUserOrNull() {
        lock.lock();
        try {
            if (authenticatedUser == null) {
                loadAuthenticatedUser();
            }
            return authenticatedUser;
        } finally {
            lock.unlock();
        }
    }

    protected void loadAuthenticatedUser() {
        lock.lock();
        try {
            authenticatedUser = settings.getAuthenticatedUser();
            if (authenticatedUser != null)
                authCondition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public void requestAuthenticatedUser(OAuth2Tokens tokens, ApiClientProvider apiClientProvider, String authorizationHeader) {
        try {
            Response<User> resp = apiClientProvider.getUserClient().getUser(authorizationHeader).execute();
            if (resp.isSuccessful()) {
                User user = resp.body();
                this.setAuthenticatedUser(new AuthenticatedUser(user.getEmail(), user.getActorKey(), user.getDisplayName(), tokens, user.getDepartment(), user.getOrgId(), user.createdInMillis(), user.getType()));
            }
        } catch (IOException e) {
            Ln.w(e);
        }
    }

    @Override
    public boolean isAuthenticated() {
        lock.lock();
        try {
            if (authenticatedUser == null) {
                loadAuthenticatedUser();
            }
            return authenticatedUser != null;
        } finally {
            lock.unlock();
        }
    }

    public void setDisplayName(String displayName) {
        authenticatedUser.setDisplayName(displayName);
        storeAuthenticatedUser(this.authenticatedUser);
    }

    @Override
    public void updateTokens(OAuth2AccessToken conversationTokens, OAuth2AccessToken kmsTokens) {
        lock.lock();
        try {
            if (authenticatedUser != null) {
                authenticatedUser.setConversationTokens(conversationTokens);
                authenticatedUser.setKmsTokens(kmsTokens);
                storeAuthenticatedUser(authenticatedUser);
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void replaceTokens(OAuth2Tokens tokens, OAuth2AccessToken conversationTokens, OAuth2AccessToken kmsTokens) {
        lock.lock();
        try {
            if (authenticatedUser != null) {
                authenticatedUser.setTokens(tokens);
                updateTokens(conversationTokens, kmsTokens);
            }
        } finally {
            lock.unlock();
        }
    }

    public boolean waitUntilAuthenticated(long time, TimeUnit timeUnit) {
        lock.lock();
        try {
            if (isAuthenticated())
                return true;
            authCondition.await(time, timeUnit);
        } catch (InterruptedException e) {
            Ln.i(e, "Interrupted waiting for authentication");
        } finally {
            lock.unlock();
        }
        return isAuthenticated();
    }
}
