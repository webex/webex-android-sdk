package com.ciscowebex.androidsdk.internal.metric;

import static com.cisco.wx2.diagnostic_events.Error.Category;

import com.ciscowebex.androidsdk.internal.ErrorDetail;
import com.webex.wme.MediaConnection;
import com.webex.wme.WmeError;

// Follows the specification on https://sqbu-github.cisco.com/WebWxSquared/event-dictionary/wiki/Error-codes-for-metric-events
public enum CallAnalyzerErrorCode {

    UNKNOWN_CALL_FAILURE(1000, Type.CALL_INITIATION_FAILURE, Category.SIGNALING, true),
    LOCUS_RATE_LIMITED_INCOMING(1001, Type.CALL_INITIATION_FAILURE, Category.SIGNALING, true),
    LOCUS_RATE_LIMITED_OUTGOING(1002, Type.CALL_INITIATION_FAILURE, Category.SIGNALING, true),
    LOCUS_UNAVAILABLE(1003, Type.CALL_INITIATION_FAILURE, Category.SIGNALING, true),
    LOCUS_CONFLICT(1004, Type.CALL_INITIATION_FAILURE, Category.SIGNALING, true),
    TIMEOUT(1005, Type.CALL_INITIATION_FAILURE, Category.SIGNALING, true),
    LOCUS_INVALID_SEQUENCE_HASH(1006, Type.CALL_INITIATION_FAILURE, Category.SIGNALING, true),
    UPDATE_MEDIA_FAILED(1007, Type.CALL_INITIATION_FAILURE, Category.SIGNALING, true),
    FAILED_TO_CONNECT_MEDIA(2001, Type.MEDIA_CONNECTION_FAILURE, Category.SIGNALING, true),
    MEDIA_ENGINE_LOST(2002, Type.MEDIA_CONNECTION_FAILURE, Category.SIGNALING, true),
    MEDIA_CONNECTION_LOST(2003, Type.MEDIA_CONNECTION_FAILURE, Category.SIGNALING, true),
    ICE_FAILURE(2004, Type.MEDIA_CONNECTION_FAILURE, Category.SIGNALING, true),
    MEDIA_ENGINE_HANG(2005, Type.MEDIA_CONNECTION_FAILURE, Category.SIGNALING, true),
    ICE_SERVER_REJECTED(2006, Type.MEDIA_CONNECTION_FAILURE, Category.SIGNALING, true),
    CALL_FULL(3001, Type.EXPECTED_FAILURE, Category.EXPECTED, true),
    ROOM_TOO_LARGE(3002, Type.EXPECTED_FAILURE, Category.EXPECTED, true),
    CALL_FULL_ADD_GUEST(3003, Type.EXPECTED_FAILURE, Category.EXPECTED, false),
    GUEST_ALREADY_ADDED(3004, Type.EXPECTED_FAILURE, Category.EXPECTED, false),
    LOCUS_USER_NOT_AUTHORISED(3005, Type.EXPECTED_FAILURE, Category.EXPECTED, true),
    CLOUDBERRY_UNAVAILABLE(3006, Type.EXPECTED_FAILURE, Category.EXPECTED, true),
    ROOM_TOO_LARGE_FREE_ACCOUNT(3007, Type.EXPECTED_FAILURE, Category.EXPECTED, true),
    EXCEEDED_ACTIVE_MEETING_LIMIT(3008, Type.EXPECTED_FAILURE, Category.EXPECTED, true),
    // Media SCA error code, start
    STREAM_ERROR_INTERNAL(3009, Type.MEDIA_CONNECTION_FAILURE, Category.EXPECTED, false),
    STREAM_ERROR_INTERNAL_TEMP(3010, Type.MEDIA_CONNECTION_FAILURE, Category.EXPECTED, false),
    STREAM_ERROR_INVALID_REQUEST(3011, Type.MEDIA_CONNECTION_FAILURE, Category.EXPECTED, false),
    STREAM_ERROR_PERMANENTLY_UNAVAILABLE(3012, Type.MEDIA_CONNECTION_FAILURE, Category.EXPECTED, false),
    STREAM_ERROR_INSUFFICIENT_SRC(3013, Type.MEDIA_CONNECTION_FAILURE, Category.EXPECTED, false),
    STREAM_ERROR_NO_MEDIA(3014, Type.MEDIA_CONNECTION_FAILURE, Category.EXPECTED, false),
    STREAM_ERROR_WRONG_STREAM(3015, Type.MEDIA_CONNECTION_FAILURE, Category.EXPECTED, false),
    STREAM_ERROR_INSUFFICIENT_BANDWIDTH(3016, Type.MEDIA_CONNECTION_FAILURE, Category.EXPECTED, false),
    STREAM_ERROR_MUTE_WITH_AVATAR(3017, Type.MEDIA_CONNECTION_FAILURE, Category.EXPECTED, false),
    // Media Internal error code, end.
    // Media local error code, start
    STREAM_ERROR_VIDEO_CAMERA_FAIL(3020, Type.MEDIA_CONNECTION_FAILURE, Category.EXPECTED, false),
    STREAM_ERROR_VIDEO_CAMERA_NOT_AUTHORIZED(3021, Type.MEDIA_CONNECTION_FAILURE, Category.EXPECTED, false),
    STREAM_ERROR_VIDEO_CAMERA_NO_DEVICE(3022, Type.MEDIA_CONNECTION_FAILURE, Category.EXPECTED, false),
    STREAM_ERROR_VIDEO_CAMERA_OCCUPIED(3023, Type.MEDIA_CONNECTION_FAILURE, Category.EXPECTED, false),
    STREAM_ERROR_VIDEO_CAMERA_RUNTIME_DIE(3024, Type.MEDIA_CONNECTION_FAILURE, Category.EXPECTED, false),
    STREAM_ERROR_VIDEO_RENDERTHREAD_GL(3025, Type.MEDIA_CONNECTION_FAILURE, Category.EXPECTED, false),
    STREAM_ERROR_AUDIO_NO_CAPTURE_DEVICE(3026, Type.MEDIA_CONNECTION_FAILURE, Category.EXPECTED, false),
    STREAM_ERROR_AUDIO_START_CAPTURE_FAILED(3027, Type.MEDIA_CONNECTION_FAILURE, Category.EXPECTED, false),
    STREAM_ERROR_AUDIO_CANNOT_CAPTURE_FROM_DEVICE(3028, Type.MEDIA_CONNECTION_FAILURE, Category.EXPECTED, false),
    STREAM_ERROR_AUDIO_NO_PLAYBACK_DEVICE(3029, Type.MEDIA_CONNECTION_FAILURE, Category.EXPECTED, false),
    STREAM_ERROR_AUDIO_START_PLAYBACK_FAILED(3030, Type.MEDIA_CONNECTION_FAILURE, Category.EXPECTED, false),
    STREAM_ERROR_AUDIO_CANNOT_PLAY_TO_DEVICE(3031, Type.MEDIA_CONNECTION_FAILURE, Category.EXPECTED, false),
    STREAM_ERROR_AUDIO_CANNOT_USE_THIS_DEVICE(3032, Type.MEDIA_CONNECTION_FAILURE, Category.EXPECTED, false),
    STREAM_ERROR_AUDIO_AUDIO_SERVICE_RUN_OUT(3033, Type.MEDIA_CONNECTION_FAILURE, Category.EXPECTED, false),
    STREAM_ERROR_SCREEN_SHARE_CAPTURE_FAIL(3034, Type.MEDIA_CONNECTION_FAILURE, Category.EXPECTED, false),
    STREAM_ERROR_SCREEN_SHARE_CAPTURE_DISPLAY_PLUGOUT(3035, Type.MEDIA_CONNECTION_FAILURE, Category.EXPECTED, false),
    STREAM_ERROR_SCREEN_SHARE_CAPTURE_NO_APP_SOURCE(3036, Type.MEDIA_CONNECTION_FAILURE, Category.EXPECTED, false),
    // Media local error code, end.
    MEETING_INACTIVE(4001, Type.CALL_INITIATION_FAILURE, Category.EXPECTED, true),
    MEETING_LOCKED(4002, Type.CALL_INITIATION_FAILURE, Category.EXPECTED, true),
    MEETING_TERMINATING(4003, Type.CALL_INITIATION_FAILURE, Category.EXPECTED, true),
    MODERATOR_PIN_OR_GUEST_REQ(4004, Type.ACCESS_RIGHTS, Category.EXPECTED, false),
    MODERATOR_PIN_OR_GUEST_PIN_REQ(4005, Type.ACCESS_RIGHTS, Category.EXPECTED, false),
    MODERATOR_REQUIRED(4006, Type.ACCESS_RIGHTS, Category.EXPECTED, false),
    USER_NOT_MEMBER_OF_ROOM(4007, Type.ACCESS_RIGHTS, Category.EXPECTED, true),
    NEW_LOCUS_ERROR(4008, Type.CALL_INITIATION_FAILURE, Category.EXPECTED, true),
    NETWORK_UNAVAILABLE(4009, Type.CALL_INITIATION_FAILURE, Category.EXPECTED, true),
    MEETING_UNAVAILABLE(4010, Type.CALL_INITIATION_FAILURE, Category.EXPECTED, true),
    MEETING_ID_INVALID(4011, Type.EXPECTED_FAILURE, Category.EXPECTED, true),
    MEETING_SITE_INVALID(4012, Type.EXPECTED_FAILURE, Category.EXPECTED, true),
    LOCUS_INVALID_JOIN_TIME(4013, Type.EXPECTED_FAILURE, Category.EXPECTED, true),
    LOBBY_EXPIRED(4014, Type.EXPECTED_FAILURE, Category.EXPECTED, true),
    MEDIA_CONNECTION_LOST_PAIRED(4015, Type.MEDIA_CONNECTION_FAILURE, Category.EXPECTED, false),
    PHONE_NUMBER_NOT_A_NUMBER(4016, Type.EXPECTED_FAILURE, Category.EXPECTED, true),
    PHONE_NUMBER_TOO_LONG(4017, Type.EXPECTED_FAILURE, Category.EXPECTED, true),
    INVALID_DIALABLE_KEY(4018, Type.EXPECTED_FAILURE, Category.EXPECTED, true),
    ONE_ON_ONE_TO_SELF_NOT_ALLOWED(4019, Type.EXPECTED_FAILURE, Category.EXPECTED, true),
    REMOVED_PARTICIPANT(4020, Type.EXPECTED_FAILURE, Category.EXPECTED, true),
    MEETING_LINK_NOT_FOUND(4021, Type.EXPECTED_FAILURE, Category.EXPECTED, true),
    PHONE_NUMBER_TOO_SHORT_AFTER_IDD(4022, Type.EXPECTED_FAILURE, Category.EXPECTED, true),
    INVALID_INVITEE_ADDRESS(4023, Type.EXPECTED_FAILURE, Category.EXPECTED, true),
    PMR_USER_ACCOUNT_LOCKED_OUT(4024, Type.EXPECTED_FAILURE, Category.EXPECTED, true),
    GUEST_FORBIDDEN(4025, Type.EXPECTED_FAILURE, Category.EXPECTED, true),
    PMR_ACCOUNT_SUSPENDED(4026, Type.EXPECTED_FAILURE, Category.EXPECTED, true),
    EMPTY_PHONE_NUMBER_OR_COUNTRY_CODE(4027, Type.EXPECTED_FAILURE, Category.EXPECTED, true),
    CONVERSATION_NOT_FOUND(4028, Type.EXPECTED_FAILURE, Category.EXPECTED, true),
    START_RECORDING_FAILED(4029, Type.EXPECTED_FAILURE, Category.EXPECTED, true),
    RECORDING_STORAGE_FULL(4030, Type.EXPECTED_FAILURE, Category.EXPECTED, true),
    NOT_WEBEX_SITE(4031, Type.EXPECTED_FAILURE, Category.EXPECTED, true),
    CAMERA_PERMISSION_DENIED(4032, Type.EXPECTED_FAILURE, Category.EXPECTED, true),
    MICROPHONE_PERMISSION_DENIED(4029, Type.EXPECTED_FAILURE, Category.EXPECTED, true),
    ACTIVE_CALL_EXISTS(4029, Type.EXPECTED_FAILURE, Category.EXPECTED, true),
    SIP_CALLEE_BUSY(5000, Type.EXPECTED_FAILURE, Category.EXPECTED, true),
    SIP_CALLEE_NOT_FOUND(5001, Type.EXPECTED_FAILURE, Category.EXPECTED, true),
    PARTICIPANT_JOINED_MIGRATED_LOCUS(5002, Type.EXPECTED_FAILURE, Category.EXPECTED, true),
    PARTICIPANT_NOT_AUTHORIZED_FOR_MEDIA(5003, Type.EXPECTED_FAILURE, Category.EXPECTED, true);

