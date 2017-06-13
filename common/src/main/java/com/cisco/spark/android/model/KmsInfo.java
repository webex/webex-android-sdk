package com.cisco.spark.android.model;


import android.net.Uri;

import com.cisco.spark.android.util.UriUtils;

public class KmsInfo {

    private String kmsMachineAccountId;
    private String rsaPublicKey;
    private String kmsCluster;

    public String getKmsId() {
        return kmsMachineAccountId;
    }

    public String getRsaPublicKey() {
        return rsaPublicKey;
    }

    public Uri getKmsCluster() {
        return UriUtils.parseIfNotNull(kmsCluster);
    }
}
