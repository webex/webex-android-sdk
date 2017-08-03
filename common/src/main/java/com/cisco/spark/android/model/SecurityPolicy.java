package com.cisco.spark.android.model;

import android.annotation.TargetApi;
import android.os.Build;

import com.cisco.spark.android.core.SecureDevice;
import com.cisco.spark.android.core.Settings;
import com.cisco.spark.android.util.UIUtils;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.github.benoitdion.ln.Ln;

import javax.inject.Inject;

public class SecurityPolicy {

    public static final String ON = "on";
    public static final String OFF = "off";
    public static final int MAX_NUM_ALLOWED_WARNINGS = 2;

    @Inject
    SecureDevice secureDevice;

    @Inject
    Settings settings;

    @Inject
    DeviceRegistration deviceRegistration;

    private boolean isClientSecurityPolicyEnabled() {
        return ON.equals(deviceRegistration.getClientSecurityPolicy());
    }

    @TargetApi(Build.VERSION_CODES.M)
    public boolean shouldEnforceSecurityPolicy() {

        boolean hasLockScreen = secureDevice.hasLockScreen();
        boolean clientSecurityPolicyEnabled = isClientSecurityPolicyEnabled();
        boolean keyguardSecure = secureDevice.isKeyguardSecure();
        int numSecurityEnforcementChecks = settings.getNumSecurityEnforcementChecks();

        boolean shouldEnforce = clientSecurityPolicyEnabled && !hasLockScreen && !deviceRegistration.getFeatures().hasDisabledPinEnforcement();

        // For help in troubleshooting issues seen in production
        String isDeviceSecure = UIUtils.hasMarshmallow() ? String.valueOf(secureDevice.isDeviceSecure()) : "NA";
        Ln.i("shouldEnforceSecurityPolicy ? %b clientSecurityPolicy = '%s' (%b) hasLockScreen ? %b keyguardSecure ? %b deviceSecure ? %s checksFailed = %d [d=%b]",
                shouldEnforce,
                deviceRegistration.getClientSecurityPolicy(),
                isClientSecurityPolicyEnabled(),
                hasLockScreen,
                keyguardSecure,
                isDeviceSecure,
                numSecurityEnforcementChecks,
                deviceRegistration.getFeatures().hasDisabledPinEnforcement()
        );
        return shouldEnforce;
    }

}
