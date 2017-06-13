package com.cisco.spark.android.metrics.value;

import com.cisco.spark.android.metrics.ConversationMetricsBuilder;

/**
 * This metric is used to calculate the number of clicks that can't be handled
 * when clicking on conversation buttons (or conversations) in the conversation
 * list
 */

public class ConversationListClickMetric {

    private final ConversationMetricsBuilder.ClickTarget target;
    private final boolean succcess;

    public ConversationListClickMetric(ConversationMetricsBuilder.ClickTarget target, boolean succcess) {
        this.target = target;
        this.succcess = succcess;
    }

}
