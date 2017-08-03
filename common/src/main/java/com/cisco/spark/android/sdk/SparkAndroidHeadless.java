package com.cisco.spark.android.sdk;

/**
 * Modifies the SparkAndroid client when run as a headless test user
 * Used to avoid toasts from headless test users
 */
public class SparkAndroidHeadless extends SparkAndroid implements SdkClient {

    @Override
    public boolean toastsEnabled() {
        return false;
    }

}
