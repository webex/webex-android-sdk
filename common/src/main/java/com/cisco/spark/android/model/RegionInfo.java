package com.cisco.spark.android.model;


public class RegionInfo {

    private String regionCode;
    private String countryCode;
    private String clientRegion;
    private String clientAddress;
    private String timezone;

    public String getRegionCode() {
        return regionCode;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public RegionInfo(String countryCode, String regionCode) {
        this.countryCode = countryCode;
        this.regionCode = regionCode;
    }

    public RegionInfo(String countryCode, String regionCode, String clientRegion, String clientAddress, String timezone) {
        this.countryCode = countryCode;
        this.regionCode = regionCode;
        this.clientRegion = clientRegion;
        this.clientAddress = clientAddress;
        this.timezone = timezone;
    }
}
