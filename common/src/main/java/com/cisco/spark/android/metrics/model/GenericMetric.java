package com.cisco.spark.android.metrics.model;

import com.cisco.spark.android.metrics.value.ClientMetricField;
import com.cisco.spark.android.metrics.value.ClientMetricTag;
import com.cisco.spark.android.model.ErrorDetail;
import com.cisco.spark.android.util.MimeUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Response;

public class GenericMetric {
    private String metricName;
    private Map<String, Object> tags;
    private Map<String, Object> fields;

    public GenericMetric(String metricName) {
        this.metricName = metricName;
        this.tags = new HashMap<>();
        this.fields = new HashMap<>();
    }

    public void addTag(ClientMetricTag tag, Object tagValue) {
        this.tags.put(tag.getTagName(), tagValue);
    }

    public void addTags(Map<String, Object> tags) {
        this.tags.putAll(tags);
    }

    public void addField(ClientMetricField field, Object fieldValue) {
        this.fields.put(field.getFieldName(), fieldValue);
    }

    public void addFields(Map<String, Object> fields) {
        this.fields.putAll(fields);
    }

    public void addNetworkFields(Response response) throws IOException {
        if (response == null) {
            return;
        }

        addField(ClientMetricField.METRIC_FIELD_WAS_SUCCESSFUL, response.isSuccessful());

        if (!response.isSuccessful()) {
            addField(ClientMetricField.METRIC_FIELD_FAILURE_CODE, response.code());
            addField(ClientMetricField.METRIC_FIELD_FAILURE_REASON, (response.errorBody() != null) ? response.errorBody().string() : response.message());
        }
    }

    public void addNetworkFields(ErrorDetail error) throws IOException {
        if (error == null) {
            return;
        }

        addField(ClientMetricField.METRIC_FIELD_WAS_SUCCESSFUL, false);
        addField(ClientMetricField.METRIC_FIELD_FAILURE_CODE, error.getErrorCode());
        addField(ClientMetricField.METRIC_FIELD_FAILURE_REASON, error.getMessage());
    }

    public void addFileFields(File file) {
        addField(ClientMetricField.METRIC_FIELD_MIME_TYPE, MimeUtils.getMimeType(file.getAbsolutePath()));
        addField(ClientMetricField.METRIC_FIELD_FILE_SIZE, file.length());
    }

    public Object getTag(ClientMetricTag metricTag) {
        return this.tags.get(metricTag.getTagName());
    }

    public Object getField(ClientMetricField metricField) {
        return this.fields.get(metricField.getFieldName());
    }

    public String getMetricName() {
        return metricName;
    }

    public String toString() {
        return "metricName " + metricName;
    }
}
