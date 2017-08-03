package com.cisco.spark.android.callcontrol.events;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class CallControlCallJoinErrorEvent {

    private int joinType;

    public static final int JOIN = 108;
    public static final int ADD = 365;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = { JOIN, ADD })
    public @interface JoinType {
    }

    public CallControlCallJoinErrorEvent() {
        this(JOIN);
    }

    public CallControlCallJoinErrorEvent(@JoinType int joinType) {
        this.joinType = joinType;
    }

    public @JoinType int getJoinType() {
        return joinType;
    }
}
