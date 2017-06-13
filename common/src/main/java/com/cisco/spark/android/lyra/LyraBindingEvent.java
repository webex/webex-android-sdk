package com.cisco.spark.android.lyra;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class LyraBindingEvent {

    public static final int LYRA_BINDING = 1;
    public static final int LYRA_UNBINDING = 2;
    public static final int LYRA_GETSTATE = 3;
    public static final int LYRA_ROOM_UPDATE = 4;
    public static final int LYRA_ROOM_LEFT = 5;

    @IntDef({LYRA_BINDING, LYRA_UNBINDING, LYRA_GETSTATE, LYRA_ROOM_UPDATE, LYRA_ROOM_LEFT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Events {
    }

    private @Events int event;
    private boolean result;
    private String conversationUrl;

    public LyraBindingEvent(@Events int event, boolean result) {
        this.event = event;
        this.result = result;
    }

    public String getConversationUrl() {
        return conversationUrl;
    }

    public void setConversationUrl(String conversationUrl) {
        this.conversationUrl = conversationUrl;
    }

    @Events
    public int getEvent() {
        return event;
    }

    public boolean isResult() {
        return result;
    }
}
