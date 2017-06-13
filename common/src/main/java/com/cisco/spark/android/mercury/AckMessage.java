package com.cisco.spark.android.mercury;

import java.util.Locale;

public class AckMessage {
    private final String type;
    private final String messageId;

    public static final String TYPE = "ack";

    public AckMessage(String type, String messageId) {
        this.type = type;
        this.messageId = messageId;
    }

    public AckMessage(String messageId) {
        this(TYPE, messageId);
    }

    public AckMessage(String type, int messageId) {
        this(TYPE, String.format(Locale.US, "%d", messageId));
    }

    public AckMessage(int messageId) {
        this(TYPE, messageId);
    }

    public String getType() {
        return type;
    }

    public String getMessageId() {
        return messageId;
    }

    public String toString() {
        return String.format(Locale.US, "{ \"type\": \"%s\", \"messageId\": \"%s\" }", type, messageId);
    }
}
