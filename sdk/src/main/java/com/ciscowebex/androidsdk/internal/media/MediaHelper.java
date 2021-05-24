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

package com.ciscowebex.androidsdk.internal.media;

import android.view.View;
import com.ciscowebex.androidsdk.utils.Json;
import com.github.benoitdion.ln.Ln;
import com.webex.wme.MediaTrack;

import java.util.Arrays;

public class MediaHelper {

    private MediaHelper() {
    }

    public static String getVideoStreamString(int vid) {
        return isActiveSpeaker(vid) ? "ActiveSpeaker" : "Other";
    }

    public static boolean isActiveSpeaker(int videoId) {
        return videoId == WMEngine.MAIN_VID;
    }

    public static void requestSCR(MediaTrack track, MediaSCR p) {
        Ln.d("Request " + Json.get().toJson(p) + "for " + track);
        track.RequestVideo(p.maxFs, p.maxFps, p.maxBr, p.maxDpb, p.maxMbps, p.priority, p.grouping, p.duplicate);
    }

    public static MediaTrack.ScalingMode getRenderMode(MediaTrack track, View view) {
        if (view == null || track == null) {
            return MediaTrack.ScalingMode.CropFill;
        }
        long decodedHeight = track.getVideoTrackStatistics().uHeight;
        long decodedWidth = track.getVideoTrackStatistics().uWidth;
        boolean receivingPortrait = decodedHeight > decodedWidth;
        long containerWidth = view.getWidth();
        long containerHeight = view.getHeight();
        boolean containerPortrait = containerHeight > containerWidth;
        if ((containerPortrait && receivingPortrait) || (!containerPortrait && !receivingPortrait)) {
            return MediaTrack.ScalingMode.CropFill;
        }
        return MediaTrack.ScalingMode.LetterBox;

    }

}
