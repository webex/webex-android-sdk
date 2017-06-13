package com.cisco.spark.android.lyra;


import android.net.Uri;

public class BindingResponse {

    private Uri bindingUrl;
    private Uri conversationUrl;

    public BindingResponse(Uri bindingUrl, Uri conversationUrl) {
        this.bindingUrl = bindingUrl;
        this.conversationUrl = conversationUrl;
    }

    public Uri getConversationUrl() {
        return conversationUrl;
    }

    public Uri getBindingUrl() {
        return bindingUrl;
    }
}
