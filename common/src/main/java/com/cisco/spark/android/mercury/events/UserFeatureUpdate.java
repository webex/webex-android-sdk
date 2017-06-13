package com.cisco.spark.android.mercury.events;

import com.cisco.spark.android.mercury.MercuryData;

public class UserFeatureUpdate extends MercuryData {
    private String appName;
    private String data;
    private String action;

    public String getAppName() {
        return appName;
    }

    public String getData() {
        return data;
    }

    public String getAction() {
        return action;
    }
}
