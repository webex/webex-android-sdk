package com.cisco.spark.android.whiteboard.persistence.model;

import android.net.Uri;

public class BoardRegistrationBinding {
    private String binding;
    private Uri channelUrl;

    public String getBinding() {
        return binding;
    }

    public Uri getChannelUrl() {
        return channelUrl;
    }
}
