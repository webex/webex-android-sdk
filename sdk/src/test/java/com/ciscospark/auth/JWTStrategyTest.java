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
            public void onSuccess() {
                System.out.println(strategy.getToken());
                System.out.println(strategy.getToken().getAccessToken());
                System.out.println("expires in: " + strategy.getToken().getExpiresIn());
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