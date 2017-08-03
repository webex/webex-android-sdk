package com.cisco.spark.android.metrics;


import com.cisco.spark.android.metrics.value.LoadWhiteboardValue;
import com.cisco.spark.android.metrics.value.SaveWhiteboardValue;
import com.cisco.spark.android.metrics.value.WhiteboardDeleteValue;

public class WhiteboardServiceMetricsBuilder extends SplunkMetricsBuilder {
    private static final String LOAD_WHITEBOARD_TAG = "loadWhiteboardService";
    private static final String SAVE_WHITEBOARD_TAG = "saveWhiteboardService";
    public static final String ANDROID_WHITEBOARD_DELETE = "android_whiteboard_delete";

    public WhiteboardServiceMetricsBuilder(MetricsEnvironment environment) {
        super(environment);
    }

    public MetricsBuilder reportWhiteboardLoaded(long networkloadtime, long decryptLoadTime, int size, String whiteboardID) {
        reportValue(LOAD_WHITEBOARD_TAG, new LoadWhiteboardValue(networkloadtime, decryptLoadTime, size, whiteboardID));
        return this;
    }

    public MetricsBuilder reportWhiteboardSaved(long savetime, String whiteboardID) {
        reportValue(SAVE_WHITEBOARD_TAG, new SaveWhiteboardValue(savetime, whiteboardID));
        return this;
    }

    public MetricsBuilder reportWhiteboardDelete(String errorType, boolean isSuccessful, int httpStatusCode, String errorMsg) {
        reportValue(ANDROID_WHITEBOARD_DELETE, new WhiteboardDeleteValue(errorType, isSuccessful, httpStatusCode, errorMsg));
        return this;
    }
}
