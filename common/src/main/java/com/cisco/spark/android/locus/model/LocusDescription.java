package com.cisco.spark.android.locus.model;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class LocusDescription {

    // Fields set by gson
    private Set<LocusTag> locusTags;
    private UUID owner;
    private String sipUri;
    private boolean isPmr;

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

    public boolean isPmr() {
        return isPmr;
    }

    public static class Builder {
        private LocusDescription description;

        public Builder() {
            description = new LocusDescription();
            description.locusTags = new HashSet<>();
        }

        public Builder setOwner(UUID owner) {
            description.owner = owner;
            return this;
        }

        public Builder setSipUri(String sipUri) {
            description.sipUri = sipUri;
            return this;
        }

        public Builder addLocusTag(LocusTag tag) {
            description.locusTags.add(tag);
            return this;
        }

        public LocusDescription build() {
            return description;
        }
    }
}
