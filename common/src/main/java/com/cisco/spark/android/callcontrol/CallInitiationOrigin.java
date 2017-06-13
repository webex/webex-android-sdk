package com.cisco.spark.android.callcontrol;


public enum CallInitiationOrigin {
    CallOriginationUnknown("unknown"),
    CallOriginationRoom("Room"),
    CallOriginationRoomList("RoomList"),
    CallOriginationRecents("Recents"),
    CallOriginationPeopleTab("PeopleTab"),
    CallOriginationDialBox("DialBox"),
    CallOriginationDialBoxNumeric("DialBoxNumeric"),
    CallOriginationSearchResult("SearchResult"),
    CallOriginationMeetingsTab("MeetingsTab"),
    CallOriginationMeetingDetails("MeetingDetails"),
    CallOriginationMeetingSummary("MeetingSummary"),
    CallOriginationToast("Toast"),
    CallOriginationZTM("ZeroTouchMeeting"),
    CallOriginationBackdoor("Backdoor"),
    CallOriginationSettingsTab("SettingsPage"),
    CallOriginationConversationDetailsTab("RoomDetailsPage"),
    CallOriginationMiniProfile("MiniProfile"),
    CallOriginationBRIC("BringRoomIntoCall"),
    CallOriginationWSS("WirelessScreenShare"),
    CallOriginationCrossLaunch("CrossLaunch"),
    CallOriginationRoster("Roster");

    private String value;

    CallInitiationOrigin(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
};
