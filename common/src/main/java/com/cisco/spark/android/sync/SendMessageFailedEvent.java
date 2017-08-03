package com.cisco.spark.android.sync;

public class SendMessageFailedEvent {
    final private String operationId;
    final private String conversationId;
    final private String activityId;
    final private boolean isOneOnOne;

    public SendMessageFailedEvent(String operationId, String conversationId, String activityId, boolean isOneOnOne) {
        this.operationId = operationId;
        this.conversationId = conversationId;
        this.activityId = activityId;
        this.isOneOnOne = isOneOnOne;
    }

    public String getOperationId() {
        return operationId;
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getActivityId() {
        return activityId;
    }

    public boolean isOneOnOne() {
        return isOneOnOne;
    }
}
