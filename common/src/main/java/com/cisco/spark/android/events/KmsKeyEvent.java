package com.cisco.spark.android.events;

import com.cisco.spark.android.model.KeyObject;

import java.util.List;

public class KmsKeyEvent {
    private List<KeyObject> keys;

    public KmsKeyEvent(List<KeyObject> keys) {
        this.keys = keys;
    }

    public List<KeyObject> getKeys() {
        return this.keys;
    }
}
