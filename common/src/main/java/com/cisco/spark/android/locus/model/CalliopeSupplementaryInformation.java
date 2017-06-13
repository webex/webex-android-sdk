package com.cisco.spark.android.locus.model;

public class CalliopeSupplementaryInformation {
    private String infoType;
    private String infoParam;


    public CalliopeSupplementaryInformation(String infoType, String infoParam) {
        this.infoType = infoType;
        this.infoParam = infoParam;
    }

    public String getInfoType() {
        return infoType;
    }

    public void setInfoType(String infoType) {
        this.infoType = infoType;
    }

    public String getInfoParam() {
        return infoParam;
    }

    public void setInfoParam(String infoParam) {
        this.infoParam = infoParam;
    }

    @Override
    public String toString() {
        return "{" +
                "infoType='" + infoType + '\'' +
                ", infoParam='" + infoParam + '\'' +
                '}';
    }

}
