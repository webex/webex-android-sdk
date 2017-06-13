package com.cisco.spark.android.sync;

import javax.inject.*;

@Singleton
public class ConversationSyncLock {
    private final Object syncLock = new Object();

    public Object getSyncLock() {
        return syncLock;
    }

    @Inject
    public ConversationSyncLock() {
    }
}
