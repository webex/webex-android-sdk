package com.cisco.spark.android.locus.responses;

import com.cisco.spark.android.locus.model.Locus;

import java.util.ArrayList;
import java.util.List;

public class GetLocusListResponse {

    private List<Locus> loci;

    public GetLocusListResponse() {
        loci = new ArrayList<>();
    }

    public List<Locus> getLoci() {
        return loci;
    }
}
