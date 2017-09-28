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

import android.app.AlertDialog;
import android.support.annotation.NonNull;
import android.view.View;
import com.ciscospark.androidsdk.CompletionHandler;

/**
 * Phone represents a Cisco Spark calling device.
 * 
 * The application can obtain a phone object from Spark object and use phone to call other Cisco Spark users or PSTN when enabled.
 * 
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
	     * @since 0.1
	     */
	    USER,
	    /**
	     * Back camera.
	     * @since 0.1
	     */
	    ENVIROMENT
    }

    /**
     * Listener for incoming call
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
     * Set the listener to listen the incoming call
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
     * Registers this phone to Cisco Spark cloud on behalf of the authenticated user.
     * 
     * It also creates the websocket and connects to Cisco Spark cloud. 
     * 
     * Subsequent invocations of this method refresh the registration.
     * 
     * @param callback A closure to be executed when completed, with error if the invocation is illegal or failed, otherwise nil.
     * @since 0.1
     */
    void register(@NonNull CompletionHandler<Void> callback);

    /**
     * Removes this *phone* from Cisco Spark cloud on behalf of the authenticated user.
     * 
     * It also disconnects the websocket from Cisco Spark cloud.
     * 
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
     * @param option Intended media options - audio only or audio and video - for the call.
     * @param callback A closure to be executed when completed.
     * @since 0.1
     */
    void dial(@NonNull String dialString, @NonNull MediaOption option, @NonNull CompletionHandler<Call> callback);

    /**
     * Render a preview of the local party before the call is answered.
     * 
     * @param view a UI view for rendering video.
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
     * @param builder AlertDialog builder
     * @param callback A closure to be executed when completed. true if the user approve the license , otherwise false.
     * @since 0.1
     */
    void requestVideoCodecActivation(@NonNull AlertDialog.Builder builder, @NonNull CompletionHandler<Boolean> callback);

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
}
