/*
 * Copyright 2016-2021 Cisco Systems Inc
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

package com.ciscowebex.androidsdk.phone.internal;

import android.app.Notification;

import com.ciscowebex.androidsdk.CompletionHandler;
import com.ciscowebex.androidsdk.phone.Call;
import com.ciscowebex.androidsdk.phone.MediaOption;

public abstract class CallContext {

    private MediaOption option;

    public MediaOption getOption() {
        return option;
    }

    public CallContext(MediaOption option) {
        this.option = option;
    }

    static class Incoming extends CallContext {

        private CompletionHandler<Void> callback;
        private CallImpl call;

        public Incoming(CallImpl call, MediaOption option, CompletionHandler<Void> callback) {
            super(option);
            this.callback = callback;
            this.call = call;
        }

        public CompletionHandler<Void> getCallback() {
            return callback;
        }

        public CallImpl getCall() {
            return call;
        }
    }

    static class Outgoing extends CallContext {

        private CompletionHandler<Call> callback;
        private String target;

        public Outgoing(String target, MediaOption option, CompletionHandler<Call> callback) {
            super(option);
            this.target = target;
            this.callback = callback;
        }

        public CompletionHandler<Call> getCallback() {
            return callback;
        }

        public String getTarget() {
            return target;
        }

    }

    static class Sharing extends CallContext {
        private CompletionHandler<Void> callback;
        private CallImpl call;
        private Notification notification;
        private int notificationId;

        public Sharing(CallImpl call, Notification notification, int notificationId, CompletionHandler<Void> callback) {
            super(null);
            this.call = call;
            this.callback = callback;
            this.notification = notification;
            this.notificationId = notificationId;
        }

        public CompletionHandler<Void> getCallback() {
            return callback;
        }

        public CallImpl getCall() {
            return call;
        }

        public Notification getNotification() {
            return notification;
        }

        public int getNotificationId() {
            return notificationId;
        }
    }

}
