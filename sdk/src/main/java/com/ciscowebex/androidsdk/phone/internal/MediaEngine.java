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

package com.ciscowebex.androidsdk.phone.internal;

import android.content.Context;
import android.view.View;
import com.ciscowebex.androidsdk.Webex;
import com.ciscowebex.androidsdk.internal.Closure;
import com.ciscowebex.androidsdk.internal.media.MediaCapability;
import com.ciscowebex.androidsdk.internal.media.WMEngine;
import com.ciscowebex.androidsdk.internal.media.WmeSession;
import com.ciscowebex.androidsdk.internal.model.MediaEngineReachabilityModel;
import com.ciscowebex.androidsdk.phone.MediaOption;
import com.ciscowebex.androidsdk.utils.Utils;

import java.util.List;
import java.util.Map;

public class MediaEngine {

    private final WMEngine engine;

    public MediaEngine(Context context, Webex.LogLevel level) {
        engine = new WMEngine(context, Utils.toTraceLevelMask(level));
    }

    public void release() {
        engine.release();
    }

    public MediaSession createSession(MediaCapability capability, MediaOption option) {
        WmeSession session = engine.createSession(capability, option);
        return new MediaSession(session, false);
    }

    public MediaSession createPreviewSession(MediaCapability capability, View view) {
        WmeSession session = engine.createPreviewSession(capability, view);
        return new MediaSession(session, true);
    }

    public void setLoggingLevel(Webex.LogLevel level) {
        engine.setLoggingLevel(Utils.toTraceLevelMask(level));
    }

    public void setDisplayRotation(int rotation) {
        engine.setDisplayRotation(rotation);
    }

    public String getVersion() {
        return engine.getVersion();
    }

    public void performReachabilityCheck(Map<String, Map<String, List<String>>> clusterInfo, Closure<MediaEngineReachabilityModel> callback) {
        engine.performStunReachabilityCheck(clusterInfo, callback::invoke);
    }

    public void clearReachability() {
        engine.clearReachabilityData();
    }


}
