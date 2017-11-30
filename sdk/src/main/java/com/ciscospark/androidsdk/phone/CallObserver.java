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

package com.ciscospark.androidsdk.phone;

import com.ciscospark.androidsdk.SparkError;
import me.helloworld.utils.Objects;
import me.helloworld.utils.annotation.StringPart;

/**
 * An observer interface for {@link Call} event
 * 
 * @since 0.1
 */
public interface CallObserver {

	/**
	 * Callback when remote participant(s) is ringing.
	 * 
	 * @param call Call
	 * @since 0.1   
	 */
	void onRinging(Call call);

	/**
	 * Callback when remote participant(s) answered and the call is connected.
	 * 
	 * @param call Call
	 * @since 0.1   
	 */
	void onConnected(Call call);

	/**
	 * Callback when the call is disconnected (hangup, cancelled, get declined or other self device pickup the call).
	 * 
	 * @param event event
	 * @since 0.1   
	 */
	void onDisconnected(CallDisconnectedEvent event);

	/**
	 * Callback when the media types of the call have changed.
	 * 
	 * @param event event
	 * @since 0.1   
	 */
	void onMediaChanged(MediaChangedEvent event);


	/**
	 * Base class for the event of a call
	 * 
	 * @since 0.1
	 */
	interface CallEvent {

		/**
		 * @return Call
		 * @since 0.1
		 */
		Call getCall();
    }

	/**
	 * Base class for the event of a call
	 * 
	 * @since 0.1
	 */
	abstract class AbstractCallEvent implements CallEvent {

        @StringPart
        protected Call _call;

        protected AbstractCallEvent(Call call) {
            _call = call;
        }

		/**
		 * @see CallEvent
		 */
		public Call getCall() {
            return _call;
        }

        @Override
        public String toString() {
            return Objects.toStringByAnnotation(this);
        }

    }

	/**
	 * This event is fired when the call is disconnected.
	 * 
	 * @since 0.1
	 */
	interface CallDisconnectedEvent extends CallEvent {

    }

	/**
	 * This event is fired when local party has left the call.
	 * 
	 * @since 0.1
	 */
	class LocalLeft extends AbstractCallEvent implements CallDisconnectedEvent {

        public LocalLeft(Call call) {
            super(call);
        }
    }

	/**
	 * This event is fired when the local party has declined the call.
     * This is only applicable when the direction of the call is incoming.
	 * 
	 * @since 0.1
	 */
	class LocalDecline extends AbstractCallEvent implements CallDisconnectedEvent {

        public LocalDecline(Call call) {
            super(call);
        }
    }

	/**
	 * TThis event is fired when the local party has cancelled the call.
     * This is only applicable when the direction of the call is outgoing.
	 * 
	 * @since 0.1
	 */
	class LocalCancel extends AbstractCallEvent implements CallDisconnectedEvent {

        public LocalCancel(Call call) {
            super(call);
        }
    }

	/**
	 * This event is fired when the remote party has left the call.
	 * 
	 * @since 0.1
	 */
	class RemoteLeft extends AbstractCallEvent implements CallDisconnectedEvent {

        public RemoteLeft(Call call) {
            super(call);
        }
    }

	/**
	 * This event is fired when the remote party has declined the call.
     * This is only applicable when the direction of the call is outgoing.
	 * 
	 * @since 0.1
	 */
	class RemoteDecline extends AbstractCallEvent implements CallDisconnectedEvent {

        public RemoteDecline(Call call) {
            super(call);
        }
    }

	/**
	 * This event is fired when the remote party has cancelled the call.
     * This is only applicable when the direction of the call is incoming.
	 * 
	 * @since 0.1
	 */
	class RemoteCancel extends AbstractCallEvent implements CallDisconnectedEvent {

        public RemoteCancel(Call call) {
            super(call);
        }
    }

	/**
	 * This event is fired when one of the other phones of the authenticated user has answered the call.
     * This is only applicable when the direction of the call is incoming.
	 * 
	 * @since 0.1
	 */
	class OtherConnected extends AbstractCallEvent implements CallDisconnectedEvent {

        public OtherConnected(Call call) {
            super(call);
        }
    }

	/**
	 * One of the other phones of the authenticated user has declined the call. This is only applicable when the direction of the call is incoming.
	 * 
	 * @since 0.1
	 */
	class OtherDeclined extends AbstractCallEvent implements CallDisconnectedEvent {

        public OtherDeclined(Call call) {
            super(call);
        }
    }

	/**
	 * This event is fired when the call to be disconnected due to an error.
	 * 
	 * @since 0.1
	 */
	class CallErrorEvent extends AbstractCallEvent implements CallDisconnectedEvent {

        @StringPart
        private SparkError _error;

        public CallErrorEvent(Call call, SparkError error) {
            super(call);
            _error = error;
        }

