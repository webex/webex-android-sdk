package com.cisco.spark.android.util;

import android.content.pm.*;

import javax.inject.*;

@Singleton
public class AppVersionProvider {
    @Inject
    protected PackageInfo info;

    public String getVersionName() {
        if (info != null) {
            return info.versionName;
        } else {
            return "1.0";
        }
    }

    public int getVersionCode() {
        if (info != null) {
            return info.versionCode;
        } else {
            return 1;
        }
    }
}
