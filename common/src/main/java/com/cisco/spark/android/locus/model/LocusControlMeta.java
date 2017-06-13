package com.cisco.spark.android.locus.model;


import java.util.Date;
import java.util.UUID;

public class LocusControlMeta  {
    private UUID modifiedBy;
    private Date lastModified;

    public UUID getModifiedBy() {
        return modifiedBy;
    }

    public Date getLastModified() {
        return lastModified;
    }
}
