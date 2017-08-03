package com.cisco.spark.android.metrics.model;

import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.BaseApiClientProvider;
import com.cisco.spark.android.metrics.value.ClientMetricField;
import com.cisco.spark.android.metrics.value.ClientMetricTag;
import com.cisco.spark.android.metrics.value.GenericMetricTagEnums;
import com.cisco.spark.android.model.ErrorDetail;
import com.cisco.wx2.diagnostic_events.ClientEvent;
import com.cisco.wx2.diagnostic_events.Event;

import org.joda.time.Instant;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import retrofit2.Response;

public class GenericMetric {
    private static final int MAX_METRICS_CACHE = 100;
    private static final ArrayList<GenericMetric> metricsCache = new ArrayList<>(MAX_METRICS_CACHE);
    private static final Object syncObj = new Object();
    private static final String BEHAVIORAL_TYPE = "behavioral";
    private static final String OPERATIONAL_TYPE = "operational";
    private static final String DIAGNOSTIC_TYPE = "diagnostic-event";
    private static final String NETWORK_FAILURE_STATUS = "user_false";
    private static final String NETWORK_FAILURE_DESCRIPTION = "Device has no network";

    private String metricName;
    private Map<String, Object> tags;
    private Map<String, Object> fields;
    protected Set<String> type;

    private Event eventPayload;

    public static GenericMetric buildBehavioralMetric(String metricName) {
        GenericMetric metric = new GenericMetric(metricName).withType(BEHAVIORAL_TYPE);
        cacheMetric(metric);
        return metric;
    }

    public static GenericMetric buildOperationalMetric(String metricName) {
        GenericMetric metric = new GenericMetric(metricName).withType(OPERATIONAL_TYPE);
        cacheMetric(metric);
        return metric;
    }

    public static GenericMetric buildBehavioralAndOperationalMetric(String metricName) {
        GenericMetric metric = new GenericMetric(metricName).withType(OPERATIONAL_TYPE).withType(BEHAVIORAL_TYPE);
        cacheMetric(metric);
        return metric;
    }

    public static GenericMetric buildDiagnosticMetric(Event eventPayload) {
        return new GenericMetric(eventPayload).withType(DIAGNOSTIC_TYPE);
    }

    public static GenericMetric[] getMetricsCache() {
        synchronized (syncObj) {
            return Arrays.copyOf(metricsCache.toArray(), metricsCache.size(), GenericMetric[].class);
        }
    }

    private static void cacheMetric(GenericMetric metric) {
        synchronized (syncObj) {
            metricsCache.add(metric);

            if (metricsCache.size() > MAX_METRICS_CACHE) {
                metricsCache.remove(0);
            }
        }
    }

    private GenericMetric(String metricName) {
        this.metricName = metricName;
        tags = new HashMap<>();
        fields = new HashMap<>();
        type = new HashSet<>();

        eventPayload = null;
    }

    private GenericMetric(Event eventPayload) {
        this.eventPayload = eventPayload;
        type = new HashSet<>();

        metricName = null;
        tags = null;
        fields = null;
    }

    public boolean isDiagnosticEvent() {
        return type.contains(DIAGNOSTIC_TYPE);
    }

    public void addTag(ClientMetricTag tag, Object tagValue) {
        this.tags.put(tag.getTagName(), tagValue != null ? tagValue.toString() : null);
    }

    public void addTag(ClientMetricTag tag, String tagValue) {
        this.tags.put(tag.getTagName(), tagValue);
    }

    public void addTag(ClientMetricTag tag, boolean tagValue) {
        this.tags.put(tag.getTagName(), Boolean.toString(tagValue));
    }

    public void addTags(Map<String, Object> tags) {
        this.tags.putAll(tags);
    }

    public void addTag(ClientMetricTag tag, GenericMetricTagEnums.MetricTagValue value) {
        this.tags.put(tag.getTagName(), value.getValue());
    }

    public void addField(ClientMetricField field, Object fieldValue) {
        this.fields.put(field.getFieldName(), fieldValue);
    }

    public void addFields(Map<String, Object> fields) {
        this.fields.putAll(fields);
    }

    public Object getTag(ClientMetricTag metricTag) {
        return getTag(metricTag.getTagName());
    }

    public Object getTag(String metricTag) {
        return this.tags.get(metricTag);
    }

    public int getTagSize() {
        return this.tags.size();
    }

    public Set<String> getTagKeySet() {
        return this.tags.keySet();
    }

