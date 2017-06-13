package com.cisco.spark.android.model;

import java.net.URI;
import java.util.UUID;

public class Participant {

    //These are filled in by gson through reflection
    private URI url;
    private URI deviceUrl;
    private String state;
    private UUID id;
    private boolean isCreator;
    private User person;
    //

    // The current state of the participant as copied from LocusParticipant.java in the cloud-apps git repository.

    public static enum State {
        /* the participant is not in the locus */
        IDLE,

        /* one of more devices of the participant are alerting */
        NOTIFIED,

        /* the participant is in the locus */
        JOINED,

        /* the participant is not in the locus */
        LEFT,

        /* the participant temporarily left the locus */
        LEFT_TEMPORARILY,

        /* the locus cannot locate the participant */
        DOES_NOT_EXIST,

        /* the participant has been removed from the locus by the administrator */
        BOOTED,

        /* the participant declined to join */
        DECLINED,

        /* the participant cannot be reached */
        GONE,

        /* the locus server encountered an error with this participant */
        ERROR
    }

    public URI getUrl() {
        return url;
    }

    public URI getDeviceUrl() {
        return deviceUrl;
    }

    public String getState() {
        return state;
    }

    public UUID getId() {
        return id;
    }

    public boolean isCreator() {
        return isCreator;
    }

    public User getPerson() {
        return person;
    }
}
