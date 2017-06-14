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
