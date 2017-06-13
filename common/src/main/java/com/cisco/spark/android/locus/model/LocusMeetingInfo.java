package com.cisco.spark.android.locus.model;


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

    public static class LocusMeetingInfoBuilder {

        private LocusMeetingInfo locusMeetingInfo;

        public LocusMeetingInfoBuilder(URI locusUrl) {
            locusMeetingInfo = new LocusMeetingInfo();
            locusMeetingInfo.locusUrl = locusUrl;
        }

        public LocusMeetingInfo build() {
            return locusMeetingInfo;
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
}
