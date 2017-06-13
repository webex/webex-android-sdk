package com.cisco.spark.android.presence;

public enum PresenceStatus {
    PRESENCE_STATUS_UNKNOWN("unknown"),
    PRESENCE_STATUS_INACTIVE("inactive"),
    PRESENCE_STATUS_ACTIVE("active"),
    PRESENCE_STATUS_DO_NOT_DISTURB("dnd"),
    PRESENCE_STATUS_PENDING("pending"),
    PRESENCE_STATUS_OOO("ooo");


    private final String status;

    private PresenceStatus(String status) {
        this.status = status;

    }

    public static PresenceStatus fromString(String type) {
        if (type != null) {
            for (PresenceStatus statusType : PresenceStatus.values()) {
                if (type.equalsIgnoreCase(statusType.status)) {
                    return statusType;
                }
            }
        }
        return PRESENCE_STATUS_UNKNOWN;
    }

    @Override
    public String toString() {
        return status;
    }
}
