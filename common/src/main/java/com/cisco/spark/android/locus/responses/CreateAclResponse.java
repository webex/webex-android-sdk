package com.cisco.spark.android.locus.responses;

import com.cisco.spark.android.locus.model.Locus;

public class CreateAclResponse {

    private Locus locus;

    public CreateAclResponse(Locus locus) {
        this.locus = locus;
    }

    public Locus getLocus() {
        return locus;
    }
}
