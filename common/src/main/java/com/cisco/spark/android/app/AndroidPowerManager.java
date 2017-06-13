package com.cisco.spark.android.app;

import android.os.PowerManager;

import com.cisco.spark.android.util.UIUtils;

public class AndroidPowerManager implements com.cisco.spark.android.app.PowerManager {

    private final PowerManager delegate;

    public AndroidPowerManager(android.os.PowerManager delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean isInteractive() {
        if (UIUtils.hasLollipop()) {
            return delegate.isInteractive();
        } else {
            return delegate.isScreenOn();
        }
    }
}
