package com.cisco.spark.android.util;

public class DiagnosticModeChangedEvent {

    private final String pin, verboseLoggingToken;

    public DiagnosticModeChangedEvent(String pin, String verboseLoggingToken) {
        this.pin = pin;
        this.verboseLoggingToken = verboseLoggingToken;
    }

    public String getPin() {
        return pin;
    }

    public String getVerboseLoggingToken() {
        return verboseLoggingToken;
    }
}
