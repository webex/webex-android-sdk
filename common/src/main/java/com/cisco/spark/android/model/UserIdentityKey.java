package com.cisco.spark.android.model;

public class UserIdentityKey {
    // private fields filled in by gson
    private String id;
    private boolean userExists;

    private String invitee;

    public UserIdentityKey(String id, boolean userExists) {
        this.id = id;
        this.userExists = userExists;
    }

    public String getId() {
        return id;
    }

    public boolean isUserExists() {
        return userExists;
    }

    public String getInvitee() {
        return invitee;
    }

    public void setInvitee(String invitee) {
        this.invitee = invitee;
    }
}
