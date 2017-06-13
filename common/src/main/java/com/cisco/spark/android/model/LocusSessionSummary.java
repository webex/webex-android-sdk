package com.cisco.spark.android.model;

import java.util.Date;
import java.util.List;

public class LocusSessionSummary extends ActivityObject {
    private Date startTime;
    private long duration;
    private boolean isGroupCall;
    private ItemCollection<LocusSessionSummaryParticipant> participants;

    public LocusSessionSummary(Date startTime, long duration, boolean isGroupCall, List<LocusSessionSummaryParticipant> participants) {
        super(ObjectType.locusSessionSummary);
        this.startTime = startTime;
        this.duration = duration;
        this.isGroupCall = isGroupCall;
        this.participants = new ItemCollection<>(participants);
    }

    public boolean isGroupCall() {
        return isGroupCall;
    }

    public Date getStartTime() {
        return startTime;
    }

    public long getDuration() {
        return duration;
    }

    public ItemCollection<LocusSessionSummaryParticipant> getParticipants() {
        return participants;
    }
}
