package com.cisco.spark.android.callcontrol.events;

import com.cisco.spark.android.locus.model.LocusKey;

public class CallControlCallInitiatedEvent {

    private final LocusKey locusKey;
    private final String invitee;

    /**
     * This event is used to tell the application that a call has been initiated
     * The call is either created with a locusKey or a invitee dialable.
     * @param locusKey
     * @param invitee
     */
    private CallControlCallInitiatedEvent(LocusKey locusKey, String invitee) {
        this.locusKey = locusKey;
        this.invitee = invitee;
    }

    /**
     * Call was initiated with locusKey (joinLocus)
     */
    public static CallControlCallInitiatedEvent fromLocus(LocusKey locusKey) {
        return new CallControlCallInitiatedEvent(locusKey, null);
    }

    /**
     * Call was initiated with invitee (callLocus)
     * @param invitee the dialable thing used to call locus
     */
    public static CallControlCallInitiatedEvent fromCall(String invitee) {
        return new CallControlCallInitiatedEvent(null, invitee);
    }

    public String getInvitee() {
        return invitee;
    }

    public LocusKey getLocusKey() {
        return locusKey;
    }
}
