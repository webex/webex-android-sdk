package com.cisco.spark.android.log;

import android.text.TextUtils;

import com.cisco.spark.android.locus.model.LocusData;
import com.cisco.spark.android.util.DateUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class LogCallIndexEntry {
    private static final String TYPE_NAME_TAG = "Call";
    private static final String LOCUS_URI_TAG = "Locus Uri";
    private static final String LAST_ACTIVE_TAG = "Last Active";
    private static final String TRACKING_ID_TAG = "Tracking ID";
    private static final String UNKNOWN_VALUE = "unknown";

    private String entryTimestamp;
    private String type;
    private String locusUri;
    private String locusId;
    private String lastActive;
    private String trackingId;


    public LogCallIndexEntry(String type, LocusData locusData, String trackingId) {
        initialize();
        this.type = type;
        if (locusData != null) {
            this.locusUri = locusData.getKey().toString();
            this.locusId = getIdFromUri(locusUri);
            if (locusData.getLocus() != null && locusData.getLocus().getFullState() != null && locusData.getLocus().getFullState().getLastActive() != null) {
                this.lastActive = DateUtils.buildIso8601Format().format(locusData.getLocus().getFullState().getLastActive());
            }
        }
        if (!TextUtils.isEmpty(trackingId))
            this.trackingId = trackingId;
    }

    public LogCallIndexEntry(String indexFileEntry) {
        initialize();
        if (!TextUtils.isEmpty(indexFileEntry)) {
            String[] values = indexFileEntry.split(",");
            for (String value : values) {
                if (value.contains(TYPE_NAME_TAG)) {
                    type = getValue(value);
                } else if (value.contains(LOCUS_URI_TAG)) {
                    locusUri = getValue(value);
                    locusId = getIdFromUri(locusUri);
                } else if (value.contains(LAST_ACTIVE_TAG)) {
                    lastActive = getValue(value);
                } else if (value.contains(TRACKING_ID_TAG)) {
                    trackingId = getValue(value);
                }
            }
        }
    }

    public String getIndexFileEntry() {
        return String.format("%s %s=%s, %s=%s, %s=%s, %s=%s", entryTimestamp,
                TYPE_NAME_TAG, type,
                LOCUS_URI_TAG, locusUri,
                LAST_ACTIVE_TAG, lastActive,
                TRACKING_ID_TAG, trackingId);
    }

    public String getType() {
        return type;
    }

    public String getLocusUri() {
        return locusUri;
    }

    public String getLocusId() {
        return locusId;
    }

    public String getLastActive() {
        return lastActive;
    }

    public String getTrackingId() {
        return trackingId;
    }


    private void initialize() {
        entryTimestamp = type = locusUri = locusId = lastActive = trackingId = UNKNOWN_VALUE;
        SimpleDateFormat formatter = new SimpleDateFormat("MM-dd HH:mm:ss.SSS");
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        if (calendar.getTime() != null) {
            entryTimestamp = formatter.format(calendar.getTime());
        }
    }

    private String getValue(String val) {
        if (val != null) {
            String[] nvPair = val.split("=");
            if (nvPair.length == 2)
                return nvPair[1];
        }
        return UNKNOWN_VALUE;
    }

    private String getIdFromUri(String locus) {
        if (locus != null) {
            String[] locusElements = locus.split("/");
            if (locusElements.length > 1) {
                return locusElements[locusElements.length - 1];
            }
        }
        return UNKNOWN_VALUE;
    }
}
