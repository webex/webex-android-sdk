package com.cisco.spark.android.features;

public interface CoreFeatures {

    // Whiteboard Features
    boolean isWhiteboardEnabled();
    boolean isSendWhiteboardEnabled();
    boolean isDeleteWhiteboardEnabled();
    boolean isNativeWhiteboardEnabled();
    boolean isWhiteBoardAddGuestAclEnabled();
    boolean isAnnotationEnabled();

    // Media/call control features
    boolean isNumericDialingEnabled();
    boolean isRoapEnabled();
    boolean uploadCallLogs();
    int getMaxRosterSize();
    int getMaxBridgeSize();
    boolean isDeltaEventEnabled();

    // Metrics
    boolean isSegmentMetricsEnabled();

    // TODO Not SDK
    boolean isCallSpinnerEnabled();
}
