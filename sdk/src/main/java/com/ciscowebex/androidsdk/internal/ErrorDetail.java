/*
 * Copyright 2016-2020 Cisco Systems Inc
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.ciscowebex.androidsdk.internal;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.SparseArray;
import me.helloworld.utils.Checker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ErrorDetail {
    public enum CustomErrorCode {
        Unknown(0),
        CantDeleteActivityType(1400005),
        UserNotPresentInCI(1400006),
        SideboardingExistingParticipant(1403001),
        SideboardingFailed(1403003),
        InvalidParticipantsSize(1403012),
        AlreadyAnnouncementSpace(1403014),
        NotAnnouncementSpace(1403015),
        NotModeratorInAnnouncementSpace(1403016),
        MissingEntitlement(1403021),
        AssignModeratorFailed(1403022),
        ActionNotAllowedIn1on1(1403023),
        RoomAlreadyLocked(1403024),
        UserNotModerator(1403025),
        RoomNotLocked(1403027),
        ClientTempIdAlreadyExists(1409001),
        KmsMessageOperationFailed(1900000),
        LocusInvalidLocusURL(2400002),
        LocusArgumentNullOrEmpty(2400003),
        LocusInvalidUser(2400004),
        LocusInvalidPhoneNumberOrCountryCode(2400005),
        LocusPhoneNumberInvalidCountryCode(2400007),
        LocusPhoneNumberNotANumber(2400008),
        LocusPhoneNumberTooShortAfterIdd(2400009),
        LocusPhoneNumberTooShortNSN(2400010),
        LocusPhoneNumberTooLong(2400011),
        LocusInvalidDialableKey(2400012),
        LocusInvalidMeetingLink(2400013),
        LocusInvalidSinceOrSequenceHashInRequest(2400014),
        LocusInvalidLocusId(2400015),
        LocusInvalidMeetingIdFormat(2400020),
        LocusInvalidSipUrlFormat(2400021),
        LocusEmptyInviteeRecord(2400022),
        LocusEmptyInviteeAddress(2400023),
        LocusInvalidInviteeAddress(2400025),
        LocusInvalidAttendeeId(2400026),
        LocusInvalidInLobby(2400027),
        LocusInvalidInvitee(2400032),
        LocusInvalidAddress(2400033),
        LocusNoInviteeOrAddress(2400033),
        LocusNotPartOfRoster(2401002),
        LocusExceededMaxNumberParticipantsFreeUser(2403001),
        LocusExceededMaxNumberParticipantsPaidUser(2403002),
        LocusExceededMaxNumberParticipantsTeamMember(2403003),
        LocusMeetingIsInactive(2403004),
        LocusOneOnOneToSelfNotAllowed(2403007),
        LocusUserIsNotAuthorized(2403010),
        LocusMeetingNotFound(2403014),
        LocusInvalidWebExSite(2403015),
        LocusPstnCallingNotEnabled(2403030),
        LocusUserNotAuthorizedForMedia(2403039),
        LocusMeetingIsMigrated(2409018),
        LocusExceededMaxRosterSizeFreePaidUser(2423001),
        LocusExceededMaxRosterSizeTeamMember(2423002),
        LocusMeetingIsLocked(2423003),
        LocusLockedWhileTerminatingPreviousMeeting(2423004),
        LocusRequiresModeratorPINOrGuest(2423005),
        LocusRequiresModeratorPINorGuestPIN(2423006),
        LocusPMRAccountSuspended(2423008),
        LocusMeetingNotStarted(2423010),
        LocusRequiresWebexLogin(2423011),
        LocusHostSessionLimitExceeded(2423012),
        NewUserInDirSycnedOrg(400110),
        CreatorFromBlockedOrg(7400901),
        UserFromBlockedOrg(7400902),
        OwnerBlockingOtherOrg(7400903),
        ParticipantFromBlockingOrg(7400904);

        private int errorCode;

        private static final SparseArray<ErrorDetail.CustomErrorCode> errorCodes = new SparseArray<>();

        static {
            for (ErrorDetail.CustomErrorCode errorCode : ErrorDetail.CustomErrorCode.values()) {
                errorCodes.put(errorCode.getErrorCode(), errorCode);
            }
        }

        CustomErrorCode(int errorCode) {
            this.errorCode = errorCode;
        }

        public int getErrorCode() {
            return errorCode;
        }

        @NonNull
        public static ErrorDetail.CustomErrorCode fromErrorCode(int errorCode) {
            ErrorDetail.CustomErrorCode code = errorCodes.get(errorCode);
            return code == null ? Unknown : code;
        }
    }

    @Nullable
    private Integer errorCode;
    private String message;
    @Nullable
    private List<ErrorDetail.ErrorDescription> errors;
    private String trackingId;
    private String stackTrace;

    public ErrorDetail(@Nullable Integer errorCode, String message, List errors, String trackingId, String stackTrace) {
        this.errorCode = errorCode;
        this.message = message;
        this.errors = errors;
        this.trackingId = trackingId;
        this.stackTrace = stackTrace;
    }

    public int getErrorCode() {
        return errorCode == null ? ErrorDetail.CustomErrorCode.Unknown.getErrorCode() : errorCode;
    }

    @NonNull
    public ErrorDetail.CustomErrorCode extractCustomErrorCode() {
        return ErrorDetail.CustomErrorCode.fromErrorCode(getErrorCode());
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

    @Nullable
    public List<ErrorDetail.ErrorDescription> getErrors() {
        return errors;
    }

    private static class ErrorDescription {
        private String errorCode;
        private String description;

        public String getErrorCode() {
            return errorCode;
        }

        public String getDescription() {
            return description;
        }
    }

    public HashMap<String, ArrayList<String>> extractBlockedParticipantIdsFromErrorDescription() {
        HashMap<String, ArrayList<String>> blockedParticipantsWithOrgIDMap = new HashMap<>();
        ArrayList<String> blockedParticipantIds = null;
        String orgID = null;
        for (ErrorDetail.ErrorDescription errorDescription : errors) {
            if (errorDescription != null && !Checker.isEmpty(errorDescription.description)) {
                List<String> orgIDWithParticipantIDList = convertStringToList(errorDescription.description);
                if (!Checker.isEmpty(orgIDWithParticipantIDList)) {
                    orgID = orgIDWithParticipantIDList.get(0);
                    if (!Checker.isEmpty(orgID)) {
                        if (!blockedParticipantsWithOrgIDMap.containsKey(orgID)) {
                            blockedParticipantIds = new ArrayList<>();
                            blockedParticipantIds.add(orgIDWithParticipantIDList.get(1));
                            blockedParticipantsWithOrgIDMap.put(orgID, blockedParticipantIds);
                        } else {
                            blockedParticipantIds = blockedParticipantsWithOrgIDMap.get(orgID);
                            blockedParticipantIds.add(orgIDWithParticipantIDList.get(1));
                        }
                    }
                }
            }
        }
        return blockedParticipantsWithOrgIDMap;
    }

    private static List<String> convertStringToList(String listAsString) {
        listAsString = listAsString.replaceAll("\\s+","");
        if (!Checker.isEmpty(listAsString)) {
            return new ArrayList(Arrays.asList(listAsString.split(",")));
        }
        return null;
    }
}
