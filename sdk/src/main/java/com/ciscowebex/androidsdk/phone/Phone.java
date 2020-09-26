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

package com.ciscowebex.androidsdk.phone;

import android.app.AlertDialog;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import com.ciscowebex.androidsdk.CompletionHandler;

import java.util.List;

/**
 * Phone represents a Cisco Webex calling device.
 * <p>
 * The application can obtain a phone object from Webex object and use phone to call other Cisco Webex users or PSTN when enabled.
 * <p>
 * The phone must be registered before it can make or receive calls.
 *
 * @since 0.1
 */
public interface Phone {

    /**
     * The enumeration of Camera facing modes.
     *
     * @since 0.1
     */
    enum FacingMode {
        /**
         * Front camera.
         *
         * @since 0.1
         */
        USER,
        /**
         * Back camera.
         *
         * @since 0.1
         */
        ENVIROMENT
    }

    /**
     * The enumeration of common bandwidth choices.
     *
     * @since 1.3.0
     */
    enum DefaultBandwidth {
        /**
         * 177Kbps for 160x90 resolution
         *
         * @since 1.3.0
         */
        MAX_BANDWIDTH_90P(177000),
        /**
         * 384Kbps for 320x180 resolution
         *
         * @since 1.3.0
         */
        MAX_BANDWIDTH_180P(384000),
        /**
         * 768Kbps for 640x360 resolution
         *
         * @since 1.3.0
         */
        MAX_BANDWIDTH_360P(768000),
        /**
         * 2.5Mbps for 1280x720 resolution
         *
         * @since 1.3.0
         */
        MAX_BANDWIDTH_720P(2500000),
        /**
         * 4Mbps for 1920x1080 resolution
         *
         * @since 1.3.0
         */
        MAX_BANDWIDTH_1080P(4000000),
        /**
         * 8Mbps data session
         *
         * @since 1.3.0
         */
        MAX_BANDWIDTH_SESSION(8000000),
        /**
         * 64kbps for voice
         *
         * @since 1.3.0
         */
        MAX_BANDWIDTH_AUDIO(64000);

        private final int id;

        DefaultBandwidth(int id) {
            this.id = id;
        }

        public int getValue() {
            return id;
        }
    }

    /**
     * The options for H.264 video codec license from Cisco Systems, Inc
     *
     * @since 2.6.0
     */
    enum H264LicenseAction {
        /**
         * Indicates that the end user has accepted the term.
         */
        ACCEPT,
        /**
         * Indicates that the end user declined the term.
         */
        DECLINE,
        /**
         * Indicates that the end user wants to view the license.
         */
        VIEW_LICENSE
    }

    /**
     * The interface for a listener for incoming call
     *
     * @since 0.1
     */
    interface IncomingCallListener {
        /**
         * Callback when call is incoming.
         *
         * @param call incoming call
         */
        void onIncomingCall(Call call);
    }

    /**
     * The listener for incoming call
     *
     * @since 0.1
     */
    IncomingCallListener getIncomingCallListener();

    /**
     * Set the listener to listen to the incoming call to this Phone.
     *
     * @since 0.1
     */
    void setIncomingCallListener(IncomingCallListener listener);

    /**
     * Default camera facing mode of this phone, used as the default when dialing or answering a call. The default mode is the front camera.
     *
     * @since 0.1
     */
    FacingMode getDefaultFacingMode();

    /**
     * Set default camera facing mode of this phone, used as the default when dialing or answering a call. The setting is not persistent.
     *
     * @since 0.1
     */
    void setDefaultFacingMode(FacingMode mode);

    /**
     * Registers this phone to Cisco Webex cloud on behalf of the authenticated user.
     * <p>
     * It also creates the websocket and connects to Cisco Webex cloud.
     * <p>
     * Subsequent invocations of this method refresh the registration.
     *
     * @param callback A closure to be executed when completed, with error if the invocation is illegal or failed, otherwise nil.
     * @since 0.1
     */
    void register(@NonNull CompletionHandler<Void> callback);

    /**
     * Removes this *phone* from Cisco Webex cloud on behalf of the authenticated user.
     * <p>
     * It also disconnects the websocket from Cisco Webex cloud.
     * <p>
     * Subsequent invocations of this method behave as a no-op.
     *
     * @param callback A closure to be executed when completed, with error if the invocation is illegal or failed, otherwise nil.
     * @since 0.1
     */
    void deregister(@NonNull CompletionHandler<Void> callback);

