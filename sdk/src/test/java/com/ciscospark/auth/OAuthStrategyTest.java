package com.ciscospark.auth;

import com.cisco.spark.android.authenticator.OAuth2AccessToken;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created on 13/06/2017.
 */
public class OAuthStrategyTest {
    String clientId = "Cc580d5219555f0df8b03d99f3e020381eae4eee0bad1501ad187480db311cce4";
    String clientSec = "d4e9385b2e5828eef376077995080ea4aa42b5c92f1b6af8f3a59fc6a4e79f6a";
    String redirect = "AndroidDemoApp://response";
    String code = "Mzg2ZTdmOWItOTc3NS00OWZhLTgyNzYtYTVkOTA4N2IwYjEwZjI0YmIxMGQtNThk";
    String scope = "spark:all spark:kms";

    @Test
    public void authorize() throws Exception {
        OAuthStrategy oAuth = new OAuthStrategy(clientId, clientSec, redirect, scope, "", code);
        oAuth.authorize(new AuthorizeListener() {
            @Override
            public void onSuccess(OAuth2AccessToken token) {
                System.out.println("success");
                System.out.println(token.toString());
                System.out.println(token.getAccessToken());
            }

            @Override
            public void onFailed() {
                System.out.println("failed");

            }
        });
    }

    @Test
    public void deauthorize() throws Exception {

    }

}
