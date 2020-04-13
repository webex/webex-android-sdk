/*
 * Copyright 2016-2020 Cisco Systems Inc
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

import android.os.Environment;
import android.os.Handler;
import android.support.test.runner.AndroidJUnitRunner;
import com.ciscowebex.androidsdk.auth.JWTAuthenticator;
import me.helloworld.utils.Checker;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class WebexTestRunner extends AndroidJUnitRunner {

    static Application application;
    static Webex webex;

    public static Webex getWebex() {
        return webex;
    }

    @Override
    public Application newApplication(ClassLoader cl, String className, Context context) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        System.out.println("!!! newApplication !!!");
        application = super.newApplication(cl, Application.class.getName(), context);
        return application;
    }

    @Override
    public void callApplicationOnCreate(Application app) {
        super.callApplicationOnCreate(app);
        new Handler().post(this::login);
    }

    private void login() {
        String jwt = BuildConfig.JWT;
        if (Checker.isEmpty(jwt)) {
            jwt = System.getProperty("JWT");
        }
        System.out.println("!!! login !!! " + jwt);
        JWTAuthenticator authenticator = new JWTAuthenticator();
        authenticator.authorize(jwt);
        webex = new Webex(application, authenticator);
        final CountDownLatch signal = new CountDownLatch(1);
        authenticator.getToken(result -> {
            if (result.isSuccessful()) {
                System.out.println("login isSuccessful!");
            } else {
                System.out.println("login failed! " + result.getError());
                System.exit(-1);
            }
            signal.countDown();
        });

        try {
            signal.await(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private HashMap<String, String > readKeyValueTxtToMap(File file) {
        final HashMap<String, String> keyValueMap = new HashMap<>();
            try {
                final InputStream open = new FileInputStream(file);
                final byte[] readArray = new byte[open.available()];
                open.read(readArray);
                open.close();
                final StringTokenizer allLine = new StringTokenizer(new String(readArray, StandardCharsets.UTF_8), "\r\n");
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
