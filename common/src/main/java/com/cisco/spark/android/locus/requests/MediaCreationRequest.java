package com.cisco.spark.android.locus.requests;

import android.net.Uri;

import com.cisco.spark.android.features.CoreFeatures;
import com.cisco.spark.android.locus.model.MediaConnection;


public class MediaCreationRequest extends DeltaRequest {
    private Uri deviceUrl;
    private MediaConnection localMedia;

    public MediaCreationRequest(CoreFeatures coreFeatures) {
        super(coreFeatures);
    }

    public Uri getDeviceUrl() {
        return deviceUrl;
    }

    public void setDeviceUrl(Uri deviceUrl) {
        this.deviceUrl = deviceUrl;
    }

    public void setLocalMedia(MediaConnection localMedia) {
        this.localMedia = localMedia;
    }
}
