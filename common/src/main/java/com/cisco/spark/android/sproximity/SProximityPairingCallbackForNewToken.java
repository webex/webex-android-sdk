package com.cisco.spark.android.sproximity;

import com.cisco.spark.android.room.MetricsPairingErrorEvent;
import de.greenrobot.event.EventBus;


public class SProximityPairingCallbackForNewToken implements SProximityPairingCallback {

    private EventBus eventBus;

    public SProximityPairingCallbackForNewToken(EventBus bus) {
        this.eventBus = bus;
    }
    @Override
    public void onFailure(String errorMessage, String errorCode) {
        eventBus.post(new MetricsPairingErrorEvent(errorMessage + errorCode));
    }

    @Override
    public void onSuccess() {
    }
}
