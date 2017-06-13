package com.cisco.spark.android.whiteboard.persistence.model;

import java.util.ArrayList;
import java.util.List;

public class ContentItems {

    private List<Content> items;

    public ContentItems() {
        items = new ArrayList<>();
    }

    public ContentItems(List<Content> items) {
        this.items = items;
    }

    public List<Content> getItems() {
        return items;
    }
}
