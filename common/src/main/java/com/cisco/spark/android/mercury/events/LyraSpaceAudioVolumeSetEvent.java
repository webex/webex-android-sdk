package com.cisco.spark.android.mercury.events;

import com.cisco.spark.android.mercury.MercuryEventType;

import java.util.UUID;

public class LyraSpaceAudioVolumeSetEvent extends LyraSpaceRoomControlEvent {

    private int level;

    public LyraSpaceAudioVolumeSetEvent(UUID actorUserId, int level) {
        super(MercuryEventType.LYRA_SPACE_AUDIO_VOLUME_SET, actorUserId);
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

}
