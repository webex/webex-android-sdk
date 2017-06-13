package com.cisco.spark.android.model;

import java.util.Map;

public class GetAvatarUrlsResponse {

    private Map<String, Map<Integer, SingleAvatarInfo>> avatarUrlsMap;

    public Map<String, Map<Integer, SingleAvatarInfo>> getAvatarUrlsMap() {
        return avatarUrlsMap;
    }

    public void setAvatarUrlsMap(Map<String, Map<Integer, SingleAvatarInfo>> avatarUrlsMap) {
        this.avatarUrlsMap = avatarUrlsMap;
    }

    public static class SingleAvatarInfo {
        private int size;
        private String url;
        private boolean defaultAvatar;

        public int getSize() {
            return size;
        }

        public String getUrl() {
            return url;
        }

        public boolean isDefaultAvatar() {
            return defaultAvatar;
        }
    }
}
