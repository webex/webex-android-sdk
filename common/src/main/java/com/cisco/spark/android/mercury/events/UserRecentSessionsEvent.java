package com.cisco.spark.android.mercury.events;

import com.cisco.spark.android.locus.model.UserRecentSessions;
import com.cisco.spark.android.mercury.MercuryData;

public  class UserRecentSessionsEvent extends MercuryData {
    private UserRecentSessions userSessions;

    public UserRecentSessionsEvent() {
        super();
    }

    public UserRecentSessions getUserRecentSessions() {
        return userSessions;
    }
}
