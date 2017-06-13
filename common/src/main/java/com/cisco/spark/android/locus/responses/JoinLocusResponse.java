package com.cisco.spark.android.locus.responses;

import com.cisco.spark.android.locus.model.Locus;



public class JoinLocusResponse {
    private Locus locus;

    public JoinLocusResponse(Locus locus) {
        this.locus = locus;
    }

    public Locus getLocus() {
        return locus;
    }
}
