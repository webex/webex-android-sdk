package com.cisco.spark.android.callcontrol.events;

import com.cisco.spark.android.locus.model.Locus;
import com.cisco.spark.android.locus.model.LocusParticipant;
import java.util.List;

public class CallControlBindingEvent {

    private final BindingEventType type;
    private final List<LocusParticipant> leftParticipants;
    private final Locus locus;

    public CallControlBindingEvent(BindingEventType type, Locus locus, List<LocusParticipant> participants) {
        this.type = type;
        this.locus = locus;
        this.leftParticipants = participants;
    }

    public BindingEventType getType() {
        return type;
    }

    public List<LocusParticipant> getLeftParticipants() {
        return leftParticipants;
    }

    public Locus getLocus() {
        return locus;
    }

    public enum BindingEventType {
        BIND,
        UNBIND
    }
}
