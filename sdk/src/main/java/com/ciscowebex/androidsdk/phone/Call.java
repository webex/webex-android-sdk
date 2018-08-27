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

package com.ciscowebex.androidsdk.phone;

import java.util.List;

import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.ciscowebex.androidsdk.CompletionHandler;

import android.view.View;
import android.util.Pair;

/**
 * A Call represents a media call on Cisco Webex.
 * <p>
 * The application can create an outgoing call by calling {@link Phone}.dial function:
 * <p>
 * The application can receive an incoming call on {@link com.ciscowebex.androidsdk.phone.Phone.IncomingCallListener}:
 *
 * @see Phone
 * @since 0.1
 */
public interface Call {

    /**
     * The enumeration of directions of a call
     *
     * @since 0.1
     */
    enum Direction {
        /**
         * The local party is a recipient of the call.
         *
         * @since 0.1
         */
        INCOMING,
        /**
         * The local party is an initiator of the call.
         *
         * @since 0.1
         */
        OUTGOING
    }

    /**
     * The status of a Call.
     *
     * @since 0.1
     */
    enum CallStatus {
        /**
         * For the outgoing call, the call has dialed.
         * For the incoming call, the call has received.
         *
         * @since 0.1
         */
        INITIATED,
        /**
         * For the outgoing call, the call is ringing the remote party.
         * For the incoming call, the call is ringing the local party.
         *
         * @since 0.1
         */
        RINGING,
        /**
         * The call is answered.
         *
         * @since 0.1
         */
        CONNECTED,
        /**
         * The call is terminated.
         *
         * @since 0.1
         */
        DISCONNECTED
    }

    /**
     * @return The camera facing mode selected for this call.
     * @since 0.1
     */
    Phone.FacingMode getFacingMode();

    /**
     * @param facingMode The camera facing mode.
     * @since 0.1
     */
    void setFacingMode(Phone.FacingMode facingMode);

    /**
     * @return The direction of this call.
     * @since 0.1
     */
    Direction getDirection();

    /**
     * @return The status of this call.
     * @since 0.1
     */
    CallStatus getStatus();

    /**
     * @param observer Observer for the events of this call
     * @since 0.1
     */
    void setObserver(CallObserver observer);

    /**
     * @return Observer for the events of this call
     * @since 0.1
     */
    CallObserver getObserver();

    /**
     * @return Call Memberships represent participants in this call.
     * @since 0.1
     */
    List<CallMembership> getMemberships();

    /**
     * @return The initiator of this call.
     * @since 0.1
     */
    CallMembership getFrom();

    /**
     * @return The intended recipient of this call.
     * @since 0.1
     */
    CallMembership getTo();

    /**
     * @return The local video render view dimensions (points) of this call.
     * @since 0.1
     */
    Rect getLocalVideoViewSize();

    /**
     * @return The remote video render view dimensions (points) of this call.
     * @since 0.1
     */
    Rect getRemoteVideoViewSize();

    /**
     * @return The content sharing render view dimensions (points) of this call.
     * @since 1.3.0
     */
    Rect getSharingViewSize();

    /**
     * @return True if the remote party of this call is sending video. Otherwise, false.
     * @since 0.1
     */
    boolean isRemoteSendingVideo();

    /**
     * @return True if the remote party of this call is sending audio. Otherwise, false.
     * @since 0.1
     */
    boolean isRemoteSendingAudio();

    /**
     * @return True if the remote party of this call is sending content sharing. Otherwise, false.
     * @since 1.3.0
     */
    boolean isRemoteSendingSharing();

    /**
     * @return True if this call is sending video. Otherwise, false.
     * @since 0.1
     */
    boolean isSendingVideo();

    /**
     * @param sending True if this call is sending video. Otherwise, false.
     * @since 0.1
     */
    void setSendingVideo(boolean sending);

    /**
     * @return True if this call is sending audio. Otherwise, false.
     * @since 0.1
     */
    boolean isSendingAudio();

    /**
     * @param sending True if this call is sending audio. Otherwise, false.
     * @since 0.1
     */
    void setSendingAudio(boolean sending);

    /**
     * @return True if the local party of this call is receiving video. Otherwise, false.
     * @since 0.1
     */
    boolean isReceivingVideo();

    /**
     * @param receiving True if the local party of this *call* is receiving video. Otherwise, false.
     * @since 0.1
     */
    void setReceivingVideo(boolean receiving);

