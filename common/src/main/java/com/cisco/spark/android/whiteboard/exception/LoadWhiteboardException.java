package com.cisco.spark.android.whiteboard.exception;

public class LoadWhiteboardException extends RuntimeException {
    public LoadWhiteboardException(String message) {
        super(message);
    }

    public LoadWhiteboardException(String message, Throwable cause) {
        super(message, cause);
    }

    public LoadWhiteboardException(Throwable cause) {
        super(cause);
    }
}
