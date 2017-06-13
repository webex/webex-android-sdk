package com.cisco.spark.android.sync;

import com.cisco.spark.android.model.Provider;

public class ParticipantUpdate {
    private ActorRecord.ActorKey actor;
    private ActorRecord.ActorKey object;
    private Provider provider;

    public ActorRecord.ActorKey getActor() {
        return actor;
    }

    public ActorRecord.ActorKey getObject() {
        return object != null ? object : actor;
    }

    public Provider getProvider() {
        return provider;
    }

    public ParticipantUpdate(ActorRecord.ActorKey actor, ActorRecord.ActorKey object, Provider provider) {
        this.actor = actor;
        this.object = object;
        this.provider = provider;
    }

    public boolean isForcedLeave() {
        return actor != null && object != null && !actor.equals(object);
    }
}
