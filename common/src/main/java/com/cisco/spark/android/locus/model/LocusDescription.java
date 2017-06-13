package com.cisco.spark.android.locus.model;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class LocusDescription {

    // Fields set by gson
    private Set<LocusTag> locusTags;
    private UUID owner;
    private String sipUri;

    public LocusDescription() {
    }

    public Set<LocusTag> getLocusTags() {
        if (locusTags != null) {
            return locusTags;
        } else {
            return new HashSet<>();
        }
    }

    public UUID getOwner() {
        return owner;
    }

    public String getSipUri() {
        return sipUri;
    }
}