    /**
     * Makes a call to an intended recipient on behalf of the authenticated user.
     *
     * @param dialString Intended recipient address in one of the supported formats.
     * @param option     Intended media options - audio only or audio and video - for the call.
     * @param callback   A closure to be executed when completed.
     * @since 0.1
     */
    void dial(@NonNull String dialString, @NonNull MediaOption option, @NonNull CompletionHandler<Call> callback);

    /**
     * Render a preview of the local party before the call is answered.
     *
     * @param view an UI view for rendering video.
     * @since 0.1
     */
    void startPreview(View view);

    /**
     * Stop rendering the preview of the local party.
     *
     * @since 0.1
     */
    void stopPreview();

    /**
     * Pops up an Alert for the end user to approve the use of H.264 codec license from Cisco Systems, Inc.
     *
     * @param builder  AlertDialog builder
     * @param callback A closure to be executed when completed.
     * @since 0.1
     */
    void requestVideoCodecActivation(@NonNull AlertDialog.Builder builder, @Nullable CompletionHandler<H264LicenseAction> callback);

    /**
     * Prevents the SDK from poping up an Alert for the end user to approve the use of H.264 video codec license from Cisco Systems, Inc.
     *
     * @since 0.1
     */
    void disableVideoCodecActivation();

    /**
     * Return the text of the H.264 codec license from Cisco Systems, Inc.
     *
     * @since 0.1
     */
    String getVideoCodecLicense();

    /**
     * Return the URL of the H.264 codec license from Cisco Systems, Inc.
     *
     * @since 0.1
     */
    String getVideoCodecLicenseURL();

    /**
     * Set the max receivning bandwidth for audio in unit bps for the call.
     * Only effective if set before the start of call.
     * if 0, default value of 64 * 1000 is used.
     *
     * @param bandwidth the suggest value could be {@link DefaultBandwidth#MAX_BANDWIDTH_AUDIO}.
     * @since 2.6.0
     */
    void setAudioMaxRxBandwidth(int bandwidth);

    /**
     * Return the current maximum receivning bandwidth of audio stream.
     *
     * @since 2.6.0
     */
    int getAudioMaxRxBandwidth();

    /**
     * Set the max receiving bandwidth for video in unit bps for the call.
     * Only effective if set before the start of call.
     * if 0, default value of 2000*1000 is used.
     *
     * @param bandwidth the suggest value could be {@link DefaultBandwidth#MAX_BANDWIDTH_90P}, {@link DefaultBandwidth#MAX_BANDWIDTH_180P},
     *                  {@link DefaultBandwidth#MAX_BANDWIDTH_360P}, {@link DefaultBandwidth#MAX_BANDWIDTH_720P}, or {@link DefaultBandwidth#MAX_BANDWIDTH_1080P}.
     * @since 2.6.0
     */
    void setVideoMaxRxBandwidth(int bandwidth);

    /**
     * Return the current maximum receiving bandwidth of video stream.
     *
     * @since 2.6.0
     */
    int getVideoMaxRxBandwidth();

    /**
     * Set the max sending bandwidth for video in unit bps for the call.
     * Only effective if set before the start of call.
     * if 0, default value of 2000*1000 is used.
     *
     * @param bandwidth the suggest value could be {@link DefaultBandwidth#MAX_BANDWIDTH_90P}, {@link DefaultBandwidth#MAX_BANDWIDTH_180P},
     *                  {@link DefaultBandwidth#MAX_BANDWIDTH_360P}, {@link DefaultBandwidth#MAX_BANDWIDTH_720P}, or {@link DefaultBandwidth#MAX_BANDWIDTH_1080P}.
     * @since 2.6.0
     */
    void setVideoMaxTxBandwidth(int bandwidth);

    /**
     * Return the current maximum sending bandwidth of video stream.
     *
     * @since 2.6.0
     */
    int getVideoMaxTxBandwidth();

    /**
     * Set the max receiving bandwidth for content sharing in unit bps for the call.
     * Only effective if set before the start of call.
     * if 0, default value of 4000*1000 is used.
     *
     * @param bandwidth the suggest value could be {@link DefaultBandwidth#MAX_BANDWIDTH_SESSION}.
     * @since 2.6.0
     */
    void setSharingMaxRxBandwidth(int bandwidth);

    /**
     * Return the current maximum receiving bandwidth of content sharing stream.
     *
     * @since 2.6.0
     */
    int getSharingMaxRxBandwidth();

