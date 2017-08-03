package com.cisco.spark.android.sdk;

import android.support.annotation.NonNull;

import com.cisco.spark.android.core.Component;
import com.cisco.spark.android.sync.operationqueue.core.Operation;

public interface SdkClient {

    String getDeviceType();
    boolean toastsEnabled();

    boolean supportsHybridKms();
    boolean supportsVoicemailScopes();

    boolean operationEnabled(Operation op);
    boolean componentEnabled(Component component);

    boolean conversationCachingEnabled();

    boolean supportsPrivateBoards();
    boolean shouldClearRemoteBoardStore();
    boolean isMobileDevice(); // Is a mobile/portable device or is a fixed endpoint always-powered on device

    @NonNull
    String generateClientInfo();
}
