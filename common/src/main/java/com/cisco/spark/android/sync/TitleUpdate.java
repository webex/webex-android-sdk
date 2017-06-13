package com.cisco.spark.android.sync;

import com.cisco.spark.android.model.Provider;

public class TitleUpdate {
    private String title;
    private ActorRecord.ActorKey actorKey;
    private Provider provider;

    public String getTitle() {
        return title;
    }

    public ActorRecord.ActorKey getActorKey() {
        return actorKey;
    }

    public Provider getProvider() {
        return provider;
    }

    public TitleUpdate(String title, ActorRecord.ActorKey actorKey, Provider provider) {
        this.title = title;
        this.actorKey = actorKey;
        this.provider = provider;
    }
}
