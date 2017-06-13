package com.cisco.spark.android.sync;

import com.cisco.wx2.sdk.kms.KmsRequest;

public class KmsMessageRequestObject {

    KmsMessageRequestType type;
    KmsRequest kmsRequest;

    public KmsMessageRequestObject(KmsMessageRequestType type, KmsRequest kmsRequest) {
        this.type = type;
        this.kmsRequest = kmsRequest;
    }
    public void setKmsRequest(KmsRequest request) {
        this.kmsRequest = request;
    }

    public KmsMessageRequestType getType() {
        return type;
    }

    public void setType(KmsMessageRequestType type) {
        this.type = type;
    }

    public KmsRequest getKmsRequest() {
        return kmsRequest;
    }

}
