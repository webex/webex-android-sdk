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

package com.ciscospark.androidsdk;

import java.lang.reflect.Field;

import com.cisco.spark.android.authenticator.OAuth2Tokens;
import com.ciscospark.androidsdk.auth.JWTAuthenticator;

import me.helloworld.utils.reflect.Fields;

import org.junit.Test;

/**
 * Created on 10/06/2017.
 */
public class PhoneImplTest {

    private String auth_token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhbmRyb2lkX3Rlc3R1c2VyXzEiLCJuYW1lIjoiQW5kcm9pZFRlc3RVc2VyMSIsImlzcyI6ImNkNWM5YWY3LThlZDMtNGUxNS05NzA1LTAyNWVmMzBiMWI2YSJ9.eJ99AY9iNDhG4HjDJsY36wgqOnNQSes_PIu0DKBHBzs";

    @Test
    public void test() throws InterruptedException {
        JWTAuthenticator authenticator = new JWTAuthenticator();
        authenticator.authorize(auth_token);
        authenticator.getToken(new CompletionHandler<String>() {
            @Override
            public void onComplete(Result<String> result) {
                System.out.println(result.getData());
                System.out.println(result.getError());
            }
        });
        Thread.sleep(10 * 1000);
    }

    @Test
    public void hello() {
        OAuth2Tokens tokens = new OAuth2Tokens();
        Field f = Fields.findDeclaredField(OAuth2Tokens.class, "refreshToken");
        System.out.println(f);
        System.out.println(f.getDeclaringClass());
        try {
            f.set(tokens, "123344");
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        System.out.println(tokens.getAccessToken());
    }

}