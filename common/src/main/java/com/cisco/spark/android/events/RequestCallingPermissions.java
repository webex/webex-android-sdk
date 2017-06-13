package com.cisco.spark.android.events;

import com.cisco.spark.android.callcontrol.CallContext;

public class RequestCallingPermissions {
    private CallContext callContext;

    public RequestCallingPermissions(CallContext callContext) {
        this.callContext = callContext;
    }

    public CallContext getCallContext() {
        return callContext;
    }
}
