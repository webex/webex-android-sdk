package com.ciscowebex.androidsdk.internal.model;

import java.util.List;

public class LocusListResponseModel {

    private List<LocusModel> loci;
    private List<String> remoteLocusClusterUrls;
    private List<String> locusUrls;

    public List<LocusModel> getLoci() {
        return loci;
    }

    public List<String> getRemoteLocusClusterUrls() {
        return remoteLocusClusterUrls;
    }

    public List<String> getLocusUrls() {
        return locusUrls;
    }
}
