package com.cisco.spark.android.mercury.events;

import com.cisco.spark.android.mercury.MercuryEventType;

import java.util.UUID;

public class LyraSpaceAudioVolumeChangeEvent extends LyraSpaceRoomControlEvent {

    public static final String VOLUME_DECREASE_ACTION = "decrease";
    public static final String VOLUME_INCREASE_ACTION = "increase";

    private String action;

    public LyraSpaceAudioVolumeChangeEvent(UUID actorUserId, String action) {
        super(MercuryEventType.LYRA_SPACE_AUDIO_VOLUME_CHANGE, actorUserId);
        this.action = action;
    }

    public String getAction() {
        return action;
    }

}
