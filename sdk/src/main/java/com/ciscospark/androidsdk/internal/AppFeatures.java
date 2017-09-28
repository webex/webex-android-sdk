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

package com.ciscospark.androidsdk.internal;

import com.cisco.spark.android.features.CoreFeatures;
import com.cisco.spark.android.wdm.DeviceRegistration;

import static com.cisco.spark.android.wdm.Features.*;

public class AppFeatures implements CoreFeatures {

    private final DeviceRegistration deviceRegistration;

    public AppFeatures(DeviceRegistration deviceRegistration) {
        this.deviceRegistration = deviceRegistration;
    }

    /**************
     * APP FEATURES
     *************/

    public boolean isFilesActivityEnabled() {
        return deviceRegistration.getFeatures().isDeveloperFeatureEnabled(ANDROID_FILES_SPACEBALL, false);
    }

    public boolean isAnnotationFileEnable() {
        return deviceRegistration.getFeatures().isDeveloperFeatureEnabled(ANDROID_BOARD_ANNOTATION_FILE, false) && isWhiteboardEnabled();
    }

    public boolean isAnnotationPresentationEnable() {
        return deviceRegistration.getFeatures().isDeveloperFeatureEnabled(ANDROID_BOARD_ANNOTATION_PRESENTATION, false) && isWhiteboardEnabled();
    }


    /**************
     * SDK FEATURES
     **************/

    @Override
    public boolean isWhiteboardEnabled()  {
        return true;
    }

    @Override
    public boolean isNativeWhiteboardEnabled()   {
        return true;
    }

    public boolean isLockWhiteboardToLandscapeEnabled() {
        return false;
    }

    @Override
    public boolean isSendWhiteboardEnabled() {
        return deviceRegistration.getFeatures().isAnyToggleEnabled(ANDROID_SEND_WHITEBOARD_EARLYADOPTERS,
                                                                   ANDROID_SEND_WHITEBOARD);
    }

    @Override
    public boolean isDeleteWhiteboardEnabled() {
        return deviceRegistration.getFeatures().isAnyToggleEnabled(ANDROID_DELETE_WHITEBOARD);
    }

    @Override
    public boolean isWhiteBoardAddGuestAclEnabled() {
        return deviceRegistration.getFeatures().isDeveloperFeatureEnabled(ANDROID_WHITEBOARD_ADD_GUEST_ACL, false);
    }

    @Override
    public boolean isAnnotationEnabled() {
        return false;
    }

    // CallImpl / media

    public boolean isRoapEnabled() {
        return deviceRegistration.getFeatures().isDeveloperFeatureEnabled(ROAP_ENABLED, false);
    }

    public int getMaxRosterSize() {
        final int defaultMaxRosterSize = getMaxBridgeSize();
        return deviceRegistration.getFeatures().getFeatureInt(MAX_ROSTER_SIZE, FeatureType.DEVELOPER, defaultMaxRosterSize);
    }

    public int getMaxBridgeSize() {
        final int defaultMaxBridgeSize = 12;
        return deviceRegistration.getFeatures().getFeatureInt(MAX_BRIDGE_SIZE, FeatureType.DEVELOPER, defaultMaxBridgeSize);
    }

    public int getMeetUrlMaxConvParticipants() {
        final int defaultMeetUrlMaxConvParticipants = getMaxBridgeSize();
        return deviceRegistration.getFeatures().getFeatureInt(MEET_MAX_CONV_PARTICIPATNS, FeatureType.DEVELOPER, defaultMeetUrlMaxConvParticipants);
    }

    public boolean uploadCallLogs() {
        return deviceRegistration.getFeatures().isDeveloperFeatureEnabled(UPLOAD_CALL_LOGS, false);
    }

    public boolean isNumericDialingEnabled() {
        return deviceRegistration.getFeatures().isFeatureEnabled(NUMERIC_DIALING_ENABLED, FeatureType.DEVELOPER, false);
    }

    public boolean isCallSpinnerEnabled() {
        return deviceRegistration.getFeatures().isDeveloperFeatureEnabled(ANDROID_CALL_SPINNER, false);
    }

    @Override
    public boolean isDeltaEventEnabled() {
        return deviceRegistration.getFeatures().isDeveloperFeatureEnabled(ANDROID_LOCUS_DELTA_EVENT, false);
    }


    // Metrics

    @Override
    public boolean isSegmentMetricsEnabled() {
        return true;
    }

}