    /**
     * Return True if Google hardware media codec is turned on for video. Otherwise, false.
     *
     * @since 2.1.1
     */
    boolean isHardwareAccelerationEnabled();

    /**
     * Turn on/off Google hardware media codec for video. By doing hardware codec,
     * android devices can do high definition (720p@30fps) video calls, but please note,
     * since the hardware codec is high depend on android devices, some devices support hardware codec not very well.
     *
     * @param enable True to turn on Google hardware media codec. Otherwise, use OpenH264 software codec.
     * @since 2.1.1
     */
    void setHardwareAccelerationEnabled(boolean enable);

    /**
     * Turn on audio enhancement for the specified device models. By default,
     * the audio enhancement is enable for Samsung S7/S7Edge/S8/S8+/S9/S9+/S10/S10+/S10e/Note8/Note9.
     *
     * @param models the list of device models, if the list is null or empty, turn off the audio enhancement.
     * @since 2.3.0
     */
    void enableAudioEnhancementForModels(List<String> models);

    /**
     * Set the max bandwidth for audio in unit bps for the call.
     * Only effective if set before the start of call.
     * if 0, default value of 64 * 1000 is used.
     *
     * @param bandwidth the suggest value could be {@link DefaultBandwidth#MAX_BANDWIDTH_AUDIO}.
     * @since 1.3.0
     * @deprecated
     */
    @Deprecated
    void setAudioMaxBandwidth(int bandwidth);

    /**
     * Return the current maximum bandwidth of audio stream.
     *
     * @since 1.3.0
     * @deprecated
     */
    @Deprecated
    int getAudioMaxBandwidth();

    /**
     * Set the max bandwidth for video in unit bps for the call.
     * Only effective if set before the start of call.
     * if 0, default value of 2000*1000 is used.
     *
     * @param bandwidth the suggest value could be {@link DefaultBandwidth#MAX_BANDWIDTH_90P}, {@link DefaultBandwidth#MAX_BANDWIDTH_180P},
     *                  {@link DefaultBandwidth#MAX_BANDWIDTH_360P}, {@link DefaultBandwidth#MAX_BANDWIDTH_720P}, or {@link DefaultBandwidth#MAX_BANDWIDTH_1080P}.
     * @since 1.3.0
     * @deprecated
     */
    @Deprecated
    void setVideoMaxBandwidth(int bandwidth);

    /**
     * Return the current maximum bandwidth of video stream.
     *
     * @since 1.3.0
     * @deprecated
     */
    @Deprecated
    int getVideoMaxBandwidth();

    /**
     * Set the max bandwidth for content sharing in unit bps for the call.
     * Only effective if set before the start of call.
     * if 0, default value of 4000*1000 is used.
     *
     * @param bandwidth the suggest value could be {@link DefaultBandwidth#MAX_BANDWIDTH_SESSION}.
     * @since 1.3.0
     * @deprecated
     */
    @Deprecated
    void setSharingMaxBandwidth(int bandwidth);

    /**
     * Return the current maximum bandwidth of content sharing stream.
     *
     * @since 1.3.0
     * @deprecated
     */
    @Deprecated
    int getSharingMaxBandwidth();

    /**
     * Set true to keep video stream(include local and remote) when minimize app, else will stop. Default is false.
     * Google recommend release camera when app goes background and resume after app foreground.
     * When you use picture-in-picture mode in app should set this method to true before call.
     *
     * @param enable true to keep video stream when minimize app, else will stop. Default is false.
     * @since 2.6.0
     */
    void enableBackgroundStream(boolean enable);

    /**
     * Set advanced setings for call. Only effective if set before the start of call.
     *
     * For example, Phone.setAdvancedSetting(new VideoMaxTxFPS(30));
     *
     * @see AdvancedSetting
     * @since 2.6.0
     */
    void setAdvancedSetting(AdvancedSetting setting);

    /**
     * Returns value of the advanced seting that has been set.
     *
     * For example, AdvancedSetting setting = Phone.getAdvancedSetting(VideoMaxTxFPS.class);
     *
     * @see AdvancedSetting
     * @since 2.6.0
     */
    AdvancedSetting getAdvancedSetting(Class<? extends AdvancedSetting> clz);

    /**
     * Cancel the currently outgoing call that has not been connected.
     *
     * @since 2.6.0
     */
    void cancel();
}
