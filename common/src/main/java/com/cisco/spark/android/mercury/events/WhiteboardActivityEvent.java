package com.cisco.spark.android.mercury.events;

import com.cisco.spark.android.mercury.MercuryData;
import com.cisco.spark.android.mercury.MercuryEventType;

import java.util.UUID;

public class WhiteboardActivityEvent extends MercuryData {

    private UUID id;
    private Envelope envelope;
    private String payload;

    public WhiteboardActivityEvent() {
        super(MercuryEventType.BOARD_ACTIVITY);
    }

    public String getEncryptionKeyUrl() {

        if (envelope == null)
            return null;

        return envelope.encryptionKeyUrl;
    }

    public String getPayload() {
        return payload;
    }

    private static class Envelope {

        private String encryptionKeyUrl;
    }
}
