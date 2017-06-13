package com.cisco.spark.android.sync;

import android.support.annotation.StringRes;

import com.cisco.spark.android.R;
import com.cisco.spark.android.model.LocusSessionSummary;
import com.cisco.spark.android.model.LocusSessionSummaryParticipant;

import java.util.Date;

public class CallSession {
    public enum State { CONNECTED, MISSED, UNAVAILABLE}

    private State state;
    private long duration;
    private Date startTime;
    private ActorRecord.ActorKey actorKey;
    private boolean isGroupCall;
    private boolean declined;

    public State getState() {
        return state;
    }

    public long getDuration() {
        return duration;
    }

    public long getDurationInMinutes() {
        return Math.max(1, duration / 60);
    }

    public Date getStartTime() {
        return startTime;
    }

    public ActorRecord.ActorKey getActorKey() {
        return actorKey;
    }

    public boolean isGroupCall() {
        return isGroupCall;
    }

    public @StringRes int getStringId(final ActorRecord.ActorKey self) {
        if (!isGroupCall) {
            if (state == State.CONNECTED) {
                return R.string.call_banner_answered;
            } else if (state == State.UNAVAILABLE) {
                if (self.equals(actorKey)) {
                    return R.string.call_banner_you_declined;
                } else {
                    return R.string.call_banner_x_declined;
                }
            } else {
                if (self.equals(actorKey)) {
                    return R.string.call_banner_you_timeout;
                } else {
                    return R.string.call_banner_x_timeout;
                }
            }
        } else {
            if (state == State.CONNECTED) {
                return R.string.call_banner_syncup;
            } else if (state == State.MISSED) {
                return R.string.call_banner_missed_syncup;
            } else {
                if (self.equals(actorKey)) {
                    return R.string.call_banner_you_unvailable_syncup;
                } else {
                    return R.string.call_banner_x_unvailable_syncup;
                }
            }
        }
    }

    public static CallSession fromLocusSessionSummary(ActorRecord.ActorKey self, LocusSessionSummary summary) {
        CallSession session = new CallSession();

        int declined = 0;
        int connected = 0;

        LocusSessionSummaryParticipant selfParticipant = null;
        // declined and missed participant are only used for 1:1 calls
        LocusSessionSummaryParticipant declinedParticipant = null;
        LocusSessionSummaryParticipant missedParticipant = null;

        for (LocusSessionSummaryParticipant participant : summary.getParticipants().getItems()) {
            if (participant.isInitiator()) {
                session.actorKey = participant.getPerson().getKey();
            }

            if (participant.getPerson().getKey().equals(self)) {
                selfParticipant = participant;
            }

            if (participant.getState() == LocusSessionSummaryParticipant.State.LEFT) {
                connected++;
            } else if (participant.getState() == LocusSessionSummaryParticipant.State.DECLINED) {
                declined++;
                declinedParticipant = participant;
            } else {
                missedParticipant = participant;
            }
        }

        if (summary.isGroupCall()) {
            if (connected >= 2) {
                session.state = State.MISSED;
                if (selfParticipant != null && selfParticipant.getState() == LocusSessionSummaryParticipant.State.LEFT) {
                    session.state =  State.CONNECTED;
                }
            } else {
                session.state = State.UNAVAILABLE;
            }
        } else {
            if (connected >= 2) {  // cater for paired 1:1 calls as well where number of participants is 3
                session.state = State.CONNECTED;
            } else {
                if (declined > 0 && selfParticipant != null) {
                    session.state = State.UNAVAILABLE;
                    session.actorKey = declinedParticipant.getPerson().getKey();
                } else {
                    session.state = State.MISSED;
                    assert missedParticipant != null;
                    session.actorKey = missedParticipant.getPerson().getKey();
                }
            }
        }

        session.declined = selfParticipant != null && selfParticipant.getState() == LocusSessionSummaryParticipant.State.DECLINED;
        session.duration = summary.getDuration();
        session.isGroupCall = summary.isGroupCall();
        session.startTime = summary.getStartTime();

        return session;
    }
}
