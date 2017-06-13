package com.cisco.spark.android.util;

public interface Clock {

    //Time in milliseconds since January 1, 1970 00:00:00.0 UTC
    //Not Monotonic
    long now();

    //Time in milliseconds (only to be used relative to other monotonicNow calls)
    //Monotonic
    long monotonicNow();
}
