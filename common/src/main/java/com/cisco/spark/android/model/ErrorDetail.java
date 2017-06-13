package com.cisco.spark.android.model;

import android.util.SparseArray;

public class ErrorDetail {
    public enum CustomErrorCode {
        SideboardingExistingParticipant(1403001),
        SideboardingFailed(1403003),
        MissingEntitlement(1403021),
        AssignModeratorFailed(1403022),
        ActionNotAllowedIn1on1(1403023),
        RoomAlreadyLocked(1403024),
        ModeratorAction(1403025),
        KmsMessageOperationFailed(1900000),
        LocusNotPartOfRoster(2401002),
        LocusExceededMaxNumberParticipantsFreeUser(2403001),
        LocusExceededMaxNumberParticipantsPaidUser(2403002),
        LocusExceededMaxNumberParticipantsTeamMember(2403003),
        LocusExceededMaxRosterSizeFreePaidUser(2423001),
        LocusExceededMaxRosterSizeTeamMember(2423002),
        LocusMeetingIsLocked(2423003),
        LocusMeetingIsInactive(2403004),
        LocusLockedWhileTerminatingPreviousMeeting(2423004),
        LocusRequiresModeratorPINOrGuest(2423005),
        LocusRequiresModeratorPINorGuestPIN(2423006),
        LocusMeetingNotStarted(2423010),
        LocusMeetingNotFound(2403014),
        LocusInvalidWebExSite(2403015);

        private int errorCode;

        private static final SparseArray<CustomErrorCode> errorCodes = new SparseArray<CustomErrorCode>();

        static {
            for (CustomErrorCode errorCode : CustomErrorCode.values()) {
                errorCodes.put(errorCode.getErrorCode(), errorCode);
            }
        }

        private CustomErrorCode(int errorCode) {
            this.errorCode = errorCode;
        }

        public int getErrorCode() {
            return errorCode;
        }

        public static CustomErrorCode fromErrorCode(int errorCode) {
            return errorCodes.get(errorCode);
        }
    }

    private Integer errorCode;
    private String message;
    private String trackingId;
    private String stackTrace;

    public ErrorDetail(Integer errorCode, String message, String trackingId, String stackTrace) {
        this.errorCode = errorCode;
        this.message = message;
        this.trackingId = trackingId;
        this.stackTrace = stackTrace;
    }

    public Integer getErrorCode() {
        return errorCode;
    }

    public String getMessage() {
        return message;
    }

    public String getTrackingId() {
        return trackingId;
    }

    public String getStackTrace() {
        return stackTrace;
    }
}
