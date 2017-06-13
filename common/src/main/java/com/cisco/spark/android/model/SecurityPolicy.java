package com.cisco.spark.android.model;

import com.cisco.spark.android.core.SecureDevice;
import com.cisco.spark.android.wdm.DeviceRegistration;

import javax.inject.Inject;

public class SecurityPolicy {
    public static final String ON = "on";
    public static final String OFF = "off";
    public static final int MAX_NUM_ALLOWED_WARNINGS = 2;

    @Inject
    SecureDevice secureDevice;

    @Inject
    DeviceRegistration deviceRegistration;

    public boolean isClientSecurityPolicyEnabled() {
        return ON.equals(deviceRegistration.getClientSecurityPolicy());
    }

    public boolean shouldEnforceSecurityPolicy() {
        return isClientSecurityPolicyEnabled() && !secureDevice.hasLockScreen() && !deviceRegistration.getFeatures().hasDisabledPinEnforcement();
    }
}
