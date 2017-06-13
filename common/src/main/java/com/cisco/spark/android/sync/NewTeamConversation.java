package com.cisco.spark.android.sync;

import com.cisco.spark.android.model.Provider;

public class NewTeamConversation {
    private ActorRecord.ActorKey actor;
    private Provider provider;
    private String newConversationId;

    public NewTeamConversation(ActorRecord.ActorKey actor, String newConversationId, Provider provider) {
        this.actor = actor;
        this.provider = provider;
        this.newConversationId = newConversationId;
    }

    public ActorRecord.ActorKey getActor() {
        return actor;
    }

    public String getNewConversationId() {
        return newConversationId;
    }

    public Provider getProvider() {
        return provider;
    }
}