    public enum Type {
        CALL_INITIATION_FAILURE,
        MEDIA_CONNECTION_FAILURE,
        EXPECTED_FAILURE,
        ACCESS_RIGHTS;
    }

    private final int errorCode;
    private final Type type;
    private final Category category;
    private final boolean fatal;

    CallAnalyzerErrorCode(int errorCode, Type type, Category category, boolean fatal) {
        this.errorCode = errorCode;
        this.type = type;
        this.category = category;
        this.fatal = fatal;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public boolean isFatal() {
        return fatal;
    }

    public Type getType() {
        return type;
    }

    public Category getCategory() {
        return category;
    }

    public static CallAnalyzerErrorCode fromMediaStatusCode(int mediaStatusCode) {
        MediaConnection.MediaStatus mediaStatus = MediaConnection.MediaStatus.of(mediaStatusCode);
        switch (mediaStatus) {
            case ERR_INTERNAL:
                return STREAM_ERROR_INTERNAL;
            case ERR_INTERNAL_TEMP:
                return STREAM_ERROR_INTERNAL_TEMP;
            case ERR_INVALID_REQUEST:
                return STREAM_ERROR_INVALID_REQUEST;
            case ERR_TEMP_UNAVAIL_NO_MEDIA:
                return STREAM_ERROR_NO_MEDIA;
            case ERR_TEMP_UNAVAIL_INSUFF_BW:
                return STREAM_ERROR_INSUFFICIENT_BANDWIDTH;
            case ERR_PERMANENTLY_UNAVAILABLE:
                return STREAM_ERROR_PERMANENTLY_UNAVAILABLE;
            case ERR_TEMP_UNAVAIL_INSUFF_SRC:
                return STREAM_ERROR_INSUFFICIENT_SRC;
            case ERR_TEMP_UNAVAIL_WRONG_STREAM:
                return STREAM_ERROR_WRONG_STREAM;
            default:
                return UNKNOWN_CALL_FAILURE;
        }
    }

    public static CallAnalyzerErrorCode fromMediaError(int mediaErrorCode) {
        if (mediaErrorCode == WmeError.E_VIDEO_CAMERA_FAIL) {
            return STREAM_ERROR_VIDEO_CAMERA_FAIL;
        } else if (mediaErrorCode == WmeError.E_VIDEO_CAMERA_NOT_AUTHORIZED) {
            return STREAM_ERROR_VIDEO_CAMERA_NOT_AUTHORIZED;
        } else if (mediaErrorCode == WmeError.E_VIDEO_CAMERA_NO_DEVICE) {
            return STREAM_ERROR_VIDEO_CAMERA_NO_DEVICE;
        } else if (mediaErrorCode == WmeError.E_VIDEO_CAMERA_OCCUPIED) {
            return STREAM_ERROR_VIDEO_CAMERA_OCCUPIED;
        } else if (mediaErrorCode == WmeError.E_VIDEO_CAMERA_RUNTIME_DIE) {
            return STREAM_ERROR_VIDEO_CAMERA_RUNTIME_DIE;
        } else if (mediaErrorCode == WmeError.E_VIDEO_RENDERTHREAD_GL) {
            return STREAM_ERROR_VIDEO_RENDERTHREAD_GL;
        } else if (mediaErrorCode == WmeError.E_AUDIO_NO_CAPTURE_DEVICE) {
            return STREAM_ERROR_AUDIO_NO_CAPTURE_DEVICE;
        } else if (mediaErrorCode == WmeError.E_AUDIO_START_CAPTURE_FAILED) {
            return STREAM_ERROR_AUDIO_START_CAPTURE_FAILED;
        } else if (mediaErrorCode == WmeError.E_AUDIO_CANNT_CAPTURE_FROM_DEVICE) {
            return STREAM_ERROR_AUDIO_CANNOT_CAPTURE_FROM_DEVICE;
        } else if (mediaErrorCode == WmeError.E_AUDIO_NO_PLAYBACK_DEVICE) {
            return STREAM_ERROR_AUDIO_NO_PLAYBACK_DEVICE;
        } else if (mediaErrorCode == WmeError.E_AUDIO_START_PLAYBACK_FAILED) {
            return STREAM_ERROR_AUDIO_START_PLAYBACK_FAILED;
        } else if (mediaErrorCode == WmeError.E_AUDIO_CANNT_PLAY_TO_DEVICE) {
            return STREAM_ERROR_AUDIO_CANNOT_PLAY_TO_DEVICE;
        } else if (mediaErrorCode == WmeError.E_AUDIO_CANNT_USE_THIS_DEVICE) {
            return STREAM_ERROR_AUDIO_CANNOT_USE_THIS_DEVICE;
        } else if (mediaErrorCode == WmeError.E_AUDIO_AUDIO_SERVICE_RUN_OUT) {
            return STREAM_ERROR_AUDIO_AUDIO_SERVICE_RUN_OUT;
        } else if (mediaErrorCode == WmeError.E_SCREEN_SHARE_CAPTURE_FAIL) {
            return STREAM_ERROR_SCREEN_SHARE_CAPTURE_FAIL;
        } else if (mediaErrorCode == WmeError.E_SCREEN_SHARE_CAPTURE_DISPLAY_PLUGOUT) {
            return STREAM_ERROR_SCREEN_SHARE_CAPTURE_DISPLAY_PLUGOUT;
        } else if (mediaErrorCode == WmeError.E_SCREEN_SHARE_CAPTURE_NO_APP_SOURCE) {
            return STREAM_ERROR_SCREEN_SHARE_CAPTURE_NO_APP_SOURCE;
        } else {
            return UNKNOWN_CALL_FAILURE;
        }
    }

    public static CallAnalyzerErrorCode fromErrorDetailCustomErrorCode(ErrorDetail.CustomErrorCode code) {
        switch (code) {
            case SideboardingExistingParticipant:
                return CALL_FULL;

            case LocusExceededMaxNumberParticipantsFreeUser:
                return ROOM_TOO_LARGE_FREE_ACCOUNT;

            case LocusExceededMaxRosterSizeFreePaidUser:
            case LocusExceededMaxNumberParticipantsPaidUser:
            case LocusExceededMaxRosterSizeTeamMember:
            case LocusExceededMaxNumberParticipantsTeamMember:
                return ROOM_TOO_LARGE;

            case LocusMeetingIsInactive:
                return MEETING_INACTIVE;

            case LocusUserIsNotAuthorized:
                return LOCUS_USER_NOT_AUTHORISED;

            case LocusLockedWhileTerminatingPreviousMeeting:
            case LocusMeetingIsLocked:
                return MEETING_LOCKED;

            case LocusRequiresModeratorPINOrGuest:
                return MODERATOR_PIN_OR_GUEST_REQ;

            case LocusRequiresModeratorPINorGuestPIN:
                return MODERATOR_PIN_OR_GUEST_PIN_REQ;

            case LocusMeetingNotStarted:
                return MEETING_INACTIVE;

            // not sure about these
            case LocusNotPartOfRoster:
            case MissingEntitlement:
                return USER_NOT_MEMBER_OF_ROOM;

            case LocusInvalidUser:
                return SIP_CALLEE_NOT_FOUND;

            case LocusInvalidLocusURL:
            case LocusArgumentNullOrEmpty:
            case LocusInvalidMeetingIdFormat:
            case LocusInvalidSipUrlFormat:
            case LocusInvalidLocusId:
                return MEETING_ID_INVALID;

            case LocusInvalidWebExSite:
                return NOT_WEBEX_SITE;

            case LocusInvalidSinceOrSequenceHashInRequest:
                return LOCUS_INVALID_SEQUENCE_HASH;

            case LocusInvalidInLobby:
                return LOBBY_EXPIRED;

            case LocusInvalidPhoneNumberOrCountryCode:
            case LocusPhoneNumberInvalidCountryCode:
            case LocusPhoneNumberNotANumber:
                return PHONE_NUMBER_NOT_A_NUMBER;

            case LocusPhoneNumberTooLong:
                return PHONE_NUMBER_TOO_LONG;

            case LocusInvalidDialableKey:
                return INVALID_DIALABLE_KEY;

            case LocusInvalidMeetingLink:
            case LocusMeetingNotFound:
                return MEETING_LINK_NOT_FOUND;

            case LocusPhoneNumberTooShortAfterIdd:
                return PHONE_NUMBER_TOO_SHORT_AFTER_IDD;

            case LocusEmptyInviteeRecord:
            case LocusEmptyInviteeAddress:
            case LocusInvalidAttendeeId:
            case LocusInvalidInviteeAddress:
            case LocusInvalidInvitee:
            case LocusInvalidAddress:
            case LocusNoInviteeOrAddress:
                return INVALID_INVITEE_ADDRESS;

            case LocusPhoneNumberTooShortNSN:
                return EMPTY_PHONE_NUMBER_OR_COUNTRY_CODE;

            case LocusOneOnOneToSelfNotAllowed:
                return ONE_ON_ONE_TO_SELF_NOT_ALLOWED;

            case LocusPMRAccountSuspended:
                return PMR_ACCOUNT_SUSPENDED;

            case LocusHostSessionLimitExceeded:
                return EXCEEDED_ACTIVE_MEETING_LIMIT;

            case LocusMeetingIsMigrated:
                return PARTICIPANT_JOINED_MIGRATED_LOCUS;

            case LocusUserNotAuthorizedForMedia:
                return PARTICIPANT_NOT_AUTHORIZED_FOR_MEDIA;

            case SideboardingFailed:
            case AssignModeratorFailed:
            case ActionNotAllowedIn1on1:
            case RoomAlreadyLocked:
            case UserNotModerator:
            case ClientTempIdAlreadyExists:
            case KmsMessageOperationFailed:
            case NewUserInDirSycnedOrg:
            case Unknown:
            default:
                return UNKNOWN_CALL_FAILURE;
        }
    }
}
