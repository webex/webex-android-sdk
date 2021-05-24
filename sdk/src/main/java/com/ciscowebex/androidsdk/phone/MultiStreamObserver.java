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

package com.ciscowebex.androidsdk.phone;

import android.view.View;

import com.ciscowebex.androidsdk.WebexError;

import me.helloworld.utils.Objects;
import me.helloworld.utils.annotation.StringPart;

/**
 * Created by qimdeng on 8/29/18.
 *
 * The interface of multi stream. Client should implement this interface to support the multi-stream feature.
 *
 * @since 2.0.0
 */

public interface MultiStreamObserver {
    /**
     * Callback when an auxiliary stream is changed
     *
     * @param event the auxiliary stream changed event
     * @since 2.0.0
     */
    void onAuxStreamChanged(AuxStreamChangedEvent event);

    /**
     * Callback of SDK when there is a new available auxiliary stream.
     * Client should give SDK a view handle for rendering, and the AuxStreamOpenedEvent would be triggered indicating whether the stream is successfully opened.
     * If the client don't want to open stream at this time, return null
     *
     * @return the view to be rendered
     * @since 2.0.0
     */
    View onAuxStreamAvailable();

    /**
     * Callback of SDK when there is a auxiliary stream unavailable.
     * Client should give SDK a view which will be closed or if the given view is null, SDK will automatically close the last opened stream if needed.
     *
     * @return the view of auxiliary stream to be closed
     * @since 2.0.0
     */
    View onAuxStreamUnavailable();

    /**
     * Interface for an auxiliary stream changed event
     *
     * @since 2.0.0
     */
    interface AuxStreamChangedEvent extends CallObserver.CallEvent {
        AuxStream getAuxStream();
    }

    /**
     * Base class for the event of an AuxStreamChangedEvent
     *
     * @since 2.0.0
     */
    abstract class AbstractAuxStreamChangedEvent extends CallObserver.AbstractCallEvent implements AuxStreamChangedEvent{

        @StringPart
        protected AuxStream _auxStream;

        protected AbstractAuxStreamChangedEvent(Call call, AuxStream auxStream) {
            super(call);
            _auxStream = auxStream;
        }

        /**
         * @return the changed AuxStream.
         * @since 2.0.0
         */
        public AuxStream getAuxStream() {
            return _auxStream;
        }

        @Override
        public String toString() {
            return Objects.toStringByAnnotation(this);
        }
    }

    /**
     * This will be triggered when auxiliary stream is opened successfully or unsuccessfully.
     * On this event, the client can hide the view
     *
     * @since 2.0.0
     */
    class AuxStreamOpenedEvent extends AbstractAuxStreamChangedEvent {
        @StringPart
        protected View _view;

        @StringPart
        protected WebexError _error;

        public AuxStreamOpenedEvent(Call call, View view, String error) {
            super(call, error == null ? call.getAuxStream(view) : null);
            _view = view;
            if (error != null) {
                _error = WebexError.from(error);
            }
        }

        /**
         * @return the result of opening an auxiliary stream
         *
         * @since 2.0.0
         */
        public boolean isSuccessful(){
            return _error == null;
        }

        /**
         * @return the rendering view of the opened auxiliary stream
         *
         * @since 2.0.0
         */
        public View getRenderView(){
            return _view;
        }

        /**
         * @return the error message if unsuccessfully
         *
         * @since 2.0.0
         */
        public WebexError getError() {return _error;}
    }

    /**
     * This will be triggered when auxiliary stream is closed successfully or unsuccessfully.
     * On this event, the client can display the view
     *
     * @since 2.0.0
     */
    class AuxStreamClosedEvent extends AbstractAuxStreamChangedEvent {
        @StringPart
        protected android.view.View _view;

        @StringPart
        protected WebexError _error;

        public AuxStreamClosedEvent(Call call, View view, String error) {
            super(call, null);
            _view = view;
            if (error != null) {
                _error = WebexError.from(error);
            }
        }

        /**
         * @return the result of closing an auxiliary stream
         *
         * @since 2.0.0
         */
        public boolean isSuccessful(){
            return _error == null;
        }

        /**
         * @return the rendering view of the closed auxiliary stream
         *
         * @since 2.0.0
         */
        public View getRenderView(){
            return _view;
        }

        /**
         * @return the error message if unsuccessfully
         *
         * @since 2.0.0
         */
        public WebexError getError() {return _error;}
    }

    /**
     * This will be triggered when the person represented this auxiliary stream is changed
     *
     * @since 2.0.0
     */
    class AuxStreamPersonChangedEvent extends AbstractAuxStreamChangedEvent {
        @StringPart
        private CallMembership _from;

        @StringPart
        private CallMembership _to;

        public AuxStreamPersonChangedEvent(Call call, AuxStream auxStream, CallMembership from, CallMembership to) {
            super(call, auxStream);
            _from = from;
            _to = to;
        }

        /**
         * @return the former person represented this auxiliary stream
         *
         * @since 2.0.0
         */
        public CallMembership from(){
            return _from;
        }

        /**
         * @return the new person represented this auxiliary stream
         *
         * @since 2.0.0
         */
        public CallMembership to(){
            return _to;
        }
    }

    /**
     * This will be triggered when the auxiliary stream view size is changed, and client can get the detail from the property
     * @see AuxStream#getSize()
     * @since 2.0.0
     */
    class AuxStreamSizeChangedEvent extends AbstractAuxStreamChangedEvent {

        public AuxStreamSizeChangedEvent(Call call, AuxStream auxStream) {
            super(call, auxStream);
        }
    }

    /**
     * This will be triggered when the network is unstable or the represented person muted or unmuted his video,
     * and client can get the detail from the property
     * @see AuxStream#isSendingVideo()
     * @since 2.0.0
     */
    class AuxStreamSendingVideoEvent extends AbstractAuxStreamChangedEvent {

        public AuxStreamSendingVideoEvent(Call call, AuxStream auxStream) {
            super(call, auxStream);
        }
    }
}
