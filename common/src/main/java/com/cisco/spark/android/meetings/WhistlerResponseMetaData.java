package com.cisco.spark.android.meetings;

public class WhistlerResponseMetaData {

    // Fields set by gson
    private String hostPin;
    private String meetingId;
    private String domain;
    private String name;
    private String site;

    public String getHostPin() {
        return hostPin;
    }

    public String getMeetingId() {
        return meetingId;
    }

    public String getDomain() {
        return domain;
    }

    public String getName() {
        return name;
    }

    public String getSite() {
        return site;
    }

    @Override
    public String toString() {
        return "WhistlerResponseMetaData{" +
                "hostPin='" + hostPin + '\'' +
                ", meetingId='" + meetingId + '\'' +
                ", domain='" + domain + '\'' +
                ", name='" + name + '\'' +
                ", site='" + site + '\'' +
                '}';
    }
}