    public Object getField(ClientMetricField metricField) {
        return getField(metricField.getFieldName());
    }

    public Object getField(String fieldName) {
        return this.fields.get(fieldName);
    }

    public int getFieldSize() {
        return this.tags.size();
    }

    public Set<String> getFieldKeySet() {
        return this.fields.keySet();
    }

    public String getMetricName() {
        return metricName;
    }

    public String toString() {
        if (metricName != null) {
            return "GenericMetric metricName " + metricName;
        } else if (eventPayload != null) {
            if (eventPayload.getEvent() instanceof ClientEvent) {
                return "GenericMetric event " + ((ClientEvent) eventPayload.getEvent()).getName().toString();
            } else {
                return "GenericMetric event " + eventPayload.getEventId().toString();
            }
        } else {
            return "GenericMetric";
        }
    }

    /**
     * Use this to add network response information for operational metrics.
     * @param response The response object from which the metric information will be populated. If this is null,
     *                 it signals that there was no network available
     * @throws IOException if parsing the response error body fails
     */
    public void addNetworkStatus(Response response) throws IOException {
        if (response == null) {
            addTag(ClientMetricTag.METRIC_TAG_SUCCESS_TAG, NETWORK_FAILURE_STATUS);
            addField(ClientMetricField.METRIC_FIELD_ERROR_DESCRIPTION, NETWORK_FAILURE_DESCRIPTION);
        } else  {
            addTag(ClientMetricTag.METRIC_TAG_SUCCESS_TAG, response.code() < 400);
            addTag(ClientMetricTag.METRIC_TAG_HTTP_STATUS, response.code());
            addField(ClientMetricField.METRIC_FIELD_TRACKING_ID, response.headers().get(BaseApiClientProvider.TRACKING_ID_HEADER));

            if (response.code() >= 400) {
                ErrorDetail errorDetail = ApiClientProvider.getErrorDetailConverter().convert(response.errorBody());

                if (errorDetail != null) {
                    addTag(ClientMetricTag.METRIC_TAG_ERROR_CODE, errorDetail.getErrorCode());
                    addField(ClientMetricField.METRIC_FIELD_ERROR_DESCRIPTION, errorDetail.getMessage());
                }
            }
        }
    }

    /*
     * Helper to add a client generated error.
     */
    public void addClientError(int errorCode, String errorDescription) {
        addTag(ClientMetricTag.METRIC_TAG_SUCCESS_TAG, false);
        addTag(ClientMetricTag.METRIC_TAG_ERROR_CODE, errorCode);
        addField(ClientMetricField.METRIC_FIELD_ERROR_DESCRIPTION, errorDescription);
    }

    /**
     * Use this to add network response information for behavioral metrics. This is different than
     * the method above for operational metrics because it uses different names for the various tags/fields
     *
     * @param response The response object from which the metric information will be populated. If this is null,
     *                 it signals that there was no network available
     * @return Returns back the metric object you called this on
     * @throws IOException if parsing the response's error body to a string fails
     */
    public GenericMetric withNetworkTraits(Response response) throws IOException {
        if (response == null) {
            addTag(ClientMetricTag.METRIC_TAG_WAS_SUCCESSFUL, NETWORK_FAILURE_STATUS);
            addField(ClientMetricField.METRIC_FIELD_FAILURE_REASON, NETWORK_FAILURE_DESCRIPTION);
        } else {
            addTag(ClientMetricTag.METRIC_TAG_WAS_SUCCESSFUL, response.code() < 400);

            if (response.code() >= 400) {
                addField(ClientMetricField.METRIC_FIELD_FAILURE_CODE, response.code());
                addField(ClientMetricField.METRIC_FIELD_FAILURE_REASON, (response.errorBody() != null) ? response.errorBody().string() : response.message());
            }
        }
        return this;
    }

    public GenericMetric withNetworkTraits(ErrorDetail error) {
        if (error != null) {
            addTag(ClientMetricTag.METRIC_TAG_WAS_SUCCESSFUL, false);
            addField(ClientMetricField.METRIC_FIELD_FAILURE_CODE, error.getErrorCode());
            addField(ClientMetricField.METRIC_FIELD_FAILURE_REASON, error.getMessage());
        }
        return this;
    }

    private GenericMetric withType(String typeToAdd) {
        type.add(typeToAdd);
        return this;
    }

    public void onInstantBeforeSend() {
        if (eventPayload != null) {
            eventPayload.getOriginTime().setSent(Instant.now());
        }
    }
}
