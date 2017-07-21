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

package com.ciscospark.auth;

import com.cisco.spark.android.authenticator.OAuth2AccessToken;
import com.ciscospark.common.SparkError;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Allen Xiao<xionxiao@cisco.com>
 * @version 0.1
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class JWTAuthenticatorTest {
    private String auth_token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyMiIsIm5hbWUiOiJ1c2VyICMyIiwiaXNzIjoiWTJselkyOXpjR0Z5YXpvdkwzVnpMMDlTUjBGT1NWcEJWRWxQVGk5aU5tSmtNemRtTUMwNU56RXhMVFEzWldVdE9UUTFOUzAxWWpZNE1tUTNNRFV6TURZIn0.5VvjLtuD-jn9hXtLthnGDdhxlIHaoKZbI80y1vK2-bY";
    private JWTAuthenticator strategy;

    @Before
    public void init() throws Exception {
        strategy = new JWTAuthenticator();
    }

    @Test
    public void a_authorize() throws Exception {
        strategy.authorize(auth_token);
        assertTrue(strategy.isAuthorized());
        if (strategy.isAuthorized()) {
        strategy.accessToken(new AuthorizeListener() {
                @Override
                public void onSuccess(OAuth2AccessToken access_token) {
                    assertTrue(strategy.isAuthorized());
                    assertNotNull(access_token);
                    assertFalse(access_token.getAccessToken().isEmpty());
                    System.out.println(access_token.getAccessToken());
                    System.out.println(access_token.getExpiresIn());
                }

                @Override
                public void onFailed(SparkError error) {
                    assertFalse(true);
                    System.out.println("failed");
                    System.out.println(error.toString());
                }
            });
        }

        Thread.sleep(2 * 1000);
    }

    @Test
    public void b_deauthorize() throws Exception {
        strategy.deauthorize();
        assertFalse(strategy.isAuthorized());
    }

    @Test
    public void c_authorizeFailed() throws Exception {
        strategy = new JWTAuthenticator();
        strategy.authorize("wrong token");
        assertTrue(strategy.isAuthorized());
        if (strategy.isAuthorized()) {
            strategy.accessToken(new AuthorizeListener() {
                @Override
                public void onSuccess(OAuth2AccessToken access_token) {
                    assertFalse(true);
                }

                @Override
                public void onFailed(SparkError error) {
                    assertFalse(strategy.isAuthorized());
                    System.out.println(error.toString());
                }
            });
        }

        Thread.sleep(2 * 1000);
    }
}