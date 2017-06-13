package com.cisco.spark.android.model;

import android.net.Uri;

public class SymmetricJWK {
    private String k;
    private Uri kid;

    public SymmetricJWK(String octetSequenceKey, Uri kid) {
        this.k = octetSequenceKey;
        this.kid = kid;
    }

    public String getOctetSequenceKey() {
        return this.k;
    }

    public Uri getKId() {
        return this.kid;
    }
}
