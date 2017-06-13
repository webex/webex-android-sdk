package com.cisco.spark.android.mercury.events;

import com.cisco.spark.android.locus.model.Locus;
import com.cisco.spark.android.locus.model.LocusKey;
import com.cisco.spark.android.mercury.MercuryData;
import com.google.gson.annotations.SerializedName;

import java.util.UUID;

public abstract class LocusEvent extends MercuryData {
    // Fields set by gson
    private UUID id;
    @SerializedName("locusUrl")
    private LocusKey locusKey;
    private Locus locus;

    public LocusEvent() {
        super();
    }

    public UUID getId() {
        return id;
    }

    public LocusKey getLocusKey() {
        return locusKey;
    }

    public Locus getLocus() {
        return locus;
    }
}
