package com.cisco.spark.android.callcontrol;


import android.os.Parcel;
import android.os.Parcelable;

import com.cisco.spark.android.locus.model.LocusKey;
import com.cisco.spark.android.media.MediaEngine;
import com.cisco.spark.android.media.MediaSession;

// https://code.google.com/p/android/issues/detail?id=6822
// Need to put this Parcelable in a bundle before passing it on to avoid the
// ClassNotFound exception warning in the logs
public class CallContext implements Parcelable {

    public enum UseRoomPreference {
        UseRoom,
        DontUseRoom,
        UseDefault
    };

    private LocusKey locusKey;
    private String invitee;
    private String usingResource;
    private String conversationId;
    private String conversationTitle;
    private boolean isOneOnOne;
    private boolean isAnsweringCall;
    private UseRoomPreference useRoomPreference;
    private boolean showFullScreen;
    private MediaEngine.MediaDirection mediaDirection;
    private CallInitiationOrigin callInitiationOrigin;
    private boolean promptLeaveRoom;
    private boolean moveMediaToResource;
    private boolean fromNotification;
    private boolean isAudioCall;
    private boolean isCrossLaunch;

    // These are not marshalled, should they be?
    private MediaSession mediaSession;
    private Boolean moderator;
    private String pin;

    protected CallContext(Parcel in) {
        locusKey = in.readParcelable(getClass().getClassLoader());
        invitee = in.readString();
        usingResource = in.readString();
        conversationId = in.readString();
        conversationTitle = in.readString();
        isOneOnOne = in.readByte() != 0;
        isAnsweringCall = in.readByte() != 0;
        showFullScreen = in.readByte() != 0;
        promptLeaveRoom = in.readByte() != 0;
        moveMediaToResource = in.readByte() != 0;
        fromNotification = in.readByte() != 0;
        isCrossLaunch = in.readByte() != 0;
        useRoomPreference = UseRoomPreference.values()[in.readInt()];
        isAudioCall = in.readByte() != 0;

        int callInitiationOriginInt = in.readInt();
        if (callInitiationOriginInt >= 0) {
            callInitiationOrigin = CallInitiationOrigin.values()[callInitiationOriginInt];
        } else {
            callInitiationOrigin = null;
        }

        int mediaDirectionInt = in.readInt();
        if (mediaDirectionInt >= 0) {
            mediaDirection = MediaEngine.MediaDirection.values()[mediaDirectionInt];
        } else {
            mediaDirection = null;
        }
    }

