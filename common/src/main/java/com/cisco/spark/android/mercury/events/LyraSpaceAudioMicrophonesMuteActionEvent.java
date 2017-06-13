package com.cisco.spark.android.mercury.events;

import com.cisco.spark.android.mercury.MercuryEventType;

import java.util.UUID;

public class LyraSpaceAudioMicrophonesMuteActionEvent extends  LyraSpaceRoomControlEvent {

    public static final String MUTE_ACTION = "mute";
    public static final String UNMUTE_ACTION = "unmute";

    private String action;

    public LyraSpaceAudioMicrophonesMuteActionEvent(UUID actorUserId, String action) {
        super(MercuryEventType.LYRA_SPACE_AUDIO_MICROPHONES_MUTE, actorUserId);
        this.action = action;
    }

    public String getAction() {
        return action;
    }
}
