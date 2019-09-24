package com.ciscowebex.androidsdk.space;

import com.google.gson.annotations.SerializedName;

public class SpaceMeeting {
    @SerializedName(value = "roomId", alternate = "spaceId")
    private String _spaceId;

    @SerializedName("meetingLink")
    private String _meetingLink;

    @SerializedName("sipAddress")
    private String _sipAddress;

    @SerializedName("meetingNumber")
    private String _meetingNumber;

    @SerializedName("callInTollFreeNumber")
    private String _callInTollFreeNumber;

    @SerializedName("callInTollNumber")
    private String _callInTollNumber;

    /**
     * A unique identifier for the space.
     * @return a unique identifier for the space.
     * @since 2.2.0
     */
    public String getSpaceId() {
        return _spaceId;
    }

    /**
     * The Webex meeting URL for the space.
     * @return the Webex meeting URL for the space.
     * @since 2.2.0
     */
    public String getMeetingLink() {
        return _meetingLink;
    }

    /**
     * The SIP address for the space.
     * @return the SIP address for the space.
     * @since 2.2.0
     */
    public String getSipAddress() {
        return _sipAddress;
    }

    /**
     * The Webex meeting number for the space.
     * @return the Webex meeting number for the space.
     * @since 2.2.0
     */
    public String getMeetingNumber() {
        return _meetingNumber;
    }

    /**
     * The toll-free PSTN number for the space.
     * @return the toll-free PSTN number for the space.
     * @since 2.2.0
     */
    public String getCallInTollFreeNumber() {
        return _callInTollFreeNumber;
    }

    /**
     * The toll (local) PSTN number for the space.
     * @return the toll (local) PSTN number for the space.
     * @since 2.2.0
     */
    public String getCallInTollNumber() {
        return _callInTollNumber;
    }
}