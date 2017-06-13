package com.cisco.spark.android.metrics;

import com.cisco.spark.android.metrics.value.ConversationListClickMetric;

public class ConversationMetricsBuilder extends SplunkMetricsBuilder {

    private static final String SPACE_LIST_CLICK = "androidSpaceListClick";

    public ConversationMetricsBuilder(MetricsEnvironment environment) {
        super(environment);
    }

    public enum ClickTarget {
        VIEW_BUTTON, // The user clicked the VIEW button for a space in the list.
        VIEW_TIMER, // The user clicked the VIEW timer for a space in the list.
        VIEW_UNKNOWN, // The user clicked on something else which triggers the VIEW action
        JOIN_BUTTON, // The user clicked on the JOIN button for a space in the list
        JOIN_TIMER, // The user clicked on the timer under a JOIN button for a space in the list
        JOIN_UNKNOWN, // The user clicked on something else which triggers the JOIN action
        OPEN_CONVERSATION // When the user clicks on a space in the space list.
    }

    public MetricsBuilder reportClick(ClickTarget target, boolean success) {
        reportValue(SPACE_LIST_CLICK, new ConversationListClickMetric(target, success));
        return this;
    }

}
