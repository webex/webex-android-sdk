package com.cisco.spark.android.events;

public class MeetingHubNavDrawerItemSelectedEvent {
    public final int itemId;
    public final int itemTitleId;
    public final String itemSubTitle;

    public MeetingHubNavDrawerItemSelectedEvent(Integer itemId,
                                                Integer itemTitleId,
                                                String itemSubTitle) {
        this.itemId = itemId;
        this.itemTitleId = itemTitleId;
        this.itemSubTitle = itemSubTitle;
    }

    public int getItemId() {
        return itemId;
    }

    public int getItemTitleId() {
        return itemTitleId;
    }

    public String getItemSubTitle() {
        return itemSubTitle;
    }
}
