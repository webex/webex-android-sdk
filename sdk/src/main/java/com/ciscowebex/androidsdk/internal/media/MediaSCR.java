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

package com.ciscowebex.androidsdk.internal.media;

public class MediaSCR {

    public static final MediaSCR p90 = new MediaSCR(60, 3000, 177*1000, 180, 1800, 254, 0, false);
    public static final MediaSCR p180 = new MediaSCR(240, 3000, 384*1000, 720, 7200, 255, 0, false);
    public static final MediaSCR p360 = new MediaSCR(920, 3000, 768*1000, 2760, 27600, 255, 0, false);
    public static final MediaSCR p720 = new MediaSCR(3600, 3000, 2000*1000, 11520, 108000, 255, 0, false);
    public static final MediaSCR p1080 = new MediaSCR(8160, 3000, 4000*1000, 24300, 245760, 255, 0, false);

    public int maxFs;
    public int maxFps;
    public int maxBr;
    public int maxDpb;
    public int maxMbps;
    public int priority;
    public int grouping;
    public boolean duplicate;

    private MediaSCR(int fs, int fps, int br, int dpb, int mbps, int priority, int grouping, boolean duplicate) {
        this.maxFs = fs;
        this.maxFps = fps;
        this.maxBr = br;
        this.maxDpb = dpb;
        this.maxMbps = mbps;
        this.priority = priority;
        this.grouping = grouping;
        this.duplicate = duplicate;
    }

}
