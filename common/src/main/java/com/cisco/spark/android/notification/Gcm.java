package com.cisco.spark.android.notification;

import android.app.Activity;

public interface Gcm {
    String PROPERTY_REG_ID = "REGISTRATION_ID";
    String PROPERTY_BUILD_TIME = "BUILD_TIME";

    String register();

    boolean checkAvailability(Activity activity);

    void clear();
}
