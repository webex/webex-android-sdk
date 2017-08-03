package com.cisco.spark.android.whiteboard.exception;

public class DownloadAnnotationBackgroundFailedException extends LoadWhiteboardException {
    public DownloadAnnotationBackgroundFailedException(String message) {
        super(message);
    }

    public DownloadAnnotationBackgroundFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public DownloadAnnotationBackgroundFailedException(Throwable cause) {
        super(cause);
    }
}
