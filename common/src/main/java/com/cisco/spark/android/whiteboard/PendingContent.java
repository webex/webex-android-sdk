package com.cisco.spark.android.whiteboard;

import com.cisco.spark.android.whiteboard.persistence.model.Content;

import java.util.List;

class PendingContent {
    private List<Content> contents;
    private String id;

    public PendingContent(List<Content> contents, String id) {
        this.contents = contents;
        this.id = id;
    }

    public List<Content> getContents() {
        return contents;
    }

    public String getId() {
        return id;
    }
}
