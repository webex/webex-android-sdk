package com.cisco.spark.android.lyra;

import android.net.Uri;

public class BindingRequest {
    private Uri conversationUrl;
    private String kmsMessage;

    public BindingRequest(Uri conversationUrl, String kmsMessage) {
        this.conversationUrl = conversationUrl;
        this.kmsMessage = kmsMessage;
    }

    public Uri getConversationUrl() {
        return conversationUrl;
    }

    public String getKmsMessage() {
        return kmsMessage;
    }

}