    public static final Creator<CallContext> CREATOR = new Creator<CallContext>() {
        @Override
        public CallContext createFromParcel(Parcel in) {
            return new CallContext(in);
        }

        @Override
        public CallContext[] newArray(int size) {
            return new CallContext[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(locusKey, flags);
        dest.writeString(invitee);
        dest.writeString(usingResource);
        dest.writeString(conversationId);
        dest.writeString(conversationTitle);
        dest.writeByte((byte) (isOneOnOne ? 1 : 0));
        dest.writeByte((byte) (isAnsweringCall ? 1 : 0));
        dest.writeByte((byte) (showFullScreen ? 1 : 0));
        dest.writeByte((byte) (promptLeaveRoom ? 1 : 0));
        dest.writeByte((byte) (moveMediaToResource ? 1 : 0));
        dest.writeByte((byte) (fromNotification ? 1 : 0));
        dest.writeByte((byte) (isCrossLaunch ? 1 : 0));
        dest.writeInt(useRoomPreference.ordinal());
        dest.writeByte((byte) (isAudioCall ? 1 : 0));
        if (callInitiationOrigin == null) {
            dest.writeInt(-1);
        } else {
            dest.writeInt(callInitiationOrigin.ordinal());
        }
        if (mediaDirection == null) {
            dest.writeInt(-1);
        } else {
            dest.writeInt(mediaDirection.ordinal());
        }
    }

    CallContext() {
        useRoomPreference = UseRoomPreference.UseDefault;
    }

    public LocusKey getLocusKey() {
        return locusKey;
    }

    public boolean isOneOnOne() {
        return isOneOnOne;
    }

    public String getInvitee() {
        return invitee;
    }

    public String getUsingResource() {
        return usingResource;
    }

    public void setUsingResource(String usingResource) {
        this.usingResource = usingResource;
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getConversationTitle() {
        return conversationTitle;
    }

    public boolean isAnsweringCall() {
        return isAnsweringCall;
    }

    public UseRoomPreference getUseRoomPreference() {
        return useRoomPreference;
    }

    public boolean isShowFullScreen() {
        return showFullScreen;
    }

    public MediaEngine.MediaDirection getMediaDirection() {
        return mediaDirection;
    }

    public CallInitiationOrigin getCallInitiationOrigin() {
        return callInitiationOrigin;
    }

    public boolean isPromptLeaveRoom() {
        return promptLeaveRoom;
    }

    public boolean isMoveMediaToResource() {
        return moveMediaToResource;
    }

    public boolean isFromNotification() {
        return fromNotification;
    }

    public boolean isCrossLaunch() {
        return isCrossLaunch;
    }

    public MediaSession getMediaSession() {
        return mediaSession;
    }

    public void setMediaSession(MediaSession mediaSession) {
        this.mediaSession = mediaSession;
    }

    public void setModerator(Boolean moderator) {
        this.moderator = moderator;
    }

    public Boolean getModerator() {
        return moderator;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public String getPin() {
        return pin;
    }

    public boolean isAudioCall() {
        return isAudioCall;
    }

    public void setAudioCall(boolean audioCall) {
        isAudioCall = audioCall;
    }

    public void setUseRoomPreference(UseRoomPreference useRoomPreference) {
        this.useRoomPreference = useRoomPreference;
    }

    public static class Builder {
        private CallContext callContext = new CallContext();

        public Builder(LocusKey locusKey) {
            callContext.locusKey = locusKey;
            callContext.isAudioCall = false;
            callContext.showFullScreen = true;
        }

        public Builder(LocusKey locusKey, String conversationId, String conversationTitle) {
            callContext.locusKey = locusKey;
            callContext.conversationId = conversationId;
            callContext.conversationTitle = conversationTitle;
            callContext.isAudioCall = false;
            callContext.showFullScreen = true;
        }

        public Builder(String invitee) {
            callContext.invitee = invitee;
            callContext.isOneOnOne = true;
            callContext.isAudioCall = false;
            callContext.showFullScreen = true;
        }

        public Builder setUsingResource(String usingResource) {
            callContext.usingResource = usingResource;
            return this;
        }

        public Builder setIsOneOnOne(boolean isOneOnOne) {
            callContext.isOneOnOne = isOneOnOne;
            return this;
        }

        public Builder setShowFullScreen(boolean showFullScreen) {
            callContext.showFullScreen = showFullScreen;
            return this;
        }

        public Builder setMediaDirection(MediaEngine.MediaDirection mediaDirection) {
            callContext.mediaDirection = mediaDirection;
            return this;
        }

        public Builder setIsAnsweringCall(boolean isAnsweringCall) {
            callContext.isAnsweringCall = isAnsweringCall;
            return this;
        }

        public Builder setUseRoomPreference(UseRoomPreference useRoomPreference) {
            callContext.useRoomPreference = useRoomPreference;
            return this;
        }


        public Builder setCallInitiationOrigin(CallInitiationOrigin callInitiationOrigin) {
            callContext.callInitiationOrigin = callInitiationOrigin;
            return this;
        }

        public Builder setPromptLeaveRoom(boolean promptLeaveRoom) {
            callContext.promptLeaveRoom = promptLeaveRoom;
            return this;
        }

        public Builder setMoveMediaToResource(boolean moveMediaToResource) {
            callContext.moveMediaToResource = moveMediaToResource;
            return this;
        }

        public Builder setFromNotification(boolean fromNotification) {
            callContext.fromNotification = fromNotification;
            return this;
        }

        public Builder setCrossLaunch(boolean isCrossLaunch) {
            callContext.isCrossLaunch = isCrossLaunch;
            return this;
        }

        public CallContext build() {
            return callContext;
        }
    }

    @Override
    public String toString() {
        return "CallContext{" +
                "locusKey=" + locusKey +
                ", invitee='" + invitee + '\'' +
                ", usingResource='" + usingResource + '\'' +
                ", conversationId='" + conversationId + '\'' +
                ", conversationTitle='" + conversationTitle + '\'' +
                ", isOneOnOne=" + isOneOnOne +
                ", isAnsweringCall=" + isAnsweringCall +
                ", useRoomPreference=" + useRoomPreference +
                ", showFullScreen=" + showFullScreen +
                ", mediaDirection=" + mediaDirection +
                ", callInitiationOrigin=" + callInitiationOrigin +
                ", promptLeaveRoom=" + promptLeaveRoom +
                ", moveMediaToResource=" + moveMediaToResource +
                ", fromNotification=" + fromNotification +
                ", isAudioCall=" + isAudioCall +
                ", isCrossLaunch=" + isCrossLaunch +
                ", mediaSession=" + mediaSession +
                ", moderator=" + moderator +
                ", pin='" + pin + '\'' +
                '}';
    }

}
