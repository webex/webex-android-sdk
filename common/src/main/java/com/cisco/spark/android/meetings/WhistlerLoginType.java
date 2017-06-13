package com.cisco.spark.android.meetings;


public enum WhistlerLoginType {
    LOGIN_NONE("loginNone"),
    LOGIN_GUEST("loginGuest"),
    LOGIN_HOST("loginHost");

    private final String text;

    WhistlerLoginType(String text) {
        this.text = text;
    }

    public static WhistlerLoginType fromString(String type) {
        if (type != null) {
            for (WhistlerLoginType eventType : WhistlerLoginType.values()) {
                if (type.equalsIgnoreCase(eventType.text)) {
                    return eventType;
                }
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return text;
    }
}
