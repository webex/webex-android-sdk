package com.cisco.spark.android.contacts;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class SyncAdapterService extends Service {
    private static ConversationSyncAdapter sSyncAdapter = null;
    private static final Object lock = new Object();

    @Override
    public void onCreate() {
        synchronized (lock) {
            if (sSyncAdapter == null) {
                sSyncAdapter = new ConversationSyncAdapter(getApplicationContext(), true);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return sSyncAdapter.getSyncAdapterBinder();
    }
}
