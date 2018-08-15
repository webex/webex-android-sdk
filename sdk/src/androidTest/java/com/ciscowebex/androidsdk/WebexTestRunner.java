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


import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.support.test.runner.AndroidJUnitRunner;

import com.ciscowebex.androidsdk.auth.OAuthTestUserAuthenticator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


public class WebexTestRunner extends AndroidJUnitRunner {
    private static String SparkUserEmail;
    private static String SparkUserName;
    private static String SparkUserPwd;
    private static String CLIENT_ID;
    private static String CLIENT_SEC;
    private static String REDIRECT_URL;
    private static String SCOPE;

    static Application application;
    static Webex webex;

    public static Webex getSpark() {
        return webex;
    }

    @Override
    public Application newApplication(ClassLoader cl, String className, Context context) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        System.out.println("!!! newApplication !!!");
        application = super.newApplication(cl, Application.class.getName(), context);
        new Handler().post(() -> loginBySparkId());
        return application;
    }

    private void loginBySparkId() {
        System.out.println("!!! loginBySparkId !!!");
        final CountDownLatch signal = new CountDownLatch(1);
        File file = new File(application.getExternalFilesDir("login"), "login.txt");
        if (file.exists()) {
            HashMap map = readKeyValueTxtToMap(file);
            SparkUserEmail = (String) map.get("SparkUserEmail");
            SparkUserName = (String) map.get("SparkUserName");
            SparkUserPwd = (String) map.get("SparkUserPwd");
            CLIENT_ID = (String) map.get("CLIENT_ID");
            CLIENT_SEC = (String) map.get("CLIENT_SEC");
            REDIRECT_URL = (String) map.get("REDIRECT_URL");
            SCOPE = (String) map.get("SCOPE");
            OAuthTestUserAuthenticator auth = new OAuthTestUserAuthenticator(CLIENT_ID, CLIENT_SEC, SCOPE, REDIRECT_URL,
                    SparkUserEmail, SparkUserName, SparkUserPwd);
            webex = new Webex(application, auth);
            auth.authorize(result -> {
                if (result.isSuccessful()) {
                    System.out.println("loginBySparkId isSuccessful!");
                } else {
                    System.out.println("loginBySparkId failed!");
                }
                signal.countDown();
            });
        } else {
            System.out.println("!!! login file is not exist !!!");
        }
        try {
            signal.await(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private HashMap readKeyValueTxtToMap(File file) {
        while (true) {
            final HashMap keyValueMap = new HashMap();
            while (true) {
                try {
                    final InputStream open = new FileInputStream(file);
                    final byte[] readArray = new byte[open.available()];
                    open.read(readArray);
                    open.close();
                    final StringTokenizer allLine = new StringTokenizer(new String(readArray, "UTF-8"), "\r\n");
                    while (allLine.hasMoreTokens()) {
                        final StringTokenizer oneLine = new StringTokenizer(allLine.nextToken(), "=");
                        final String leftKey = oneLine.nextToken();
                        if (!oneLine.hasMoreTokens()) {
                            break;
                        }
                        final String rightValue = oneLine.nextToken();
                        keyValueMap.put(leftKey, rightValue);
                    }
                    return keyValueMap;
                } catch (IOException e) {
                    e.printStackTrace();
                    return keyValueMap;
                }
            }
        }
    }
}
