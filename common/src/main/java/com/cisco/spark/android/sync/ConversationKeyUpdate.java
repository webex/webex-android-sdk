package com.cisco.spark.android.sync;

import android.net.Uri;
import com.cisco.spark.android.model.Activity;
import com.cisco.spark.android.model.Conversation;
import com.cisco.spark.android.model.Provider;

public class ConversationKeyUpdate {
    private Uri defaultActivityEncryptionKeyUrl;
    private ActorRecord.ActorKey actorKey;
    private Provider provider;

    public Uri getDefaultActivityEncryptionKeyUrl() {
        return defaultActivityEncryptionKeyUrl;
    }

    public ActorRecord.ActorKey getActorKey() {
        return actorKey;
    }

    public Provider getProvider() {
        return provider;
    }

    public ConversationKeyUpdate(Uri defaultActivityEncryptionKeyUrl, ActorRecord.ActorKey actorKey, Provider provider) {
        this.defaultActivityEncryptionKeyUrl = defaultActivityEncryptionKeyUrl;
        this.actorKey = actorKey;
        this.provider = provider;
    }

    public static ConversationKeyUpdate fromActivity(Activity activity) {
        if (activity.getObject().isConversation()) {
            Conversation conversationObject = (Conversation) activity.getObject();
            return new ConversationKeyUpdate(conversationObject.getDefaultActivityEncryptionKeyUrl(), activity.getActor().getKey(), activity.getProvider());
        }
        return null;
    }
}
