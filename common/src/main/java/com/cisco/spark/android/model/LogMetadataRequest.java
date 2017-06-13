package com.cisco.spark.android.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Transport object for associating metadata to log file uploaded to admin service
 *
 * @ see com.cisco.spark.android.client.AdminClient.setLogMetadata
 */
public class LogMetadataRequest {
    private String filename;
    private String userId;
    private List<NVPair> data;

    public LogMetadataRequest(String filename) {
        this.filename = filename;
    }

    public String getFilename() {
        return filename;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String value) {
        this.userId = value;
    }

    public List<NVPair> getData() {
        return data != null ? data : Collections.EMPTY_LIST;
    }

    public LogMetadataRequest addData(NVPair nv) {
        if (data == null) data = new ArrayList<NVPair>();
        data.add(nv);
        return this;
    }

    public LogMetadataRequest addData(String name, String value) {
        addData(new NVPair(name, value));
        return this;
    }

    public static class NVPair {
        private String key, value;

        public NVPair(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
