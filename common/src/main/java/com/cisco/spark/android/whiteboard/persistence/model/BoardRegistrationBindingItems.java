package com.cisco.spark.android.whiteboard.persistence.model;

import java.util.ArrayList;
import java.util.List;

public class BoardRegistrationBindingItems {
    private List<BoardRegistrationBinding> items;

    public BoardRegistrationBindingItems() {
        items = new ArrayList<>();
    }

    public BoardRegistrationBindingItems(List<BoardRegistrationBinding> items) {
        this.items = items;
    }

    public List<BoardRegistrationBinding> getItems() {
        return items;
    }
}
