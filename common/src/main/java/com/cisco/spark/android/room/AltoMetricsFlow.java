package com.cisco.spark.android.room;

import android.support.annotation.Nullable;

import com.cisco.spark.android.room.model.RoomState;

/**
 * A pairing flow consists of various stages, and require various stages to be hit to be completed
 * A complete flow will publish metrics to splunk
 */
public interface AltoMetricsFlow {

    void event(AltoFlowEvent event, @Nullable RoomState roomState);
    void publishMetrics();

}
