package com.cisco.spark.android.sync;

import com.cisco.spark.android.model.Provider;

public class TeamColorUpdate {
    private String color;
    private ActorRecord.ActorKey actorKey;
    private Provider provider;

    public String getColor() {
        return color;
    }

    public ActorRecord.ActorKey getActorKey() {
        return actorKey;
    }

    public Provider getProvider() {
        return provider;
    }

    public TeamColorUpdate(String color, ActorRecord.ActorKey actorKey, Provider provider) {
        this.color = color;
        this.actorKey = actorKey;
        this.provider = provider;
    }
}
