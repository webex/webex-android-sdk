package com.ciscowebex.androidsdk.internal.queue;

public interface NamedRunnable extends Runnable {

    enum Name {
        FireCallOnConnected
    }

    Name getName();
}
