package com.cisco.spark.android.whiteboard;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class WhiteboardResaveContentEvent {

    public static final int RESAVING = 1;
    public static final int RESAVE_FAILED = 2;
    public static final int RESAVE_SUCCEED = 3;

    private @Events int event;

    @IntDef({RESAVING, RESAVE_FAILED, RESAVE_SUCCEED})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Events {
    }

    @Events
    public int getEvent() {
        return event;
    }

    public WhiteboardResaveContentEvent(@Events int event) {
        this.event = event;
    }
}
