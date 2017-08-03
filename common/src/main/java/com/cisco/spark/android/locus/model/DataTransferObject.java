package com.cisco.spark.android.locus.model;
// File copied from: https://sqbu-github.cisco.com/WebExSquared/cisco-spark-base/blob/25554a0af535b2db356948e78191f039880829c1/wx2-core/common/src/main/java/com/cisco/wx2/dto/DataTransferObject.java

public class DataTransferObject {
    public static GenericResponse genericResponse(String status) {
        return new GenericResponse(status);
    }

    public DataTransferObject() {
    }

    @Override
    public String toString() {
        return JsonUtil.toJson(this);
    }
}
