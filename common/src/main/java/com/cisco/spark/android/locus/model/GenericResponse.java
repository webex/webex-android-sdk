package com.cisco.spark.android.locus.model;
// File copied from: https://sqbu-github.cisco.com/WebExSquared/cisco-spark-base/blob/25554a0af535b2db356948e78191f039880829c1/wx2-core/common/src/main/java/com/cisco/wx2/dto/GenericResponse.java

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.google.common.base.Preconditions;

public final class GenericResponse extends DataTransferObject {

    @JsonProperty("status")
    private final String status;

    @JsonCreator
    public GenericResponse(@JsonProperty("status") String status) {
        this.status = Preconditions.checkNotNull(status);
    }

    public final String getStatus() {
        return status;
    }
}
