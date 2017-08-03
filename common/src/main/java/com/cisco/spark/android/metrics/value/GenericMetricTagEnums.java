package com.cisco.spark.android.metrics.value;

/*
 * NOTE: Please add new fields in alphabetical order!
 * Doing this will help reduce merge conflicts.
 */
public class GenericMetricTagEnums {
    public interface MetricTagValue {
        String getValue();
    }

    /**
     * Use these values with ClientMetricTag.METRIC_TAG_CONVERSATION_TYPE
     */
    public enum ConversationTypeMetricTagValue implements MetricTagValue {
        CONVERSATION_TYPE_GROUP("Group"),
        CONVERSATION_TYPE_ONE_TO_ONE("One2One"),
        CONVERSATION_TYPE_TEAM_SPACE("TeamSpace"),
        CONVERSATION_TYPE_NEW_TEAM("Team");

        private String value;

        ConversationTypeMetricTagValue(String value) {
            this.value = value;
        }

        public String getValue() {
            return this.value;
        }
    }

    public enum DataTypeMetricTagValue implements MetricTagValue {
        DATA_TYPE_FILE("file"),
        DATA_TYPE_THUMBNAIL("thumbnail"),
        DATA_TYPE_AVATAR("avatar"),
        DATA_TYPE_CONVERSATION_AVATAR("conversation_avatar"),
        DATA_TYPE_PREVIEW("preview"),
        DATA_TYPE_UNKNOWN("unknown");

        private String value;

        DataTypeMetricTagValue(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
