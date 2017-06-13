package com.ciscospark.auth;

import com.cisco.spark.android.authenticator.OAuth2AccessToken;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created on 10/06/2017.
 */
public class JWTStrategyTest {
    static final String TAG = "JWTStrategyTest";
    private String auth_token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyMiIsIm5hbWUiOiJ1c2VyICMyIiwiaXNzIjoiWTJselkyOXpjR0Z5YXpvdkwzVnpMMDlTUjBGT1NWcEJWRWxQVGk5aU5tSmtNemRtTUMwNU56RXhMVFEzWldVdE9UUTFOUzAxWWpZNE1tUTNNRFV6TURZIn0.5VvjLtuD-jn9hXtLthnGDdhxlIHaoKZbI80y1vK2-bY";

    @Test
    public void authorize() throws Exception {
        JWTStrategy strategy = new JWTStrategy(auth_token);
        strategy.authorize(new AuthorizeListener() {
            @Override
            public void onSuccess(OAuth2AccessToken token) {
                System.out.println(token);
                System.out.println(token.getAccessToken());
                System.out.println("expires in: " + token.getExpiresIn());
            }

            @Override
            public void onFailed() {
                System.out.println("failed");
            }
        });

        Thread.sleep(10000);

    }

    @Test
    public void deauthorize() throws Exception {

    }
}