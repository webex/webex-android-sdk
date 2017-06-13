package com.cisco.spark.android.wdm;

import android.os.Build;

import com.cisco.spark.android.model.RegionInfo;

import java.util.UUID;

@SuppressWarnings({ "FieldCanBeLocal", "unused" })
public class DeviceInfo {

    public static final String SPARKBOARD_DEVICE_TYPE = "SPARK_BOARD";
    public static final String ANDROID_DEVICE_TYPE = "ANDROID";
    public static final String UC_DEVICE_TYPE = "UC";
    public static final String TP_DEVICE_TYPE = "TP_ENDPOINT";

    private String gcmRegistrationId;
    private String countryCode;
    private String regionCode;
    private String name;
    private String model;
    private String localizedModel;
    private String systemName;
    private String systemVersion;
    public String deviceType;
    private Capabilities capabilities;
    private boolean isDeviceManaged;
    private String deviceIdentifier;

    public static DeviceInfo defaultConfig(String gcmRegistrationId, RegionInfo regionInfo) {
        return new Builder().gcmRegistrationId(gcmRegistrationId)
                            .regionInfo(regionInfo)
                            .deviceType(DeviceInfo.ANDROID_DEVICE_TYPE)
                            .name(Build.DEVICE)
                            .model(Build.MODEL)
                            .localizedModel(Build.MODEL)
                            .systemName(Build.PRODUCT)
                            .systemVersion(Build.VERSION.RELEASE)
                            .build();
    }

    private DeviceInfo(Builder builder) {
        gcmRegistrationId = builder.gcmRegistrationId;
        countryCode = builder.countryCode;
        regionCode = builder.regionCode;
        name = builder.name;
        model = builder.model;
        localizedModel = builder.localizedModel;
        systemName = builder.systemName;
        systemVersion = builder.systemVersion;
        setDeviceType(builder.deviceType);
        capabilities = builder.capabilities;
        setDeviceManaged(builder.isDeviceManaged);
        deviceIdentifier = builder.deviceIdentifier;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public void setDeviceManaged(boolean deviceManaged) {
        isDeviceManaged = deviceManaged;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceID(UUID deviceID) {
        deviceIdentifier = deviceID.toString();
    }

    public void setRegionInfo(RegionInfo regionInfo) {
        if (regionInfo != null) {
            this.regionCode = regionInfo.getRegionCode();
            this.countryCode = regionInfo.getCountryCode();
        }
    }

    public String getModel() {
        return model;
    }

    public String getName() {
        return name;
    }

    @SuppressWarnings("unused")
    public static final class Builder {

        private String gcmRegistrationId;
        private String countryCode;
        private String regionCode;
        private String name;
        private String model;
        private String localizedModel;
        private String systemName;
        private String systemVersion;
        private String deviceType;
        private String deviceIdentifier;

        private Capabilities capabilities;
        private boolean isDeviceManaged;

        public Builder() {
            capabilities = new Capabilities();
        }

        public Builder gcmRegistrationId(String registrationId) {
            gcmRegistrationId = registrationId;
            return this;
        }

        public Builder regionInfo(RegionInfo regionInfo) {
            if (regionInfo != null) {
                regionCode = regionInfo.getRegionCode();
                countryCode = regionInfo.getCountryCode();
            }
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder localizedModel(String localizedModel) {
            this.localizedModel = localizedModel;
            return this;
        }

        public Builder systemName(String systemName) {
            this.systemName = systemName;
            return this;
        }

        public Builder systemVersion(String systemVersion) {
            this.systemVersion = systemVersion;
            return this;
        }

        public Builder deviceType(String deviceType) {
            this.deviceType = deviceType;
            return this;
        }

        public Builder capabilities(Capabilities capabilities) {
            this.capabilities = capabilities;
            return this;
        }

        public Builder isDeviceManaged(boolean isDeviceManaged) {
            this.isDeviceManaged = isDeviceManaged;
            return this;
        }

        public Builder deviceIdentifier(String deviceIdentifier) {
            this.deviceIdentifier = deviceIdentifier;
            return this;
        }

        public DeviceInfo build() {
            return new DeviceInfo(this);
        }
    }
}
