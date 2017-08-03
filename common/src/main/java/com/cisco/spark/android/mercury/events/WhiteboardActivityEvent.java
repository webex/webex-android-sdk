package com.cisco.spark.android.mercury.events;

import android.text.TextUtils;

import com.cisco.spark.android.mercury.MercuryData;
import com.cisco.spark.android.mercury.MercuryEventType;

import java.util.UUID;

public class WhiteboardActivityEvent extends MercuryData {

    private UUID id;
    private Envelope envelope;
    private String payload;
    private String channelId;

    public WhiteboardActivityEvent() {
        super(MercuryEventType.BOARD_ACTIVITY);
    }

    public String getEncryptionKeyUrl() {

        if (envelope == null)
            return null;

        return envelope.encryptionKeyUrl;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getChannelId() {
        if (envelope == null)
            return null;
        return TextUtils.isEmpty(envelope.channelId) ? channelId : envelope.channelId;
    }

    public String getPayload() {
        return payload;
    }

    private static class Envelope {

        private String encryptionKeyUrl;
        private String channelId;
    }
}
