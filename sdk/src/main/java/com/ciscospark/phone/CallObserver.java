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

package com.ciscospark.phone;



import com.ciscospark.common.SparkError;

import java.util.List;

/**
 * Created on 12/06/2017.
 */

public interface CallObserver {

    /**
     * this function will be called while remoted part is ringing
     * @param call  the call to which this event belonged.
     * @return none
     */
    void onRinging(Call call);

    /**
     * this function will be called while a call is connected
     * @param call  the call to which this event belonged.
     * @return none
     */
    void onConnected(Call call);


    /**
     * this function will be called while a call is disconnected
     * @param call  the call to which this event belonged.
     * @param reason  reason of disconnection
     * @param errorInfo, if the reason is a error type, errorInfo will contain supplement
     *                   information
     *                   if the reason is not a error, errorInfo will be null.
     * @return none
     */
    void onDisconnected(Call call, DisconnectedReason reason, SparkError errorInfo);


    /**
     * this function will be called while a call is disconnected
     * @param call  the call to which this event belonged.
     * @param reason  what change.
     * @return none
     */
    void onMediaChanged(Call call, MediaChangeReason reason);




    enum DisconnectedReason {

        /**
         * call end as user need to grant permissions.
         */
        endForAndroidPermission,
        /**
         * call end as user hang up
         */
        selfHangUP,
        /**
         * call end as remoted part hang up
         */
        remoteHangUP,
        /**
         * call end as remoted part reject
         */
        remoteReject,
        /**
         * call end as sever end the call
         */
        callEnd,
        /**
         * call end as get error
         */
        Error_serviceFailed_CallJoinError

    }

    enum MediaChangeReason {
        DTMF
    }
}
