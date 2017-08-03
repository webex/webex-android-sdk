package com.cisco.spark.android.lyra.model;


import java.util.List;

public class LyraSpaceOccupants {
    private LyraSpaceOccupant self;
    private List<LyraSpaceOccupant> items;
    private Links links;

    public LyraSpaceOccupants(LyraSpaceOccupant self,
                              List<LyraSpaceOccupant> items,
                              Links links) {
        this.self = self;
        this.items = items;
        this.links = links;
    }

    public LyraSpaceOccupant getSelf() {
        return self;
    }

    public List<LyraSpaceOccupant> getItems() {
        return items;
    }

    public Links getLinks() {
        return links;
    }
}
