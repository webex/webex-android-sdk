package com.cisco.spark.android.mercury.events;

import com.cisco.spark.android.mercury.MercuryData;
import com.cisco.spark.android.mercury.MercuryEventType;

import java.util.UUID;

public class LyraSpaceRoomControlEvent extends MercuryData {

    private UUID actorUserId;

    public LyraSpaceRoomControlEvent(MercuryEventType eventType, UUID actorUserId) {
        super(eventType);
        this.actorUserId = actorUserId;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }
}
