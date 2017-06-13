package com.cisco.spark.android.sdk;

import com.cisco.spark.android.sync.operationqueue.core.Operation;

public interface SdkClient {

    String getDeviceType();
    boolean toastsEnabled();
    boolean supportsReducedScopes();
    boolean operationEnabled(Operation op);
    boolean conversationCachingEnabled();

    boolean supportsPrivateBoards();
}
