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

import android.media.ExifInterface;

import com.github.benoitdion.ln.Ln;

import java.io.File;
import java.io.IOException;


public class Metadata {

    private static class ExifTag {
        private String id;
        private boolean isInteresting;

        public ExifTag(String id, boolean isInteresting) {
            this.id = id;
            this.isInteresting = isInteresting;
        }

        public String getId() {
            return id;
        }

        public boolean isInteresting() {
            return isInteresting;
        }
    }

    static ExifTag[] tags = {
            // Metadata that we're interested in changing (removing)
            new ExifTag(ExifInterface.TAG_DATETIME, true),
            new ExifTag(ExifInterface.TAG_GPS_ALTITUDE, true),
            new ExifTag(ExifInterface.TAG_GPS_ALTITUDE_REF, true),
            new ExifTag(ExifInterface.TAG_GPS_DATESTAMP, true),
            new ExifTag(ExifInterface.TAG_GPS_LATITUDE, true),
            new ExifTag(ExifInterface.TAG_GPS_LATITUDE_REF, true),
            new ExifTag(ExifInterface.TAG_GPS_LONGITUDE, true),
            new ExifTag(ExifInterface.TAG_GPS_LONGITUDE_REF, true),
            new ExifTag(ExifInterface.TAG_GPS_PROCESSING_METHOD, true),
            new ExifTag(ExifInterface.TAG_GPS_TIMESTAMP, true),
            // All the other metadata
            new ExifTag(ExifInterface.TAG_APERTURE, false),
            new ExifTag(ExifInterface.TAG_EXPOSURE_TIME, false),
            new ExifTag(ExifInterface.TAG_FLASH, false),
            new ExifTag(ExifInterface.TAG_FOCAL_LENGTH, false),
            new ExifTag(ExifInterface.TAG_IMAGE_LENGTH, false),
            new ExifTag(ExifInterface.TAG_IMAGE_WIDTH, false),
            new ExifTag(ExifInterface.TAG_ISO, false),
            new ExifTag(ExifInterface.TAG_MAKE, false),
            new ExifTag(ExifInterface.TAG_MODEL, false),
            new ExifTag(ExifInterface.TAG_ORIENTATION, false),
            new ExifTag(ExifInterface.TAG_WHITE_BALANCE,  false)
    };


    private ExifInterface exif;

    public Metadata(File file) {
        if (file != null) {
            try {
                exif = new ExifInterface(file.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    // Dump only tags we're interested in
    public void dump() {
        dumpTags(true);
    }

    public void dumpAll() {
        dumpTags(false);
    }

    private void dumpTags(boolean onlyInteresting) {
        if (exif == null) {
            Ln.w(false, "dumpTags: ExifInterface is null");
            return;
        }

        for (ExifTag tag : tags) {
            if (onlyInteresting && !tag.isInteresting())
                continue;
            Ln.d("tag: '%s', value: '%s'", tag.getId(), exif.getAttribute(tag.getId()));
        }
    }


    // Sanitize only tags we're interested in
    public void sanitize() {
        sanitizeTags(true);
    }

    public void sanitizeAll() {
        sanitizeTags(false);
    }

    private void sanitizeTags(boolean onlyInteresting) {
        if (exif == null) {
            Ln.w(false, "sanitizeTags: ExifInterface is null");
            return;
        }

        for (ExifTag tag : tags) {
            if (onlyInteresting && !tag.isInteresting()) {
                continue;
            }
            exif.setAttribute(tag.getId(), null);
        }
    }
}
