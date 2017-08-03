package com.cisco.spark.android.locus.responses;

import com.cisco.spark.android.locus.model.Locus;

public class UpdateLocusResponse {
    private Locus locus;

    public UpdateLocusResponse(Locus locus) {
        this.locus = locus;
    }

    public Locus getLocus() {
        return locus;
    }
}
