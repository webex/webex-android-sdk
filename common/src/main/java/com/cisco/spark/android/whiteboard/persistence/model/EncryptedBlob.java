package com.cisco.spark.android.whiteboard.persistence.model;

import android.net.Uri;

public class EncryptedBlob {

    private final Uri encryptionKeyUrl;
    private final String encryptedData;

    public EncryptedBlob(final Uri encryptionKeyUrl, final String encryptedData) {
        this.encryptionKeyUrl = encryptionKeyUrl;
        this.encryptedData = encryptedData;
    }

    public Uri getEncryptionKeyUrl() {
        return encryptionKeyUrl;
    }

    public String getEncryptedData() {
        return encryptedData;
    }
}
