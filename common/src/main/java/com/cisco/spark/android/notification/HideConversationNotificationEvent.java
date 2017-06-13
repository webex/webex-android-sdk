package com.cisco.spark.android.notification;

public class HideConversationNotificationEvent {
    private final String conversationId;

    public HideConversationNotificationEvent(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getConversationId() {
        return conversationId;
    }
}
