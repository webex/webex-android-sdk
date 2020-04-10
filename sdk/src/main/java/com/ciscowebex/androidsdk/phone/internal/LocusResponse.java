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

import android.content.Intent;
import com.ciscowebex.androidsdk.CompletionHandler;
import com.ciscowebex.androidsdk.internal.Device;
import com.ciscowebex.androidsdk.internal.model.FloorModel;
import com.ciscowebex.androidsdk.internal.model.LocusModel;

public interface LocusResponse {

    class Call implements LocusResponse {

        private Device device;
        private MediaSession session;
        private LocusModel locus;
        private CompletionHandler<com.ciscowebex.androidsdk.phone.Call> callback;

        public Call(Device device,
                    MediaSession session,
                    LocusModel model,
                    CompletionHandler<com.ciscowebex.androidsdk.phone.Call> callback) {
            this.device = device;
            this.session = session;
            this.locus = model;
            this.callback = callback;
        }

        public Device getDevice() {
            return device;
        }

        public MediaSession getSession() {
            return session;
        }

        public LocusModel getLocus() {
            return locus;
        }

        public CompletionHandler<com.ciscowebex.androidsdk.phone.Call> getCallback() {
            return callback;
        }
    }

    class Answer extends WithResult {

        public Answer(CallImpl call, LocusModel result, CompletionHandler<Void> callback) {
            super(call, result, callback);
        }
    }

    class Leave extends WithResult {

        public Leave(CallImpl call, LocusModel result, CompletionHandler<Void> callback) {
            super(call, result, callback);
        }
    }

    class Update extends WithResult {

        public Update(CallImpl call, LocusModel result, CompletionHandler<Void> callback) {
            super(call, result, callback);
        }
    }

    class Reject extends WithoutResult {

        public Reject(CallImpl call, CompletionHandler<Void> callback) {
            super(call, callback);
        }
    }

    class Ack extends WithoutResult {

        public Ack(CallImpl call, CompletionHandler<Void> callback) {
            super(call, callback);
        }
    }

    class MediaShare extends WithoutResult {

        private Intent sharing;
        private FloorModel.Disposition disposition;

        public MediaShare(CallImpl call, FloorModel.Disposition disposition, Intent sharing, CompletionHandler<Void> callback) {
            super(call, callback);
            this.sharing = sharing;
            this.disposition = disposition;
        }

        public Intent getIntent() {
            return sharing;
        }

        public FloorModel.Disposition getDisposition() {
            return disposition;
        }
    }

    abstract class WithoutResult extends CallRelated {

        public WithoutResult(CallImpl call, CompletionHandler<Void> callback) {
            super(call, callback);
        }

    }

    abstract class WithResult extends CallRelated {

        private LocusModel result;

        public WithResult(CallImpl call, LocusModel result, CompletionHandler<Void> callback) {
            super(call, callback);
            this.result = result;
        }

        public LocusModel getResult() {
            return result;
        }

    }

    abstract class CallRelated implements LocusResponse {

        private CallImpl call;
        private CompletionHandler<Void> callback;

        public CallRelated(CallImpl call, CompletionHandler<Void> callback) {
            this.call = call;
            this.callback = callback;
        }

        public CallImpl getCall() {
            return call;
        }

        public CompletionHandler<Void> getCallback() {
            return callback;
        }
    }

}
