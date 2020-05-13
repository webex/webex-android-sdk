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

package com.ciscowebex.androidsdk.utils;

import com.ciscowebex.androidsdk.BuildConfig;
import com.github.benoitdion.ln.Ln;

import java.util.Locale;
import java.util.UUID;

public class TrackingIdGenerator {

    private final String base;
    private int counter;
    private String currentTrackingId;

    public TrackingIdGenerator() {
        base = UUID.randomUUID().toString();
        currentTrackingId = "";
    }

    public synchronized String nextTrackingId() {
        String trackingId = String.format(Locale.US, "%s_%s_%d", "webex-android-sdk", base, counter);
        if (BuildConfig.INTEGRATION_TEST) {
            trackingId = "IT" + trackingId;
        }
        counter++;
        if (counter > 65536) {
            counter = 0;
        }
        currentTrackingId = trackingId;
        Ln.report(null, "Tracking-Id: " + currentTrackingId);
        return trackingId;
    }

    public String currentTrackingId() {
        return currentTrackingId;
    }
}
