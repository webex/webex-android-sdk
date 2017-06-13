package com.cisco.spark.android.locus.model;

import android.net.Uri;

import java.util.List;

public class LocusControl {
    private Uri url;
    private LocusLockControl lock;
    private LocusRecordControl record;
    private List<LocusLink> links;

    public LocusControl(Uri url, LocusLockControl lock, LocusRecordControl record, List<LocusLink> links) {
        this.url = url;
        this.lock = lock;
        this.record = record;
        this.links = links;
    }

    public Uri getUrl() {
        return url;
    }

    public LocusLockControl getLock() {
        return lock;
    }

    public LocusRecordControl getRecord() {
        return record;
    }

    public List<LocusLink> getLinks() {
        return links;
    }
}
