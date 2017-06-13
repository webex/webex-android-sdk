package com.cisco.spark.android.model;

import android.database.Cursor;
import com.cisco.spark.android.sync.ConversationContract.*;
import com.google.gson.Gson;

public class SyncOperation {

    private String syncOperationId;
    private SyncOperationEntry.SyncState syncState;
    private int retries;
    private long startTime;
    private Activity activity;

    public SyncOperation(String syncOperationId, Activity activity) {
        this.syncOperationId = syncOperationId;
        this.syncState = SyncOperationEntry.SyncState.EXECUTING;
        this.retries = 0;
        this.startTime = System.currentTimeMillis();
        this.activity = activity;
    }

    public static SyncOperation fromCursor(Cursor cursor, Gson gson) {
        if (cursor == null || gson == null)
            return null;

        Activity a = (Activity) gson.fromJson(cursor.getString(SyncOperationEntry.DATA.ordinal()), Activity.class);
        SyncOperation syncOp = new SyncOperation(cursor.getString(SyncOperationEntry.SYNC_OPERATION_ID.ordinal()), a);
        int syncState = cursor.getInt(SyncOperationEntry.SYNC_STATE.ordinal());
        syncOp.setSyncState(SyncOperationEntry.SyncState.values()[syncState]);
        syncOp.setRetries(cursor.getInt(SyncOperationEntry.RETRIES.ordinal()));
        syncOp.setStartTime(cursor.getLong(SyncOperationEntry.START_TIME.ordinal()));
        return syncOp;
    }

    public String getSyncOperationId() {
        return syncOperationId;
    }

    public void setSyncOperationId(String syncOperationId) {
        this.syncOperationId = syncOperationId;
    }

    public SyncOperationEntry.SyncState getSyncState() {
        return syncState;
    }

    public void setSyncState(SyncOperationEntry.SyncState syncState) {
        this.syncState = syncState;
    }

    public int getRetries() {
        return retries;
    }

    public void setRetries(int retries) {
        this.retries = retries;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public Activity getActivity() {
        return activity;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }
}
