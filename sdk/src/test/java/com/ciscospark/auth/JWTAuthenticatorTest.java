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

import com.ciscospark.CompletionHandler;
import com.ciscospark.SparkError;

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
public class JWTAuthenticatorTest {
    private String auth_token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhbmRyb2lkX3Rlc3R1c2VyXzEiLCJuYW1lIjoiQW5kcm9pZFRlc3RVc2VyMSIsImlzcyI6ImNkNWM5YWY3LThlZDMtNGUxNS05NzA1LTAyNWVmMzBiMWI2YSJ9.eJ99AY9iNDhG4HjDJsY36wgqOnNQSes_PIu0DKBHBzs";
    private JWTAuthenticator authenticator;

    @Before
    public void init() throws Exception {
        authenticator = new JWTAuthenticator(auth_token);
    }

    @Test
    public void a_authorize() throws Exception {
        authenticator.authorize(new CompletionHandler<String>() {
            @Override
            public void onComplete(String authCode) {
                assertTrue(authenticator.isAuthorized());
                assertNotNull(authCode);
                assertFalse(authCode.isEmpty());
                System.out.println(authCode);
            }

            @Override
            public void onError(SparkError error) {
                //assertFalse(true);
                System.out.println("failed");
                System.out.println(error.toString());
            }
        });

        Thread.sleep(5 * 1000);
    }

    @Test
    public void b_deauthorize() throws Exception {
        authenticator.deauthorize();
        assertFalse(authenticator.isAuthorized());
        authenticator.getToken(new CompletionHandler<String>() {
            @Override
            public void onComplete(String result) {

            }

            @Override
            public void onError(SparkError error) {

            }
        });
    }

    @Test
    public void c_authorizeFailed() throws Exception {
        authenticator = new JWTAuthenticator("Wrong token");
        authenticator.authorize(new CompletionHandler<String>() {
            @Override
            public void onComplete(String authCode) {
                assertFalse(true);
            }

            @Override
            public void onError(SparkError error) {
                assertFalse(authenticator.isAuthorized());
                System.out.println(error.toString());
            }
        });

        Thread.sleep(5 * 1000);
    }
}