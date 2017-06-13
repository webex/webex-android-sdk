package com.cisco.spark.android.model;

import com.google.gson.annotations.SerializedName;

public class ParticipantRoomProperties {
    @SerializedName("isModerator")
    private boolean moderator;

    public ParticipantRoomProperties() {

    }

    public boolean isModerator() {
        return moderator;
    }

    public void setModerator(boolean value) {
        this.moderator = value;
    }
}
