package com.cisco.spark.android.mercury.events;

import com.cisco.spark.android.mercury.MercuryData;
import com.cisco.spark.android.mercury.MercuryEventType;

public class LyraActivityEvent extends MercuryData {
    private String spaceUrl;

    public LyraActivityEvent(String spaceUrl) {
        super(MercuryEventType.LYRA_SPACE_UPDATE_EVENT);
        this.spaceUrl = spaceUrl;
    }

    public String getSpaceUrl() {
        return spaceUrl;
    }
}
