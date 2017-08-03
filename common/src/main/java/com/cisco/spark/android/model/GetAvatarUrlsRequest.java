package com.cisco.spark.android.model;

import com.cisco.spark.android.core.AvatarProvider;

import java.util.ArrayList;
import java.util.List;

public class GetAvatarUrlsRequest {

    private ItemCollection<SingleAvatarUrlRequestInfo> avatarsList = new ItemCollection<>();

    public GetAvatarUrlsRequest(String uuidOrEmail) {
        this.avatarsList.addItem(new SingleAvatarUrlRequestInfo(uuidOrEmail));
    }

    public ItemCollection<SingleAvatarUrlRequestInfo> getAvatarsList() {
        return avatarsList;
    }

    public static class SingleAvatarUrlRequestInfo {
        private String uuid;
        private List<Long> sizes;

        public SingleAvatarUrlRequestInfo(String uuid) {
            this.uuid = uuid;
            this.sizes = new ArrayList<>();
            this.sizes.add(AvatarProvider.AvatarSize.BIG.getSize());
            this.sizes.add(AvatarProvider.AvatarSize.MEDIUM.getSize());
            this.sizes.add(AvatarProvider.AvatarSize.SMALL.getSize());
            this.sizes.add(AvatarProvider.AvatarSize.TINY.getSize());
        }

        public String getUuid() {
            return uuid;
        }

        public List<Long> getSizes() {
            return sizes;
        }
    }
}
