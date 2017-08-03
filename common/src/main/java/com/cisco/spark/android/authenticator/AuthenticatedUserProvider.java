package com.cisco.spark.android.authenticator;

import com.cisco.spark.android.core.AuthenticatedUser;

public interface AuthenticatedUserProvider {

    AuthenticatedUser getAuthenticatedUser();

    AuthenticatedUser getAuthenticatedUserOrNull();

    void setAuthenticatedUser(AuthenticatedUser authenticatedUser);

    boolean isAuthenticated();

    void updateTokens(OAuth2AccessToken tokens, OAuth2AccessToken conversationTokens, OAuth2AccessToken kmsTokens, OAuth2AccessToken voicemailTokens);

    // Exposed for testing
    void replaceTokens(OAuth2Tokens tokens, OAuth2AccessToken conversationTokens, OAuth2AccessToken kmsTokens, OAuth2AccessToken voicemailTokens);
}
