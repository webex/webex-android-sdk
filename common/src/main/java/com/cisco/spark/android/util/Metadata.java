package com.cisco.spark.android.util;

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

    public Metadata(File file) throws IOException {
        if (file != null)
            exif = new ExifInterface(file.getAbsolutePath());
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
            if (onlyInteresting && !tag.isInteresting())
                continue;
            exif.setAttribute(tag.getId(), null);
        }
    }
}
