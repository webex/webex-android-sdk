package com.cisco.spark.android.locus.events;

import android.net.Uri;

import com.cisco.spark.android.locus.model.LocusKey;

public class LocusUrlUpdatedEvent {
    private final Uri conversationUrl;
    private final LocusKey oldLocusKey;
    private final LocusKey newLocusKey;

    public LocusUrlUpdatedEvent(Uri conversationUrl, LocusKey oldLocusKey, LocusKey newLocusKey) {
        this.conversationUrl = conversationUrl;
        this.oldLocusKey = oldLocusKey;
        this.newLocusKey = newLocusKey;
    }

    public Uri getConversationUrl() {
        return conversationUrl;
    }

    public LocusKey getOldLocusKey() {
        return oldLocusKey;
    }

    public LocusKey getNewLocusKey() {
        return newLocusKey;
    }
}
