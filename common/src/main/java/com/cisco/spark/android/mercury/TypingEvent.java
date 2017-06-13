package com.cisco.spark.android.mercury;

import com.cisco.spark.android.model.Person;

public class TypingEvent extends MercuryData {
    private String conversationId;
    private Person actor;

    public TypingEvent(String conversationId, boolean typing) {
        this(conversationId, null, typing);
    }

    public TypingEvent(String conversationId, Person actor, boolean typing) {
        super(typing ? MercuryEventType.START_TYPING : MercuryEventType.STOP_TYPING_EVENT);
        this.actor = actor;
        this.conversationId = conversationId;
    }

    public String getConversationId() {
        return conversationId;
    }

    public Person getActor() {
        return actor;
    }
}
