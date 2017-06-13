package com.cisco.spark.android.metrics.value;

import java.util.Date;

public class CallRatingValue {

    private String deviceUrl;
    private String locusId;
    private Date locusTimestamp;
    private String participantId;
    private Integer rating;
    private Boolean declinedRating;
    private Boolean isPaired;
    private String roomUser;
    private String joinLocusTrackingID;

    private CallRatingValue(String deviceUrl, String locusId, Date locusTimestamp, String participantId, boolean wasRoomCall, String roomIdentifier, String locusTrackingId, int rating, boolean declinedRating) {
        this.deviceUrl = deviceUrl;
        this.locusId = locusId;
        this.locusTimestamp = locusTimestamp;
        this.participantId = participantId;
        this.isPaired = wasRoomCall;
        this.roomUser = roomIdentifier;
        this.joinLocusTrackingID = locusTrackingId;
        this.rating = (declinedRating) ? 0 : rating;
        this.declinedRating = declinedRating;
    }

    /** constructor to use when user provides a rating */
    public CallRatingValue(String deviceUrl, String locusId, Date locusTimestamp, String participantId, boolean wasRoomCall, String roomIdentifier, String locusTrackingId, int rating) {
        this(deviceUrl, locusId, locusTimestamp, participantId, wasRoomCall, roomIdentifier, locusTrackingId, rating, false);
    }

    /** constructor to use when user <b>did not</b> provide a rating */
    public CallRatingValue(String deviceUrl, String locusId, Date locusTimestamp, String participantId, boolean wasRoomCall, String roomIdentifier, String locusTrackingId) {
        this(deviceUrl, locusId, locusTimestamp, participantId, wasRoomCall, roomIdentifier, locusTrackingId, 0, true);
    }

    public Integer getRating() {
        return rating;
    }

    public Boolean wasPaired() {
        return isPaired;
    }

    public String getRoomUser() {
        return roomUser;
    }

}
