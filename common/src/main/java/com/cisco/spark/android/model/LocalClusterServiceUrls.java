package com.cisco.spark.android.model;

import android.net.Uri;

public class LocalClusterServiceUrls {
    private Uri mercuryApiServiceClusterUrl;
    private  Uri mercuryConnectionServiceClusterUrl;

    public Uri getMercuryApiServiceClusterUrl() {
        return mercuryApiServiceClusterUrl;
    }

    public void setMercuryApiServiceClusterUrl(Uri mercuryApiServiceClusterUrl) {
        this.mercuryApiServiceClusterUrl = mercuryApiServiceClusterUrl;
    }

    public Uri getMercuryConnectionServiceClusterUrl() {
        return mercuryConnectionServiceClusterUrl;
    }

    public void setMercuryConnectionServiceClusterUrl(Uri mercuryConnectionServiceClusterUrl) {
        this.mercuryConnectionServiceClusterUrl = mercuryConnectionServiceClusterUrl;
    }

    public LocalClusterServiceUrls(Uri apiServiceClusterUrl, Uri connectionServiceClusterUrl) {
        this.mercuryApiServiceClusterUrl = apiServiceClusterUrl;
        this.mercuryConnectionServiceClusterUrl = connectionServiceClusterUrl;
    }
}
