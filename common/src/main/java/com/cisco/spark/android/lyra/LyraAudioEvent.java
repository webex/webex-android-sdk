package com.cisco.spark.android.lyra;

import android.support.annotation.IntDef;

import com.cisco.spark.android.model.AudioState;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class LyraAudioEvent {
    public static final int LYRA_AUDIO_UPDATE = 1;

    private @Events int event;
    private final AudioState audioState;

    @IntDef({LYRA_AUDIO_UPDATE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Events {
    }

    @Events
    public int getEvent() {
        return event;
    }

    public AudioState getAudioState() {
        return audioState;
    }

    public LyraAudioEvent(@Events int event, AudioState audioState) {
        this.event = event;
        this.audioState = audioState;
    }
}
