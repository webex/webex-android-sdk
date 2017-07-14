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

package com.ciscospark.phone;

import android.util.Log;

import com.cisco.spark.android.authenticator.ApiTokenProvider;
import com.cisco.spark.android.authenticator.AuthenticatedUserTask;
import com.cisco.spark.android.authenticator.OAuth2AccessToken;
import com.cisco.spark.android.authenticator.OAuth2Tokens;
import com.cisco.spark.android.callcontrol.CallControlService;
import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.ApplicationController;
import com.cisco.spark.android.core.AuthenticatedUser;
import com.cisco.spark.android.media.MediaEngine;
import com.cisco.spark.android.sync.ActorRecord;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

public class Phone {
    private OAuth2AccessToken access_token;
    private static final String TAG = "Phone";

    /*
    @Inject
    ApplicationController applicationController;

    @Inject
    ApiTokenProvider apiTokenProvider;

    @Inject
    ApiClientProvider apiClientProvider;

    @Inject
    CallControlService callControlService;
    */

    @Inject
    MediaEngine mediaEngine;

    @Inject
    EventBus bus;

    public Phone(OAuth2AccessToken token) {
        Log.d(TAG, "constructure");
        this.access_token = token;
    }

    public void register(RegisterListener listener) {
        OAuth2Tokens tokens = (OAuth2Tokens)access_token;

        String email = "";
        String name = "";

        //AuthenticatedUser authenticatedUser = new AuthenticatedUser(email, new ActorRecord.ActorKey(email), name, tokens, "Unknown", null, 0, null);
        //apiTokenProvider.setAuthenticatedUser(authenticatedUser);
        //new AuthenticatedUserTask(applicationController).execute();
    }

    public void deregister(DeregisterListener listener) {

    }

    public Call dial(String dialString) {
        return null;
    }

    public void startPreView() {

    }

    public void stopPreView() {

    }

    public boolean isConnected() {
        return false;
    }

}
