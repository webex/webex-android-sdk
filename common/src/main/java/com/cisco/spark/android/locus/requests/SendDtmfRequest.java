package com.cisco.spark.android.locus.requests;

import android.net.Uri;

public class SendDtmfRequest {
    private Uri deviceUrl;
    private DtmfInfo dtmf;

    public SendDtmfRequest() { }

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
