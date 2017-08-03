package com.cisco.spark.android.events;

import com.cisco.spark.android.whiteboard.persistence.model.Channel;

import java.util.List;

public class WhiteboardDeleteEvent {
    private boolean isAllSuccessful;
    private List<Channel> selectedChannels;
    private List<Channel> deletedChannels;

    public WhiteboardDeleteEvent(boolean isAllSuccessful, List<Channel> selectedChannels, List<Channel> deletedChannels) {
        this.isAllSuccessful = isAllSuccessful;
        this.selectedChannels = selectedChannels;
        this.deletedChannels = deletedChannels;
    }

    public boolean isAllSuccessful() {
        return isAllSuccessful;
    }

    public List<Channel> getSelectedChannels() {
        return selectedChannels;
    }

    public List<Channel> getDeletedChannels() {
        return deletedChannels;
    }
}
