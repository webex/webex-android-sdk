package com.cisco.spark.android.core;

import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.cisco.spark.android.util.SystemUtils;

public class SecureDevice {
    private Context context;

    public SecureDevice(Context context) {
        this.context = context;
    }

    /**
     * Will use the newest available api to determine if the user has a lock screen set
     * None or Swipe to unlock is not considered a secure lock screen, while others are:
     * - Pattern
     * - Pin of any length (4 is minimum?)
     * - Fingerprint
     * - Direction swipe
     * @return
     */
    public boolean hasLockScreen() {
        return SystemUtils.hasLockScreen(context);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public boolean isDeviceSecure() {
        return SystemUtils.isDeviceSecure(context);
    }

    public boolean isKeyguardSecure() {
        return SystemUtils.isKeyguardSecure(context);
    }

}
