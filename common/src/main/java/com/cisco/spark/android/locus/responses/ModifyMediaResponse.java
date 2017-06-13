package com.cisco.spark.android.locus.responses;

import com.cisco.spark.android.locus.model.Locus;

public class ModifyMediaResponse {

    private Locus locus;

    public ModifyMediaResponse() { }

    public ModifyMediaResponse(Locus locus) {
        this.locus = locus;
    }

    public Locus getLocus() {
        return locus;
    }
}
