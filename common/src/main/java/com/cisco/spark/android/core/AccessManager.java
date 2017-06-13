package com.cisco.spark.android.core;

import android.support.annotation.Nullable;

import com.cisco.spark.android.model.RegionInfo;

public interface AccessManager {
    void revokeAccess();
    void grantAccess();
    boolean isAccessGranted();
    @Nullable RegionInfo getRegion();
    void reset();
}
