package com.cisco.spark.android.model;


import android.net.Uri;

import com.cisco.spark.android.util.UriUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class RemoteSearchQueryRequest {
    private String query;
    private List<String> sharedBy;
    private List<String> sharedIn;
    private List<String> sharedWith;
    private List<String> types;
    private Date startDate;
    private Date endDate;
    private int size;
    private String searchEncryptionKeyUrl;
    private int limit;
    private String kmsMessage;

    public RemoteSearchQueryRequest(String query, Uri searchEncryptionKeyUrl) {
        this.query = query;
        this.searchEncryptionKeyUrl = UriUtils.toString(searchEncryptionKeyUrl);
    }

    public RemoteSearchQueryRequest(Uri searchEncryptionKeyUrl) {
        this.searchEncryptionKeyUrl = UriUtils.toString(searchEncryptionKeyUrl);
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public List<String> getSharedBy() {
        return sharedBy;
    }

    public void setSharedBy(String sharedBy) {
        this.sharedBy = Collections.singletonList(sharedBy);
    }

    public List<String> getSharedIn() {
        return sharedIn;
    }

    public void setSharedIn(String sharedIn) {
        this.sharedIn = Collections.singletonList(sharedIn);
    }

    public List<String> getTypes() {
        return types;
    }

    public void setTypes(List<String> types) {
        this.types = types;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public String getSearchEncryptionKeyUrl() {
        return searchEncryptionKeyUrl;
    }

    public void setSearchEncryptionKeyUrl(String searchEncryptionKeyUrl) {
        this.searchEncryptionKeyUrl = searchEncryptionKeyUrl;
    }

    public String getKmsMessage() {
        return kmsMessage;
    }

    public void setKmsMessage(String kmsMessage) {
        this.kmsMessage = kmsMessage;
    }

    public void setSharedWith(List<String> uuids) {
        this.sharedWith = uuids;
    }

    public static class Builder {
        RemoteSearchQueryRequest remoteSearchQueryRequest;

        public Builder(Uri searchEncryptionKeyUrl) {
            remoteSearchQueryRequest = new RemoteSearchQueryRequest(searchEncryptionKeyUrl);
        }

        public Builder setKmsMessage(String kmsMessage) {
            remoteSearchQueryRequest.kmsMessage = kmsMessage;
            return this;
        }

        public Builder setQuery(String query) {
            remoteSearchQueryRequest.query = query;
            return this;
        }

        public Builder searchForMessages(boolean searchForMessageType) {
            if (searchForMessageType) {
                if (remoteSearchQueryRequest.types == null) {
                    remoteSearchQueryRequest.types = new ArrayList<>();
                }
                remoteSearchQueryRequest.types.add("comment");
            }
            return this;
        }

        public Builder searchForFiles(boolean searchForContentType) {
            if (searchForContentType) {
                if (remoteSearchQueryRequest.types == null) {
                    remoteSearchQueryRequest.types = new ArrayList<>();
                }
                remoteSearchQueryRequest.types.add("content");
                remoteSearchQueryRequest.types.add("file");
            }
            return this;
        }

        public Builder setSharedBy(String uuid) {
            remoteSearchQueryRequest.setSharedBy(uuid);
            return this;
        }

        public Builder setSharedIn(String conversationId) {
            remoteSearchQueryRequest.setSharedIn(conversationId);
            return this;
        }

        public Builder setSharedWith(List<String> uuids) {
            remoteSearchQueryRequest.setSharedWith(uuids);
            return this;
        }

        public Builder setLimit(int limit) {
            remoteSearchQueryRequest.limit = limit;
            return this;
        }

        public RemoteSearchQueryRequest build() {
            return remoteSearchQueryRequest;
        }
    }

}
