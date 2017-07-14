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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Allen Xiao<xionxiao@cisco.com>
 * @version 0.1
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class OAuthStrategyTest {
    String clientId = "Cc580d5219555f0df8b03d99f3e020381eae4eee0bad1501ad187480db311cce4";
    String clientSec = "d4e9385b2e5828eef376077995080ea4aa42b5c92f1b6af8f3a59fc6a4e79f6a";
    String redirect = "AndroidDemoApp://response";
    // Every time get the code from browser manually, or test will fail.
    // Visit flowing link in browser to get the code:
    // "https://api.ciscospark.com/v1/authorize?client_id=Cc580d5219555f0df8b03d99f3e020381eae4eee0bad1501ad187480db311cce4&response_type=code&redirect_uri=AndroidDemoApp%3A%2F%2Fresponse&scope=spark%3Aall%20spark%3Akms"
    String code = "MmI5ZDkwY2ItYTUyNi00ODM2LTljZTctZGU5ZjdjNWRkZDBlMWMzN2Q1YWItNTM1";
    String email = "xionxiao@cisco.com";
    String scope = "spark:all spark:kms";

    OAuthStrategy strategy;

    @Before
    public void init() throws Exception {
        strategy = new OAuthStrategy(clientId, clientSec, redirect, scope, email, code);
    }

    @Test
    public void a_authorize() throws Exception {
        strategy.authorize(new AuthorizeListener() {
            @Override
            public void onSuccess() {
                assertTrue(strategy.isAuthorized());
                OAuth2AccessToken token = strategy.accessToken();
                assertNotNull(token);
                assertNotNull(token.getAccessToken());
                assertFalse(token.getAccessToken().isEmpty());
                System.out.println(token.getAccessToken());
                System.out.println("expires in: " + token.getExpiresIn());
                System.out.println("success");
            }

            @Override
            public void onFailed(SparkError error) {
                assertFalse("Every time get the code from browser manually, or test will fail.", true);
            }
        });
        Thread.sleep(10 * 1000);
    }

    @Test
    public void b_deauthorize() throws Exception {
        strategy.deauthorize();
        assertFalse(strategy.isAuthorized());
        OAuth2AccessToken token = strategy.accessToken();
        assertNull(token);
    }

    @Test
    public void c_authorizeFailed() throws Exception {
        strategy.setAuthCode("wrong_code");
        strategy.authorize(new AuthorizeListener() {
            @Override
            public void onSuccess() {
                // not go here
                assertFalse(true);
            }

            @Override
            public void onFailed(SparkError error) {
                assertTrue(true);
            }
        });
        Thread.sleep(10 * 1000);
    }

    @Test
    public void d_authorizeFailed() throws Exception {
        strategy.setScope("wrong_scope");
        strategy.authorize(new AuthorizeListener() {
            @Override
            public void onSuccess() {
                // not go here
                assertFalse(true);
            }

            @Override
            public void onFailed(SparkError error) {
                assertTrue(true);
            }
        });
        Thread.sleep(10 * 1000);
    }
}

