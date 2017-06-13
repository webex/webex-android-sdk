package com.cisco.spark.android.sync;

import com.cisco.spark.android.model.Activity;
import com.cisco.spark.android.model.Provider;

public class Tombstone {
    private ActorRecord.ActorKey actor;
    private ActorRecord.ActorKey author;
    private Provider provider;

    public Tombstone(Activity activity) {
        actor = activity.getActor().getKey();
        if (activity.getObject() != null && activity.getObject().isActivity()) {
            author = ((Activity) activity.getObject()).getActor().getKey();
            provider = ((Activity) activity.getObject()).getProvider();
        }
    }

    public Tombstone(ActorRecord.ActorKey actor, ActorRecord.ActorKey author, Provider provider) {
        this.actor = actor;
        this.author = author;
        this.provider = provider;
    }

    public Tombstone() {
    }

    public ActorRecord.ActorKey getActor() {
        return actor;
    }

    public ActorRecord.ActorKey getAuthor() {
        return author;
    }
}
