package com.cisco.spark.android.callcontrol;


import static com.cisco.spark.android.callcontrol.CallEndReason.CallEndReasonType.CANCELLED_BY_LOCAL_ERROR;
import static com.cisco.spark.android.callcontrol.CallEndReason.CallEndReasonType.UNKNOWN;

public class CallEndReason {

    public enum CallEndReasonType {
        UNKNOWN("unknown"),
        CANCELLED_BY_LOCAL_USER("cancelledbyLocalUser"),
        DIAL_TIMEOUT_REACHED("dialTimeoutReached"),
        DECLINED_BY_REMOTE_USER("declinedByRemoteUser"),
        CANCELLED_BY_LOCAL_ERROR("cancelledByLocalError"),
        ENDED_BY_LOCAL_USER("endedByLocalUser"),
        ENDED_BY_REMOTE_USER("endedByRemoteUser"),
        ENDED_BY_LOCUS("endedByLocus"),
        ENDED_BY_RECONNECT_TIMEOUT("endedByReconnectTimeout");

        private String type;

        CallEndReasonType(String type) {
            this.type = type;
        }
    }

    private CallEndReasonType type;
    private String error;

    public CallEndReason() {
        this(UNKNOWN);
    }

    public CallEndReason(CallEndReasonType type) {
        this(type, null);
    }

    public CallEndReason(CallEndReasonType type, String error) {
        this.type = type;
        if (type.equals(CANCELLED_BY_LOCAL_ERROR)) {
            if (error != null) {
                this.error = error;
            } else {
                throw new IllegalStateException("CallEndReasonType: " + type.type + " is missing the 'error' code");
            }
        } else if (error != null) {
            throw new IllegalArgumentException("Invalid 'error' argument specified for CallEndReasonType: '" + type.type + "'");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CallEndReason that = (CallEndReason) o;

        if (type != null ? !type.equals(that.type) : that.type != null) return false;
        if (error != null ? !error.equals(that.error) : that.error != null) return false;

        return true;
    }
}
