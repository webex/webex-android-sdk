package com.cisco.spark.android.model;

import android.net.Uri;

import com.cisco.spark.android.sync.ContentManager;

import java.util.Map;

public class GetAvatarUrlsResponse {

    private Map<String, Map<Integer, SingleUserAvatarUrlsInfo>> avatarUrlsMap;

    private Map<Uri, ContentManager.CacheRecordRequestParameters> parametersMap;

    public Map<String, Map<Integer, SingleUserAvatarUrlsInfo>> getAvatarUrlsMap() {
        return avatarUrlsMap;
    }

    public void setAvatarUrlsMap(Map<String, Map<Integer, SingleUserAvatarUrlsInfo>> avatarUrlsMap) {
        this.avatarUrlsMap = avatarUrlsMap;
    }

    public Map<Uri, ContentManager.CacheRecordRequestParameters> getParametersMap() {
        return parametersMap;
    }

    public void setParametersMap(Map<Uri, ContentManager.CacheRecordRequestParameters> parametersMap) {
        this.parametersMap = parametersMap;
    }
}
