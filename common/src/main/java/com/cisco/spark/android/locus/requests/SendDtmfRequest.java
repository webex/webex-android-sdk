package com.cisco.spark.android.locus.requests;

import android.net.Uri;

import com.cisco.spark.android.features.CoreFeatures;

public class SendDtmfRequest extends DeltaRequest {
    private Uri deviceUrl;
    private DtmfInfo dtmf;

    public SendDtmfRequest(CoreFeatures coreFeatures) {
        super(coreFeatures);
    }

    public Uri getDeviceUrl() {
        return deviceUrl;
    }
    public void setDeviceUrl(Uri deviceUrl) {
        this.deviceUrl = deviceUrl;
    }

    public void setDtmf(int correlationId, String tones) {
        dtmf = new DtmfInfo();
        dtmf.tones = tones;
        dtmf.correlationId = Integer.toString(correlationId);
    }

    public class DtmfInfo {
        public String correlationId;
        public String tones;
    }
}
