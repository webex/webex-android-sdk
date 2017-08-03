package com.cisco.spark.android.callcontrol.events;

import com.cisco.spark.android.callcontrol.CallControlService;
import com.cisco.spark.android.locus.model.LocusKey;

public class CallControlCallCancelledEvent {
    private final LocusKey locusKey;
    private final CallControlService.CancelReason reason;

    public CallControlCallCancelledEvent(LocusKey locusKey, CallControlService.CancelReason reason) {
        this.locusKey = locusKey;
        this.reason = reason;
    }

    public LocusKey getLocusKey() {
        return locusKey;
    }

    public CallControlService.CancelReason getReason() {
        return reason;
    }
}
