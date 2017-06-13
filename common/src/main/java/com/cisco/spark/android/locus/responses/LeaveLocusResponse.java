package com.cisco.spark.android.locus.responses;

import com.cisco.spark.android.locus.model.Locus;

public class LeaveLocusResponse {

    private Locus locus;

    public LeaveLocusResponse() { }

    public LeaveLocusResponse(Locus locus) {
        this.locus = locus;
    }

    public Locus getLocus() {
        return locus;
    }
}
