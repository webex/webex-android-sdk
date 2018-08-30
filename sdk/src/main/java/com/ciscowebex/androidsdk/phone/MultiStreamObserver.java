package com.ciscowebex.androidsdk.phone;

import android.view.View;

import com.ciscowebex.androidsdk.Result;
import com.ciscowebex.androidsdk.WebexError;
import com.ciscowebex.androidsdk.internal.ResultImpl;

import me.helloworld.utils.Objects;
import me.helloworld.utils.annotation.StringPart;

/**
 * Created by qimdeng on 8/29/18.
 */

public interface MultiStreamObserver {
    /**
     *
     *
     * @param event event
     * @since 2.0.0
     */
    void onAuxStreamChanged(AuxStreamChangedEvent event);

    /**
     *
     *
     * @since 2.0.0
     */
    View onAuxStreamAvailable();

    /**
     *
     *
     * @since 2.0.0
     */
    View onAuxStreamUnavailable();

    /**
     * Remote aux video change event
     *
     * @since 2.0.0
     */
    interface AuxStreamChangedEvent extends CallObserver.CallEvent {
        AuxStream getAuxStream();
    }

    /**
     * Base class for the event of a RemoteAuxVideoChangeEvent
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
         * @return changed AuxStream.
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

    class AuxStreamOpenedEvent extends AbstractAuxStreamChangedEvent {
        @StringPart
        protected View _view;

        @StringPart
        protected WebexError _error;

        public AuxStreamOpenedEvent(Call call, View view, String error) {
            super(call, error == null ? call.getAuxStream(view) : null);
            _view = view;
            if (error != null)
                _error = new WebexError(WebexError.ErrorCode.UNEXPECTED_ERROR, error);
        }

        public boolean isSuccessful(){
            return _error == null;
        }

        public View getRenderView(){
            return _view;
        }

        public WebexError getError() {return _error;}
    }

    class AuxStreamClosedEvent extends AbstractAuxStreamChangedEvent {
        @StringPart
        protected android.view.View _view;

        @StringPart
        protected WebexError _error;

        public AuxStreamClosedEvent(Call call, View view, String error) {
            super(call, null);
            _view = view;
            if (error != null)
                _error = new WebexError(WebexError.ErrorCode.UNEXPECTED_ERROR, error);
        }

        public boolean isSuccessful(){
            return _error == null;
        }

        public View getRenderView(){
            return _view;
        }

        public WebexError getError() {return _error;}
    }

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

        public CallMembership from(){
            return _from;
        }

        public CallMembership to(){
            return _to;
        }
    }

    class AuxStreamSizeChangedEvent extends AbstractAuxStreamChangedEvent {

        public AuxStreamSizeChangedEvent(Call call, AuxStream auxStream) {
            super(call, auxStream);
        }
    }

    class AuxStreamSendingVideoEvent extends AbstractAuxStreamChangedEvent {

        public AuxStreamSendingVideoEvent(Call call, AuxStream auxStream) {
            super(call, auxStream);
        }
    }
}
