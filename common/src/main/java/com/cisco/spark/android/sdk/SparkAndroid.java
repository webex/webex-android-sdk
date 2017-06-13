package com.cisco.spark.android.sdk;

import com.cisco.spark.android.sync.operationqueue.core.Operation;
import com.cisco.spark.android.wdm.DeviceInfo;

public class SparkAndroid implements SdkClient {

    @Override
    public String getDeviceType() {
        return DeviceInfo.ANDROID_DEVICE_TYPE;
    }

    @Override
    public boolean toastsEnabled() {
        return true;
    }

    @Override
    public boolean operationEnabled(Operation op) {
        return true;
    }

    @Override
    public boolean conversationCachingEnabled() {
        return true;
    }

    @Override
    public boolean supportsPrivateBoards() {
        return false;
    }

    @Override
    public boolean supportsReducedScopes() {
        return true;
    }
}