		/**
		 * @return Error
		 * @since 0.1
		 */
		public SparkError getError() {
            return _error;
        }

        @Override
        public String toString() {
            return Objects.toStringByAnnotation(this);
        }
    }

	/**
	 * Media change event
	 * 
	 * @since 0.1
	 */
	interface MediaChangedEvent extends CallEvent {

    }

	/**
	 * This might be triggered when the remote party muted or unmuted the video.
	 * 
	 * @since 0.1
	 */
	class RemoteSendingVideoEvent extends AbstractCallEvent implements MediaChangedEvent {

        @StringPart
        private boolean _sending;

        public RemoteSendingVideoEvent(Call call, boolean sending) {
            super(call);
            _sending = sending;
        }

		/**
		 * @return True if the remote party now is sending video. Otherwise false.
		 * @since 0.1
		 */
		public boolean isSending() {
            return _sending;
        }

        @Override
        public String toString() {
            return Objects.toStringByAnnotation(this);
        }
    }

	/**
	 * This might be triggered when the remote party muted or unmuted the audio.
	 * 
	 * @since 0.1
	 */
	class RemoteSendingAudioEvent extends AbstractCallEvent implements MediaChangedEvent {

        @StringPart
        private boolean _sending;

        public RemoteSendingAudioEvent(Call call, boolean sending) {
            super(call);
            _sending = sending;
        }

		/**
		 * @return True if the remote party now is sending audio. Otherwise false.
		 * @since 0.1
		 */
		public boolean isSending() {
            return _sending;
        }

        @Override
        public String toString() {
            return Objects.toStringByAnnotation(this);
        }
    }

	/**
	 * This might be triggered when the local party muted or unmuted the video.
	 * 
	 * @since 0.1
	 */
	class SendingVideo extends AbstractCallEvent implements MediaChangedEvent {

        @StringPart
        private boolean _sending;

        public SendingVideo(Call call, boolean sending) {
            super(call);
            _sending = sending;
        }

		/**
		 * @return True if the local party now is sending video. Otherwise false.
		 * @since 0.1
		 */
		public boolean isSending() {
            return _sending;
        }

        @Override
        public String toString() {
            return Objects.toStringByAnnotation(this);
        }
    }

	/**
	 * This might be triggered when the local party muted or unmuted the audio.
	 * 
	 * @since 0.1
	 */
	class SendingAudio extends AbstractCallEvent implements MediaChangedEvent {

        @StringPart
        private boolean _sending;

        public SendingAudio(Call call, boolean sending) {
            super(call);
            _sending = sending;
        }

		/**
		 * @return True if the local party now is sending aduio. Otherwise false.
		 * @since 0.1
		 */
		public boolean isSending() {
            return _sending;
        }

        @Override
        public String toString() {
            return Objects.toStringByAnnotation(this);
        }
    }

	/**
	 * This might be triggered when the local party muted or unmuted the video.
	 * 
	 * @since 0.1
	 */
	class ReceivingVideo extends AbstractCallEvent implements MediaChangedEvent {

        @StringPart
        private boolean _receiving;

        public ReceivingVideo(Call call, boolean receiving) {
            super(call);
            _receiving = receiving;
        }

		/**
		 * @return True if the local party now is receiving video. Otherwise false.
		 * @since 0.1
		 */
		public boolean isReceiving() {
            return _receiving;
        }

        @Override
        public String toString() {
            return Objects.toStringByAnnotation(this);
        }
    }

	/**
	 * This might be triggered when the local party muted or unmuted the audio.
	 * 
	 * @since 0.1
	 */
	class ReceivingAudio extends AbstractCallEvent implements MediaChangedEvent {

        @StringPart
        private boolean _receiving;

        public ReceivingAudio(Call call, boolean receiving) {
            super(call);
            _receiving = receiving;
        }

		/**
		 * @return True if the local party now is receiving audio. Otherwise false.
		 * @since 0.1
		 */
		public boolean isReceiving() {
            return _receiving;
        }

        @Override
        public String toString() {
            return Objects.toStringByAnnotation(this);
        }
    }

	/**
	 * Camera FacingMode on local device has switched.
	 * 
	 * @since 0.1
	 */
	class CameraSwitched extends AbstractCallEvent implements MediaChangedEvent {
        public CameraSwitched(Call call) {
            super(call);
        }
    }

	/**
	 * Local video rendering view size has changed.
	 * 
	 * @since 0.1
	 */
	class LocalVideoViewSizeChanged extends AbstractCallEvent implements MediaChangedEvent {
        public LocalVideoViewSizeChanged(Call call) {
            super(call);
        }
    }

	/**
	 * Remote video rendering view size has changed.
	 * 
	 * @since 0.1
	 */
	class RemoteVideoViewSizeChanged extends AbstractCallEvent implements MediaChangedEvent {
        public RemoteVideoViewSizeChanged(Call call) {
            super(call);
        }
    }

}
