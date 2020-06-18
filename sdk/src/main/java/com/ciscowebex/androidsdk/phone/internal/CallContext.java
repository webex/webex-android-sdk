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

package com.ciscowebex.androidsdk.phone.internal;

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
        private boolean isModerator;
        private String PIN;

        public Incoming(CallImpl call, MediaOption option, CompletionHandler<Void> callback) {
            this(call, option, false, null, callback);
        }

        public Incoming(CallImpl call, MediaOption option, boolean isModerator, String PIN, CompletionHandler<Void> callback) {
            super(option);
            this.callback = callback;
            this.call = call;
            this.isModerator = isModerator;
            this.PIN = PIN;
        }

        public CompletionHandler<Void> getCallback() {
            return callback;
        }

        public CallImpl getCall() {
            return call;
        }

        public boolean isModerator() {
            return isModerator;
        }

        public String getPIN() {
            return PIN;
        }
    }

    static class Outgoing extends CallContext {

        private CompletionHandler<Call> callback;
        private String target;
        private boolean isModerator;
        private String PIN;

        public Outgoing(String target, MediaOption option, CompletionHandler<Call> callback) {
            this(target, option, false, null, callback);
        }

        public Outgoing(String target, MediaOption option, boolean isModerator, String PIN, CompletionHandler<Call> callback) {
            super(option);
            this.target = target;
            this.callback = callback;
            this.isModerator = isModerator;
            this.PIN = PIN;
        }

        public CompletionHandler<Call> getCallback() {
            return callback;
        }

        public String getTarget() {
            return target;
        }

        public boolean isModerator() {
            return isModerator;
        }

        public String getPIN() {
            return PIN;
        }
    }

    static class Sharing extends CallContext {
        private CompletionHandler<Void> callback;
        private CallImpl call;

        public Sharing(CallImpl call, CompletionHandler<Void> callback) {
            super(null);
            this.call = call;
            this.callback = callback;
        }

        public CompletionHandler<Void> getCallback() {
            return callback;
        }

        public CallImpl getCall() {
            return call;
        }
    }

}
