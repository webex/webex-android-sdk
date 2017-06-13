package com.cisco.spark.android.model;

public class CompleteContentUploadSession {
    private int size;

    public CompleteContentUploadSession(int size) {
        this.size = size;
    }

    public long getSize() {
        return size;
    }
}
