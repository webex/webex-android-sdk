package com.cisco.spark.android.events;

import android.graphics.Bitmap;

import java.util.UUID;

public class SnapshotEvent {

    private final UUID snapshotUUID;
    private final Bitmap bitmap;

    public SnapshotEvent(UUID u, Bitmap b) {
        snapshotUUID = u;
        bitmap = b;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public UUID getSnapshotUUID() {
        return snapshotUUID;
    }
}

