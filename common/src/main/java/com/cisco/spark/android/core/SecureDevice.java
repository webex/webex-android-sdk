package com.cisco.spark.android.core;

import android.content.Context;

import com.cisco.spark.android.util.SystemUtils;

public class SecureDevice {
    private Context context;

    public SecureDevice(Context context) {
        this.context = context;
    }

    public boolean hasLockScreen() {
        return SystemUtils.hasLockScreen(context);
    }
}
