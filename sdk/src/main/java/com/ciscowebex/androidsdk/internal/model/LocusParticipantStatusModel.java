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

package com.ciscowebex.androidsdk.internal.model;

import java.util.List;

public class LocusParticipantStatusModel {

    public final LocusMediaDirection audioStatus;
    public final LocusMediaDirection videoStatus;
    public final LocusMediaDirection videoSlidesStatus;
    public final List<Long> csis;

    public LocusParticipantStatusModel(LocusMediaDirection audioStatus, LocusMediaDirection videoStatus, LocusMediaDirection videoSlidesStatus, List<Long> csis) {
        this.audioStatus = audioStatus;
        this.videoStatus = videoStatus;
        this.videoSlidesStatus = videoSlidesStatus;
        this.csis = csis;
    }

    public LocusMediaDirection getAudioStatus() {
        return audioStatus;
    }

    public LocusMediaDirection getVideoStatus() {
        return videoStatus;
    }

    public LocusMediaDirection getVideoSlidesStatus() {
        return videoSlidesStatus;
    }

    public List<Long> getCsis() {
        return csis;
    }
}