    /**
     * @return True if the local party of this *call* is receiving audio. Otherwise, false.
     * @since 0.1
     */
    boolean isReceivingAudio();

    /**
     * @param receiving True if the local party of this *call* is receiving audio. Otherwise, false.
     * @since 0.1
     */
    void setReceivingAudio(boolean receiving);

    /**
     * @return True if the local party of this call is receiving content sharing. Otherwise, false.
     * @since 1.3.0
     */
    boolean isReceivingSharing();

    /**
     * @param receiving True if the local party of this *call* is receiving content sharing. Otherwise, false.
     * @since 1.3.0
     */
    void setReceivingSharing(boolean receiving);

    /**
     * @return The render views for local and remote video of this call.
     * @since 1.3.0
     */
    Pair<View, View> getVideoRenderViews();

    /**
     * @param videoRenderViews render views for local and remote video of this call. If is nil, it will update the video state as inactive to the server side.
     * @since 1.3.0
     */
    void setVideoRenderViews(@Nullable Pair<View, View> videoRenderViews);


    /**
     * @return The render view for the remote content sharing of this call.
     * @since 1.3.0
     */
    View getSharingRenderView();

    /**
     * @param view The render view for the remote content sharing of this call. If is nil, it will update the content sharing state as inactive to the server side.
     * @since 1.3.0
     */
    void setSharingRenderView(View view);

    /**
     * Acknowledge (without answering) an incoming call. Will cause the initiator's Call instance to emit the ringing event.
     *
     * @param callback A closure to be executed when completed, with error if the invocation is illegal or failed, otherwise nil.
     * @see CallStatus
     * @since 0.1
     */
    void acknowledge(@NonNull CompletionHandler<Void> callback);

    /**
     * Answers this call. This can only be invoked when this call is incoming and in ringing status.
     *
     * @param option   Intended media options - audio only or audio and video - for the call.
     * @param callback A closure to be executed when completed, with error if the invocation is illegal or failed, otherwise nil.
     * @see CallStatus
     * @since 0.1
     */
    void answer(@NonNull MediaOption option, @NonNull CompletionHandler<Void> callback);

    /**
     * Rejects this call. This can only be invoked when this call is incoming and in ringing status.
     *
     * @param callback A closure to be executed when completed, with error if the invocation is illegal or failed, otherwise nil.
     * @see CallStatus
     * @since 0.1
     */
    void reject(@NonNull CompletionHandler<Void> callback);

    /**
     * Disconnects this call. This can only be invoked when this call is in answered status.
     *
     * @param callback A closure to be executed when completed, with error if the invocation is illegal or failed, otherwise nil.
     * @see CallStatus
     * @since 0.1
     */
    void hangup(@NonNull CompletionHandler<Void> callback);

    /**
     * @return True if the DTMF keypad is enabled for this *call*. Otherwise, false.
     * @since 0.1
     */
    boolean isSendingDTMFEnabled();

    /**
     * Sends DTMF events to the remote party. Valid DTMF events are 0-9, *, #, a-d, and A-D.
     *
     * @param dtmf     any combination of valid DTMF events matching regex mattern "^[0-9#\*abcdABCD]+$"
     * @param callback A closure to be executed when completed, with error if the invocation is illegal or failed, otherwise nil.
     * @since 0.1
     */
    void sendDTMF(String dtmf, @NonNull CompletionHandler<Void> callback);

    /**
     * Sends feedback for this call to Cisco Webex team.
     *
     * @param rating  The rating of the quality of this call between 1 and 5 where 5 means excellent quality.
     * @param comment The comments for this call.
     * @since 0.1
     */
    void sendFeedback(int rating, @Nullable String comment);

    /**
     * @return start content sharing.
     * @since 1.4
     */
    void startSharing(@NonNull CompletionHandler<Void> callback);

    /**
     * @return stop content sharing.
     * @since 1.4
     */
    void stopSharing(@NonNull CompletionHandler<Void> callback);

    /**
     * @return True if the local party of this call is sharing content. Otherwise, false.
     * @since 1.4
     */
    boolean isSendingSharing();

    void subscribeRemoteAuxVideo(View view, @NonNull CompletionHandler<RemoteAuxVideo> callback);

    void unsubscribeRemoteAuxVideo(RemoteAuxVideo remoteAuxVideo, @NonNull CompletionHandler<Void> callback);

    int getAvailableAuxVideoCount();

    CallMembership getActiveSpeaker();
}
