package com.cisco.spark.android.events;

import com.cisco.spark.android.wdm.DeviceRegistration;

public class DeviceRegistrationChangedEvent {
    private DeviceRegistration deviceRegistration;
    private final String clientCompatibilityHint;
    private final boolean triggeredByGcmRegistration;

    public DeviceRegistrationChangedEvent(DeviceRegistration deviceRegistration, String clientCompatibilityHint) {
        this(deviceRegistration, clientCompatibilityHint, false);
    }

    public DeviceRegistrationChangedEvent(DeviceRegistration deviceRegistration, String clientCompatibilityHint, boolean triggeredByGcmRegistration) {
        this.deviceRegistration = deviceRegistration;
        this.clientCompatibilityHint = clientCompatibilityHint;
        this.triggeredByGcmRegistration = triggeredByGcmRegistration;
    }

    public DeviceRegistration getDeviceRegistration() {
        return deviceRegistration;
    }

    public String getClientCompatibilityHint() {
        return clientCompatibilityHint;
    }

    public boolean wasTriggeredByGcmRegistration() {
        return triggeredByGcmRegistration;
    }

}
