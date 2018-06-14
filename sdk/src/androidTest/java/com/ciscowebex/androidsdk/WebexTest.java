/*
 * Copyright 2016-2017 Cisco Systems Inc
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

package com.ciscowebex.androidsdk;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import android.support.annotation.Nullable;
import android.util.Base64;

import com.ciscowebex.androidsdk.auth.JWTAuthenticator;
import com.google.gson.Gson;

import org.junit.Test;

/**
 * Created on 12/06/2017.
 */
public class WebexTest {
    private String auth_token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyMiIsIm5hbWUiOiJ1c2VyICMyIiwiaXNzIjoiWTJselkyOXpjR0Z5YXpvdkwzVnpMMDlTUjBGT1NWcEJWRWxQVGk5aU5tSmtNemRtTUMwNU56RXhMVFEzWldVdE9UUTFOUzAxWWpZNE1tUTNNRFV6TURZIn0.5VvjLtuD-jn9hXtLthnGDdhxlIHaoKZbI80y1vK2-bY";
    private static final String TAG = "WebexTest";

    @Test
    public void version() throws Exception {
        JWTAuthenticator authenticator = new JWTAuthenticator();
        authenticator.authorize(auth_token);
        Webex webex = new Webex(WebexTestRunner.application, authenticator);
        System.out.println(webex.getVersion());
        System.out.println(webex._mediaEngine);
        authenticator.getToken(System.out::println);
        Thread.sleep(1000000 * 1000);
//        assertEquals(webex.version(), "0.1");
    }

    @Test
    public void authorize() throws Exception {
//        Webex webex = new Webex();
//        JWTAuthenticator strategy = new JWTAuthenticator();
//        webex.setAuthenticator(strategy);
//        webex.authorize(new CompletionHandler<String>() {
//            @Override
//            public void onComplete(String code) {
//                assertTrue(webex.isAuthorized());
//                Log.i(TAG, "get token: " + code);
//            }
//
//            @Override
//            public void onError(WebexError E) {
//                assertFalse(true);
//            }
//        });
//        Thread.sleep(10 * 1000);
    }

    private @Nullable
    Map<String, Object> parseJWT(String jwt) {
        String[] split = jwt.split("\\.");
        if (split.length != 3) {
            return null;
        }
        try {
            String json = new String(Base64.decode(split[1], Base64.URL_SAFE), "UTF-8");
            Gson gson = new Gson();
            Map<String, Object> map = new HashMap<String, Object>();
            return gson.fromJson(json, map.getClass());
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }
}