package com.cisco.spark.android.locus.model;


import android.text.TextUtils;

import java.io.Serializable;
import java.net.URI;

public class LocusMeetingInfo implements Serializable {

    // Fields set by gson
    private URI locusUrl;
    private String webExMeetingLink;
    private String sipMeetingUri;
    private CallInNumbersInfo callInNumbersInfo;
    private String meetingNumber;
    private String owner;
    private String numericCode;
    private String uri;
    private PstnNumber localDialInNumber;
    private String meetingName;
    private boolean isPmr;
    private URI meetingLink;

    // Not set by gson
    private long lastUpdated;

    private LocusMeetingInfo() {
        // Use builder
    }

    public URI getLocusUrl() {
        return locusUrl;
    }

    public String getSipMeetingUri() {
        return sipMeetingUri;
    }

    public String getMeetingNumber() {
        return meetingNumber;
    }

    public String getWebExMeetingLink() {
        return webExMeetingLink;
    }

    public String getUri() {
        return uri;
    }

    public CallInNumbersInfo getCallInNumbersInfo() {
        return callInNumbersInfo;
    }

    public PstnNumber getLocalDialInNumber() {
        return localDialInNumber;
    }

    public String getNumericCode() {
        return numericCode;
    }

    public String getOwner() {
        return owner;
    }

    public String getMeetingName() {
        return meetingName;
    }

    public boolean isPmr() {
        return isPmr;
    }

    public URI getMeetingLink() {
        return meetingLink;
    }

    @Override
    public String toString() {
        return "LocusMeetingInfo{" +
                "locusUrl=" + locusUrl +
                '}';
    }

    public enum MeetingType {
        CLAIMED_PMR,
        UNCLAIMED_PMR,
        SPARK_SPACE,
        SCHEDULED_WEBEX,
        UNKNOWN
    }

    public static class LocusMeetingInfoBuilder {

        private LocusMeetingInfo locusMeetingInfo;

        public LocusMeetingInfoBuilder() {
            locusMeetingInfo = new LocusMeetingInfo();
        }

        public LocusMeetingInfo build() {
            return locusMeetingInfo;
        }

        public LocusMeetingInfoBuilder setLocusURI(URI locusURI) {
            locusMeetingInfo.locusUrl = locusURI;
            return this;
        }

        public LocusMeetingInfoBuilder setWebExMeetingLink(String webExMeetingLink) {
            locusMeetingInfo.webExMeetingLink = webExMeetingLink;
            return this;
        }

        public LocusMeetingInfoBuilder setSipMeetingUri(String sipMeetingUri) {
            locusMeetingInfo.sipMeetingUri = sipMeetingUri;
            return this;
        }

        public LocusMeetingInfoBuilder setCallInNumbersInfo(CallInNumbersInfo callInNumbersInfo) {
            locusMeetingInfo.callInNumbersInfo = callInNumbersInfo;
            return this;
        }

        public LocusMeetingInfoBuilder setMeetingNumber(String meetingNumber) {
            locusMeetingInfo.meetingNumber = meetingNumber;
            return this;
        }

        public LocusMeetingInfoBuilder setOwner(String owner) {
            locusMeetingInfo.owner = owner;
            return this;
        }

        public LocusMeetingInfoBuilder setNumericCode(String numericCode) {
            locusMeetingInfo.numericCode = numericCode;
            return this;
        }

        public LocusMeetingInfoBuilder setUri(String uri) {
            locusMeetingInfo.uri = uri;
            return this;
        }

        public LocusMeetingInfoBuilder setMeetingName(String meetingName) {
            locusMeetingInfo.meetingName = meetingName;
            return this;
        }

        public LocusMeetingInfoBuilder setLocalDialInNumber(PstnNumber number) {
            locusMeetingInfo.localDialInNumber = number;
            return this;
        }

        public LocusMeetingInfoBuilder setIsPMR(boolean isPMR) {
            locusMeetingInfo.isPmr = isPMR;
            return this;
        }

        public LocusMeetingInfoBuilder setMeetingLink(URI meetingLink) {
            locusMeetingInfo.meetingLink = meetingLink;
            return this;
        }

        public LocusMeetingInfoBuilder setLastUpdated(long lastUpdated) {
            locusMeetingInfo.lastUpdated = lastUpdated;
            return this;
        }
    }

    public LocusMeetingInfo(URI locusUrl, String sipMeetingUri, String meetingNumber,
                            String webExMeetingLink, String meetingName, String owner,
                            CallInNumbersInfo callInNumbersInfo, boolean isPMR, URI meetingLink) {
        super();
        this.locusUrl = locusUrl;
        this.webExMeetingLink = webExMeetingLink;
        this.sipMeetingUri = sipMeetingUri;
        this.callInNumbersInfo = callInNumbersInfo;
        this.meetingNumber = meetingNumber;
        this.owner = owner;
        this.meetingName = meetingName;
        this.isPmr = isPMR;
        this.meetingLink = meetingLink;
    }

    public void setCallInNumbersInfo(CallInNumbersInfo callInNumbersInfo) {
        this.callInNumbersInfo = callInNumbersInfo;
    }

    public void setMeetingLink(URI meetingLink) {
        this.meetingLink = meetingLink;
    }

    public void setMeetingName(String meetingName) {
        this.meetingName = meetingName;
    }

    public void setSipMeetingUri(String sipMeetingUri) {
        this.sipMeetingUri = sipMeetingUri;
    }

    public void setMeetingNumber(String meetingNumber) {
        this.meetingNumber = meetingNumber;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public MeetingType determineMeetingType(LocusData locusData) {
        // This type needs to be first, because it could be that we get a null locusMeetingInfo
        // returned but it is still this type
        if (locusData != null && locusData.getLocus().isSparkSpaceMeeting()) {
            return LocusMeetingInfo.MeetingType.SPARK_SPACE;
        }

        if (locusData == null) {
            return LocusMeetingInfo.MeetingType.UNKNOWN;
        } else if (isPmr()) {
            return locusData.getLocus().getInfo().getOwner() != null ? LocusMeetingInfo.MeetingType.CLAIMED_PMR : LocusMeetingInfo.MeetingType.UNCLAIMED_PMR;
        } else if (!isEmptyString(getWebExMeetingLink())) {
            return LocusMeetingInfo.MeetingType.SCHEDULED_WEBEX;
        } else if (!isEmptyString(getSipMeetingUri())) {
            // TODO: if it is a spark space, it should have been caught above, but isn't this also valid?
            return LocusMeetingInfo.MeetingType.SPARK_SPACE;
        } else {
            return LocusMeetingInfo.MeetingType.UNKNOWN;
        }
    }

    public static boolean isEmptyString(String value) {
        // A bug in CMR4 can cause empty fields in the LocusMeetingInfo DTO to actually be returned as
        // a String consisting of one or more whitespace characters. To workaround this until the bug
        // is fixed, all whitespace will be removed from the data and if the resulting string is not
        // empty, then the input string is considered OK.

        // detects NULL and empty string, then whether the string is comprised only of whitespace
        return TextUtils.isEmpty(value) || TextUtils.isEmpty(value.trim());
    }
}
