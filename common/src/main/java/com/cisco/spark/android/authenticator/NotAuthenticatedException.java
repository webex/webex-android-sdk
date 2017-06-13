package com.cisco.spark.android.authenticator;

public class NotAuthenticatedException extends RuntimeException {
    public NotAuthenticatedException(String detailMessage) {
        super(detailMessage);
    }

    public NotAuthenticatedException() {
    }
}
