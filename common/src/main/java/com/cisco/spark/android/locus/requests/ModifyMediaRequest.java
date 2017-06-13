package com.cisco.spark.android.locus.requests;

import android.net.*;
import com.cisco.spark.android.locus.model.MediaConnection;


import java.util.ArrayList;
import java.util.List;

public class ModifyMediaRequest {
    private Uri deviceUrl;
    private List<MediaConnection> localMedias;

    public Uri getDeviceUrl() {
        return deviceUrl;
    }

    public void setDeviceUrl(Uri deviceUrl) {
        this.deviceUrl = deviceUrl;
    }

    public List<MediaConnection> getLocalMedias() {
        return localMedias;
    }

    public void setLocalMedias(List<MediaConnection> localMedias) {
        this.localMedias = localMedias;
    }

    public void setLocalMedias(MediaConnection[] localMedias) {
        this.localMedias = new ArrayList<MediaConnection>(localMedias.length);
        for (MediaConnection mediaInfo : localMedias) {
            this.localMedias.add(mediaInfo);
        }
    }
}
