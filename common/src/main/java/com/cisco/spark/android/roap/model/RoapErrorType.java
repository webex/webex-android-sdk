package com.cisco.spark.android.roap.model;



public enum RoapErrorType {
    NOMATCH("NOMATCH"),
    TIMEOUT("TIMEOUT"),
    REFUSED("REFUSED"),
    CONFLICT("CONFLICT"),
    DOUBLE_CONFLICT("DOUBLE_CONFLICT"),
    FAILED("FAILED"),
    INVALID_STATE("INVALID_STATE"),
    OUT_OF_ORDER("OUT_OF_ORDER"),
    RETRY("RETRY");

    private String name;

    private RoapErrorType(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
