package com.cisco.spark.android.whiteboard;

import com.cisco.spark.android.whiteboard.persistence.model.Channel;

import java.util.ArrayList;
import java.util.List;

public class WhiteboardListReadyEvent {

    private List<Channel> whiteboardList = new ArrayList<>();
    private String link;
    private boolean isLocalStore;
    private boolean isFirstPage;

    public WhiteboardListReadyEvent(List<Channel> whiteboardList, String link, boolean isLocalStore, boolean isFirstPage) {
        this.whiteboardList = whiteboardList;
        this.link = link;
        this.isLocalStore = isLocalStore;
        this.isFirstPage = isFirstPage;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public List<Channel> getWhiteboardList() {
        return whiteboardList;
    }

    public void setWhiteboardList(List<Channel> whiteboardList) {
        this.whiteboardList = whiteboardList;
    }

    public void setLocalStore(boolean localStore) {
        this.isLocalStore = localStore;
    }

    public boolean isLocalStore() {
        return isLocalStore;
    }

    public void setFirstPage(final boolean isFirstPage) {
        this.isFirstPage = isFirstPage;
    }

    public boolean isFirstPage() {
        return isFirstPage;
    }
}
