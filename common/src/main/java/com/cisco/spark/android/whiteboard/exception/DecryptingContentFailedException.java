package com.cisco.spark.android.whiteboard.exception;

public class DecryptingContentFailedException extends LoadWhiteboardException {
    public DecryptingContentFailedException(String message) {
        super(message);
    }

    public DecryptingContentFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public DecryptingContentFailedException(Throwable cause) {
        super(cause);
    }
}
