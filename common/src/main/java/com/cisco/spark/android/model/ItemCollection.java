package com.cisco.spark.android.model;

import java.util.ArrayList;
import java.util.List;

public class ItemCollection<T> {
    private List<T> items = new ArrayList<T>();

    public ItemCollection() {
    }

    public ItemCollection(List<T> items) {
        setItems(items);
    }

    public List<T> getItems() {
        return items;
    }

    public void setItems(List<T> items) {
        this.items = items;
    }

    public void addItem(T item) {
        items.add(item);
    }

    public void addItems(List<T> items) {
        this.items.addAll(items);
    }

    public int size() {
        return items.size();
    }

    public void clear() {
        items.clear();
    }
}
