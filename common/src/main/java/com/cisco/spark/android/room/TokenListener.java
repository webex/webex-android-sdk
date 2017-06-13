package com.cisco.spark.android.room;

import com.cisco.spark.android.room.audiopairing.UltrasoundMetrics;
import com.cisco.spark.android.sproximity.SProximityPairingCallback;

public interface TokenListener {

    void newTokenWithCallback(String token, UltrasoundMetrics ultrasoundMetrics, boolean firstToken, final SProximityPairingCallback callback);

    void newToken(String token, UltrasoundMetrics ultrasoundMetrics, boolean firstToken);
}
