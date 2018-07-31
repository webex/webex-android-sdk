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

package com.ciscowebex.androidsdk.message;

import com.ciscowebex.androidsdk.Webex;
import com.ciscowebex.androidsdk.auth.JWTAuthenticator;
import com.ciscowebex.androidsdk.message.internal.MessageClientImpl;
import com.google.gson.Gson;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created by zhiyuliu on 02/09/2017.
 */

public class MessageClientTest {

    private static String auth_token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJreWxlX2RlbW8iLCJuYW1lIjoia3lsZV9kZW1vIiwiaXNzIjoiY2Q1YzlhZjctOGVkMy00ZTE1LTk3MDUtMDI1ZWYzMGIxYjZhIn0.KuabN0TWa00F2Auv4vnu8DXrXAiVM9p1dL8fJUEScbg";
    private static String testPersonId = "Y2lzY29zcGFyazovL3VzL1BFT1BMRS83MmEyYTY4ZS1kNjc5LTRkMTUtOTdlOC1mNzRiZWViYTA2OWU";
    private static JWTAuthenticator authenticator;
    private static MessageClient mClient;
    private static Webex mWebex;
    private static boolean authCompleted = false;

    @BeforeClass
    public static void setUp() throws Exception {
        System.out.println("setup test case");
        authenticator = new JWTAuthenticator();
        authenticator.authorize(auth_token);
//        mClient = new MessageClientImpl(authenticator);
    }

    @Test
    public void post() throws Exception {
        mClient.post(null, null, "zhiyuliu@cisco.com", "Hello", null, null, result -> {
            System.out.println(result);
        });
        Thread.sleep(10 * 1000);
    }

    @Test
    public void test() {

        Gson gson = new Gson();
        Message m = gson.fromJson("{\"spaceType\":\"drr\"}", Message.class);

        System.out.println(gson.toJson(m));


    }

}
