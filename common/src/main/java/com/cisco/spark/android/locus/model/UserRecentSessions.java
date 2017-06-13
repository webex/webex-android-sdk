package com.cisco.spark.android.locus.model;

import java.util.List;


public class UserRecentSessions {

    private List<UserRecentSession> userSessions;

    public UserRecentSessions(List<UserRecentSession> userSessions) {
        this.userSessions = userSessions;
    }


    public List<UserRecentSession> getUserSessions() {
        return userSessions;
    }
}
