package com.cisco.spark.android.room.model;

import com.cisco.spark.android.model.Actor;

import java.util.UUID;

public class RoomUser implements Actor {

    private String deviceUrl;
    private String userId;

    private UUID id;
    private String email;
    private String displayName;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    @Override
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getIdOrEmail() {
        return id == null ? email : id.toString();
    }

    public String getDeviceUrl() {
        return deviceUrl;
    }

    public void setDeviceUrl(String deviceUrl) {
        this.deviceUrl = deviceUrl;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public int hashCode() {
        int result = deviceUrl != null ? deviceUrl.hashCode() : 0;
        result = 31 * result + (userId != null ? userId.hashCode() : 0);
        result = 31 * result + (email != null ? email.hashCode() : 0);
        result = 31 * result + (displayName != null ? displayName.hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RoomUser roomUser = (RoomUser) o;

        if (deviceUrl != null ? !deviceUrl.equals(roomUser.deviceUrl) : roomUser.deviceUrl != null)
            return false;
        if (userId != null ? !userId.equals(roomUser.userId) : roomUser.userId != null)
            return false;
        if (email != null ? !email.equals(roomUser.email) : roomUser.email != null) return false;
        return displayName != null ? displayName.equals(roomUser.displayName) : roomUser.displayName == null;
    }
}
