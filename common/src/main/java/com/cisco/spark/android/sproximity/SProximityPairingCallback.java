package com.cisco.spark.android.sproximity;

public interface SProximityPairingCallback {

    void onFailure(String errorMessage, String errorCode);

    void onSuccess();
}
