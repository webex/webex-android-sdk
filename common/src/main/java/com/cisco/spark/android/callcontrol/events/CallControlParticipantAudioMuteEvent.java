package com.cisco.spark.android.callcontrol.events;

import com.cisco.spark.android.locus.model.LocusKey;
import com.cisco.spark.android.locus.model.LocusParticipant;

public class CallControlParticipantAudioMuteEvent extends CallControlMeetingControlsStatusEvent {

    // Mute/unmute indicator - true means mute; false means unmute.
    private boolean mute;
    private LocusParticipant participant;

    public CallControlParticipantAudioMuteEvent(LocusKey locuskey, Actor actor, boolean status, LocusParticipant participant, boolean mute) {
        super(locuskey, actor, status);
        this.mute = mute;
        this.participant = participant;
    }

    public LocusParticipant getParticipant() {
        return participant;
    }

    public boolean isMuted() {
        return mute;
    }
}
