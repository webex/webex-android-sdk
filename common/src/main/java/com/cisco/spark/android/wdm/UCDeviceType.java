package com.cisco.spark.android.wdm;

/**
 * The deviceType for UC devices is "UC".
 * In integration tests, we're using "TEST" instead because "UC devices don't expose a web socket url.
 * {@link UCDeviceType} helps us use a different value in the app than in tests.
 */
public class UCDeviceType {
    private final String value;

    public UCDeviceType(String value) {
        this.value = value;
    }

    public String get() {
        return value;
    }
}
