package com.ciscowebex.androidsdk.conversation;

import java.util.ArrayList;
import java.util.List;

public class Participants {

    private List<Actor> items;

    Participants() {
        this.items = new ArrayList<Actor>();
    }

    public List<Actor> getItems() {
        return items;
    }

    public void setItems(List<Actor> items) {
        this.items = items;
    }

}
