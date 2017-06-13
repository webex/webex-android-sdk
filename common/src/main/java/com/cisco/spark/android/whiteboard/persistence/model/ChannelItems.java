package com.cisco.spark.android.whiteboard.persistence.model;

import java.util.ArrayList;
import java.util.List;

public class ChannelItems<E extends Channel> {

    private List<E> items;

    public ChannelItems() {
        items = new ArrayList<>();
    }

    public List<E> getItems() {
        return items;
    }
}
