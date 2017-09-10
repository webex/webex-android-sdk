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

package com.ciscospark.androidsdk.phone;


import com.ciscospark.androidsdk.SparkError;
import com.ciscospark.androidsdk.utils.Objects;
import com.ciscospark.androidsdk.utils.annotation.StringPart;

/**
 * Created on 12/06/2017.
 */

public interface CallObserver {

    void onRinging(Call call);

    void onConnected(Call call);

    void onDisconnected(CallDisconnectedEvent event);

    void onMediaChanged(MediaChangedEvent event);

    interface CallEvent {

        Call getCall();
    }

    abstract class AbstractCallEvent implements CallEvent {

        @StringPart
        protected Call _call;

        protected AbstractCallEvent(Call call) {
            _call = call;
        }

        public Call getCall() {
            return _call;
        }

        @Override
        public String toString() {
            return Objects.toStringByAnnotation(this);
        }

    }

    interface CallDisconnectedEvent extends CallEvent {

    }

    class LocalLeft extends AbstractCallEvent implements CallDisconnectedEvent {

        public LocalLeft(Call call) {
            super(call);
        }
    }

    class LocalDecline extends AbstractCallEvent implements CallDisconnectedEvent {

        public LocalDecline(Call call) {
            super(call);
        }
    }

    class LocalCancel extends AbstractCallEvent implements CallDisconnectedEvent {

        public LocalCancel(Call call) {
            super(call);
        }
    }

    class RemoteLeft extends AbstractCallEvent implements CallDisconnectedEvent {

        public RemoteLeft(Call call) {
            super(call);
        }
    }

    class RemoteDecline extends AbstractCallEvent implements CallDisconnectedEvent {

        public RemoteDecline(Call call) {
            super(call);
        }
    }

    class RemoteCancel extends AbstractCallEvent implements CallDisconnectedEvent {

        public RemoteCancel(Call call) {
            super(call);
        }
    }

    class OtherConnected extends AbstractCallEvent implements CallDisconnectedEvent {

        public OtherConnected(Call call) {
            super(call);
        }
    }

    class OtherDeclined extends AbstractCallEvent implements CallDisconnectedEvent {

        public OtherDeclined(Call call) {
            super(call);
        }
    }

    class CallErrorEvent extends AbstractCallEvent implements CallDisconnectedEvent {

        @StringPart
        private SparkError _error;

        public CallErrorEvent(Call call, SparkError error) {
            super(call);
            _error = error;
        }

        public SparkError getError() {
            return _error;
        }

        @Override
        public String toString() {
            return Objects.toStringByAnnotation(this);
        }
    }

    interface MediaChangedEvent extends CallEvent {

    }

    class RemoteSendingVideoEvent extends AbstractCallEvent implements MediaChangedEvent {

        @StringPart
        private boolean _sending;

        public RemoteSendingVideoEvent(Call call, boolean sending) {
            super(call);
            _sending = sending;
        }

        public boolean isSending() {
            return _sending;
        }

        @Override
        public String toString() {
            return Objects.toStringByAnnotation(this);
        }
    }

    class RemoteSendingAudioEvent extends AbstractCallEvent implements MediaChangedEvent {

        @StringPart
        private boolean _sending;

        public RemoteSendingAudioEvent(Call call, boolean sending) {
            super(call);
            _sending = sending;
        }

        public boolean isSending() {
            return _sending;
        }

        @Override
        public String toString() {
            return Objects.toStringByAnnotation(this);
        }
    }

    class SendingVideo extends AbstractCallEvent implements MediaChangedEvent {

        @StringPart
        private boolean _sending;

        public SendingVideo(Call call, boolean sending) {
            super(call);
            _sending = sending;
        }

        public boolean isSending() {
            return _sending;
        }

        @Override
        public String toString() {
            return Objects.toStringByAnnotation(this);
        }
    }

    class SendingAudio extends AbstractCallEvent implements MediaChangedEvent {

        @StringPart
        private boolean _sending;

        public SendingAudio(Call call, boolean sending) {
            super(call);
            _sending = sending;
        }

        public boolean isSending() {
            return _sending;
        }

        @Override
        public String toString() {
            return Objects.toStringByAnnotation(this);
        }
    }

    class ReceivingVideo extends AbstractCallEvent implements MediaChangedEvent {

        @StringPart
        private boolean _receiving;

        public ReceivingVideo(Call call, boolean receiving) {
            super(call);
            _receiving = receiving;
        }

        public boolean isSending() {
            return _receiving;
        }

        @Override
        public String toString() {
            return Objects.toStringByAnnotation(this);
        }
    }

    class ReceivingAudio extends AbstractCallEvent implements MediaChangedEvent {

        @StringPart
        private boolean _receiving;

        public ReceivingAudio(Call call, boolean receiving) {
            super(call);
            _receiving = receiving;
        }

        public boolean isSending() {
            return _receiving;
        }

        @Override
        public String toString() {
            return Objects.toStringByAnnotation(this);
        }
    }

    class CameraSwitched extends AbstractCallEvent implements MediaChangedEvent {
        public CameraSwitched(Call call) {
            super(call);
        }
    }

    class SpearkerSwitched extends AbstractCallEvent implements MediaChangedEvent {
        public SpearkerSwitched(Call call) {
            super(call);
        }
    }

    class LocalVideoViewSize extends AbstractCallEvent implements MediaChangedEvent {
        public LocalVideoViewSize(Call call) {
            super(call);
        }
    }

    class RemoteVideoViewSize extends AbstractCallEvent implements MediaChangedEvent {
        public RemoteVideoViewSize(Call call) {
            super(call);
        }
    }

}
