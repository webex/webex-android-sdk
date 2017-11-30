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

package com.ciscospark.androidsdk.people;

import com.ciscospark.androidsdk.Spark;
import com.ciscospark.androidsdk.auth.JWTAuthenticator;

/**
 * Created on 27/08/2017.
 */
public class PersonClientTest {
    private static String auth_token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhbmRyb2lkX3Rlc3R1c2VyXzEiLCJuYW1lIjoiQW5kcm9pZFRlc3RVc2VyMSIsImlzcyI6ImNkNWM5YWY3LThlZDMtNGUxNS05NzA1LTAyNWVmMzBiMWI2YSJ9.eJ99AY9iNDhG4HjDJsY36wgqOnNQSes_PIu0DKBHBzs";
    private static String testPersonId = "Y2lzY29zcGFyazovL3VzL1BFT1BMRS84MWM1MzkwOC1jMDIxLTRkOWQtOTk0Ny0xMDJlMzQ3ODMwMDc";
    private static JWTAuthenticator authenticator;
    private static PersonClient mClient;
    private static Spark mSpark;
    private static boolean authCompleted = false;

//    @BeforeClass
//    public static void setUp() throws Exception {
//        System.out.println("setup test case");
//        authenticator = new JWTAuthenticator();
//        mSpark = new Spark(authenticator);
//        mSpark.authorize(new CompletionHandler<String>() {
//            @Override
//            public void onComplete(String result) {
//                System.out.println(result);
//                authCompleted = true;
//            }
//
//            @Override
//            public void onError(SparkError error) {
//                System.out.println(error.toString());
//                assertFalse(true);
//            }
//        });
//
//        int time_wait = 0;
//        while(!authCompleted && ++time_wait < 10) {
//            System.out.println(time_wait + "s");
//            Thread.sleep(1000);
//        }
//
//        if (authenticator.isAuthorized()) {
//            mClient = new PersonClient(mSpark);
//        }
//    }
//
//    @Test
//    public void list() throws Exception {
//        if (mSpark.isAuthorized()) {
//            mClient.list("xionxiao@cisco.com", null, 3, new CompletionHandler<List<Person>>() {
//                @Override
//                public void onComplete(List<Person> result) {
//                    System.out.println(result.toString());
//                    assertTrue(true);
//                }
//
//                @Override
//                public void onError(SparkError error) {
//                    System.out.println(error.toString());
//                    assertFalse(true);
//                }
//            });
//        } else {
//            assertFalse(true);
//        }
//        Thread.sleep(10 * 1000);
//    }
//
//    @Test
//    public void get() throws Exception {
//        if (mSpark.isAuthorized()) {
//            mClient.get(testPersonId, new CompletionHandler<Person>() {
//                @Override
//                public void onComplete(Person result) {
//                    System.out.println(result.toString());
//                    assertTrue(true);
//                }
//
//                @Override
//                public void onError(SparkError error) {
//                    System.out.println(error.toString());
//                    assertFalse(true);
//
//                }
//            });
//        } else {
//            assertFalse(true);
//        }
//        Thread.sleep(10 * 1000);
//    }
//
//    @Test
//    public void getMe() throws Exception {
//        if (mSpark.isAuthorized()) {
//            mClient.getMe(new CompletionHandler<Person>() {
//                @Override
//                public void onComplete(Person result) {
//                    System.out.println(result.toString());
//                    assertNotNull(result);
//                }
//
//                @Override
//                public void onError(SparkError error) {
//                    System.out.println(error.toString());
//                    assertFalse(true);
//                }
//            });
//        } else {
//            assertFalse(true);
//        }
//        Thread.sleep(10 * 1000);
//    }
}