package com.cisco.spark.android.model;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.cisco.spark.android.core.AuthenticatedUser;
import com.cisco.spark.android.mercury.AlertType;
import com.cisco.spark.android.sync.ActorRecord;
import com.cisco.spark.android.sync.CallSession;
import com.cisco.spark.android.sync.ConversationContract;
import com.cisco.spark.android.sync.ConversationKeyUpdate;
import com.cisco.spark.android.sync.DisplayableFileSet;
import com.cisco.spark.android.sync.ImageURI;
import com.cisco.spark.android.sync.EventUpdate;
import com.cisco.spark.android.sync.Message;
import com.cisco.spark.android.sync.NewTeamConversation;
import com.cisco.spark.android.sync.ParticipantUpdate;
import com.cisco.spark.android.sync.RoomAvatarAssignment;
import com.cisco.spark.android.sync.TeamColorUpdate;
import com.cisco.spark.android.sync.TitleUpdate;
import com.cisco.spark.android.sync.Tombstone;
import com.cisco.spark.android.util.LocationManager;
import com.github.benoitdion.ln.Ln;
import com.google.gson.Gson;

import java.io.IOException;
import java.text.ParseException;
import java.util.Comparator;
import java.util.Date;

public class Activity extends ActivityObject {

    // These are used by gson through reflection
    private ActivityObject target;
    private ActivityObject object;
    private Person actor;
    private Provider provider;
    private String verb;
    private Place location;
    private Uri encryptionKeyUrl;
    private transient boolean isEncrypted;
    private transient AlertType alertType = AlertType.NONE;

    private transient ConversationContract.ActivityEntry.Type activityType = ConversationContract.ActivityEntry.Type._UNKNOWN;
    private transient ConversationContract.ActivityEntry.Source source = ConversationContract.ActivityEntry.Source.SYNC;

    public static final Comparator<Activity> ASCENDING_PUBLISH_TIME_COMPARATOR = new Comparator<Activity>() {

        @Override
        public int compare(Activity rhs, Activity lhs) {
            // Sort by publish time ascending
            int result = rhs.getPublished().compareTo(lhs.getPublished());
            if (result == 0 && lhs.isAcknowledgeActivity()) {
                return -1;
            } else if (result == 0 && rhs.isAcknowledgeActivity()) {
                return 1;
            }
            return result;
        }
    };

    public static final Comparator<Activity> DESCENDING_PUBLISH_TIME_COMPARATOR = new Comparator<Activity>() {

        @Override
        public int compare(Activity rhs, Activity lhs) {
            // just reverse the params
            return ASCENDING_PUBLISH_TIME_COMPARATOR.compare(lhs, rhs);
        }
    };

    public ConversationContract.ActivityEntry.Source getSource() {
        return source;
    }

    public void setSource(ConversationContract.ActivityEntry.Source source) {
        this.source = source;
    }

    public static Activity create(Person actor) {
        return new Activity(Verb.create, actor, null);
    }

    public static Activity add(Person actor, ActivityObject object, ActivityObject target, String encryptedBlob) {
        Activity activity = add(actor, object, target);
        activity.setEncryptedKmsMessage(encryptedBlob);
        return activity;
    }

    public static Activity add(Person actor, ActivityObject object, ActivityObject target) {
        return new Activity(Verb.add, actor, object, target);
    }

    public static Activity post(Person actor, ActivityObject object, ActivityObject target, String clientTempId, LocationManager locationManager, Uri encryptionKeyUrl) {
        Activity activity = new Activity(Verb.post, actor, object, target);
        activity.setClientTempId(clientTempId);
        if (locationManager != null && locationManager.isEnabled()) {
            Place location = new Place(locationManager.getCoarseLocationName(), locationManager.getCoarseLocationISO6709Position());
            activity.setLocation(location);
        }
        activity.setEncryptionKeyUrl(encryptionKeyUrl);
        return activity;
    }

    public static Activity delete(Person actor, ActivityObject object, ActivityObject target, String clientTempId) {
        Activity activity = new Activity(Verb.delete, actor, object, target);
        activity.setClientTempId(clientTempId);
        return activity;
    }

    public static Activity lock(Person actor, ActivityObject object, String clientTempId) {
        Activity activity = new Activity(Verb.lock, actor, object, null);
        activity.setClientTempId(clientTempId);

        return activity;
    }

    public static Activity acknowledge(Person actor, ActivityObject object, ActivityObject target) {
        return new Activity(Verb.acknowledge, actor, object, target);
    }

    public static Activity update(Person actor, ActivityObject object, ActivityObject target) {
        return new Activity(Verb.update, actor, object, target);
    }

    public static Activity updateKey(Person actor, ActivityObject object, ActivityObject target) {
        return new Activity(Verb.updateKey, actor, object, target);
    }

    public static Activity leave(Person person, String conversationId, String encryptedBlob) {
        Activity leaveActivity = leave(person, conversationId);
        leaveActivity.setEncryptedKmsMessage(encryptedBlob);
        return leaveActivity;
    }

    public static Activity leave(Person person, String conversationId) {
        return new Activity(Verb.leave, null, person, new Conversation(conversationId));
    }

    /**
     * Note in the UI this is referred to as leaving. 1:1 can not be left so they are hidden until
     * someone sends you a new message. Then it re-appears.
     */
    public static Activity hide(Person person, String conversartionId) {
        return new Activity(Verb.hide, null, person, new Conversation(conversartionId));
    }

    public static Activity mute(String conversationId) {
        return new Activity(Verb.mute, null, new Conversation(conversationId), null);
    }

    public static Activity unmute(String conversationId) {
        return new Activity(Verb.unmute, null, new Conversation(conversationId), null);
    }

    public static Activity favorite(String conversationId) {
        return new Activity(Verb.favorite, null, new Conversation(conversationId), null);
    }

    public static Activity unfavorite(String conversationId) {
        return new Activity(Verb.unfavorite, null, new Conversation(conversationId), null);
    }

    public static Activity share(Person actor, ActivityObject object, ActivityObject target, String clientTempId) {
        Activity activity = new Activity(Verb.share, actor, object, target);
        activity.setClientTempId(clientTempId);
        return activity;
    }

    public Activity() {
        super(ObjectType.activity);
    }

    public Activity(String verb) {
        this();
        this.verb = verb;
    }

    public Activity(String verb, Person actor, ActivityObject object) {
        this(verb, actor, object, null);
    }

    public Activity(String verb, Person actor, ActivityObject object, ActivityObject target) {
        this(null, verb, actor, object, target);
    }

    public Activity(String id, String verb, Person actor, ActivityObject object, ActivityObject target) {
        this(verb);
        setId(id);
        this.actor = actor;
        this.object = object;
        this.target = target;
    }

    public ActivityObject getTarget() {
        return target;
    }

    public void setTarget(ActivityObject target) {
        this.target = target;
    }

    public ActivityObject getObject() {
        return object;
    }

    public void setObject(ActivityObject object) {
        this.object = object;
    }

    public Person getActor() {
        return actor;
    }

    public void setActor(Person actor) {
        this.actor = actor;
    }

    public String getVerb() {
        return verb;
    }

    public void setVerb(String verb) {
        this.verb = verb;
    }

    public Provider getProvider() {
        return provider;
    }

    public void setProvider(Provider provider) {
        this.provider = provider;
    }

    public Place getLocation() {
        return location;
    }

    public void setLocation(Place location) {
        this.location = location;
    }

    public Uri getEncryptionKeyUrl() {
        return encryptionKeyUrl;
    }

    public void setEncryptionKeyUrl(Uri encryptionKeyUrl) {
        this.encryptionKeyUrl = encryptionKeyUrl;
    }

    @Override
    public Activity setPublished(Date published) {
        super.setPublished(published);
        return this;
    }

    @Override
    public Activity setId(String id) {
        super.setId(id);
        return this;
    }

    public boolean isAckable(AuthenticatedUser self) {
        if (isFromSelf(self)) {
            return false;
        }
        return isTypeAckable(getType());
    }

    public static boolean isTypeAckable(ConversationContract.ActivityEntry.Type type) {
        switch (type) {
            case MESSAGE:
            case ADD_PARTICIPANT:
            case CREATE_CONVERSATION:
            case UPDATE_TITLE_AND_SUMMARY:
            case ASSIGN_ROOM_AVATAR:
            case REMOVE_ROOM_AVATAR:
            case LEFT_CONVERSATION:
            case PHOTO:
            case FILE:
            case SCHEDULED_SYNCUP:
            case CALL_SESSION:
            case TOMBSTONE:
            case IMAGE_URI:
            case NEW_TEAM_CONVERSATION:
            case WHITEBOARD:
                return true;
        }
        return false;
    }

    // Determines whether the activity will be displayed in the Room list as a subheading
    public boolean shouldBecomeLastActivityPreview(AuthenticatedUser self) {
        return shouldBecomeLastActivityPreview(getType()) || (getType() == ConversationContract.ActivityEntry.Type.ADD_PARTICIPANT && isObjectSelf(self));
    }

    public static boolean shouldBecomeLastActivityPreview(ConversationContract.ActivityEntry.Type type) {
        switch (type) {
            case PHOTO:
            case MESSAGE:
            case FILE:
            case CREATE_CONVERSATION:
            case CALL_SESSION:
            case SCHEDULED_SYNCUP:
            case IMAGE_URI:
            case WHITEBOARD:
                return true;
        }
        return false;
    }

    // Determines whether the activity should affect the room sorting order
    public boolean shouldAffectRoomSortingOrder(AuthenticatedUser self) {
        ConversationContract.ActivityEntry.Type type = getType();

        switch (type) {
            case PHOTO:
            case MESSAGE:
            case FILE:
            case CREATE_CONVERSATION:
            case SCHEDULED_SYNCUP:
            case IMAGE_URI:
            case CALL_SESSION:
            case WHITEBOARD:
                return true;
            case ADD_PARTICIPANT:
                return isObjectSelf(self);
        }

        return false;
    }

    public boolean shouldWriteToActivityEntry() {
        return !isMuteConversation() &&
                !isUnmuteConversation() &&
                !isFavoriteConversation() &&
                !isUnfavoriteConversation() &&
                !isHideConversation() &&
                !isUnhideConversation() &&
                !isTag() &&
                !isUnTag();
    }

    public boolean isFromSelf(AuthenticatedUser self) {
        return getActor().isAuthenticatedUser(self);
    }

    public boolean isObjectSelf(AuthenticatedUser self) {
        if (object.isPerson()) {
            return TextUtils.equals(self.getEmail(), object.getId())
                    || (object instanceof Person && TextUtils.equals(((Person) object).getUuid(), self.getKey().getUuid()));
        }
        return false;
    }

    public boolean isAddParticipant() {
        return verb.equals(Verb.add) && object != null && object.isPerson() && target != null && target.isConversation();
    }

    public boolean isCreateConversation() {
        return verb.equals(Verb.create) && object != null && object.isConversation();
    }

    public boolean isTag() {
        return verb.equals(Verb.tag) && object != null && object.isConversation();
    }

    public boolean isUnTag() {
        return verb.equals(Verb.untag) && object != null && object.isConversation();
    }

    public boolean isPostOrAddComment() {
        return (verb.equals(Verb.post) || verb.equals(Verb.add)) &&
                object != null && object.isComment() && target != null && target.isConversation();
    }

    public boolean isPostOrShareContent() {
        return (verb.equals(Verb.post) || verb.equals(Verb.share)) &&
                object != null && object.isContent() && target != null && target.isConversation();
    }

    public boolean isShareFile() {
        return object instanceof Content && verb.equals(Verb.share) && (((Content) object).isFile() || ((Content) object).isVideo());
    }

    public boolean isShareImage() {
        return object instanceof Content && verb.equals(Verb.share) && ((Content) object).isImage();
    }

    public boolean isShareWhiteboard() {
        return object instanceof Content && verb.equals(Verb.share) && ((Content) object).isWhiteboard();
    }

    public boolean isImageURI() {
        return (verb.equals(Verb.post) && object != null && object.isImageURI() && target != null && target.isConversation());
    }

    public boolean isAcknowledgeActivity() {
        return verb.equals(Verb.acknowledge);
    }

    public boolean isUpdateTitleAndSummaryActivity() {
        return verb.equals(Verb.update) && object != null && object.isConversation();
    }

    public boolean isSetTeamColor() {
        return verb.equals(Verb.update) && object != null && object.isTeamObject();
    }

    public boolean isMoveRoomToTeam() {
        return verb.equals(Verb.add) && object != null && object.isConversation();
    }

    public boolean isRemoveRoomFromTeam() {
        return verb.equals(Verb.remove) && object != null && object.isConversation();
    }

    public boolean isLocusActivity() {
        return object != null && object.isLocus();
    }

    public boolean isLeaveActivity() {
        return verb.equals(Verb.leave) && (object == null || object.isPerson()) && target != null && target.isConversation();
    }

    public boolean isSelfLeaveActivity(AuthenticatedUser self) {
        if (isLeaveActivity()) {
            Person actorObject = (Person) getObject();
            if (actorObject == null) // Some malformed activities contain a null object
                return true;

            return actorObject.isAuthenticatedUser(self);
        }
        return false;
    }

    public boolean isAddParticipantSelf(AuthenticatedUser self) {
        if (isAddParticipant()) {
            Person person = (Person) object;
            ActorRecord.ActorKey actorAdded = person.getKey();
            if (actorAdded.isAuthenticatedUser(self)) {
                return true;
            }
        }
        return false;
    }

    public boolean isMuteConversation() {
        return verb.equals(Verb.mute);
    }

    public boolean isHideConversation() {
        return verb.equals(Verb.hide);
    }

    public boolean isArchiveConversation() {
        return verb.equals(Verb.archive);
    }

    public boolean isUnarchiveConversation() {
        return verb.equals(Verb.unarchive);
    }

    public boolean isUnhideConversation() {
        return verb.equals(Verb.unhide);
    }

    public boolean isUnmuteConversation() {
        return verb.equals(Verb.unmute);
    }

    public boolean isFavoriteConversation() {
        return verb.equals(Verb.favorite);
    }

    public boolean isUnfavoriteConversation() {
        return verb.equals(Verb.unfavorite);
    }

    public boolean isLockConversation() {
        return verb.equals(Verb.lock);
    }

    public boolean isUnlockConversation() {
        return verb.equals(Verb.unlock);
    }

    public boolean isUpdateContent() {
        return verb.equals(Verb.update) && object.isContent();
    }

    public boolean isAssignModerator() {
        return verb.equals(Verb.assignModerator);
    }

    public boolean isUnassignModerator() {
        return verb.equals(Verb.unassignModerator);
    }

    public boolean isUpdateKeyActivity() {
        return verb.equals(Verb.updateKey) && object != null && object.isConversation();
    }

    public boolean isScheduledSyncUp() {
        return (verb.equals(Verb.schedule) || verb.equals(Verb.update) || verb.equals(Verb.cancel))
                && object != null
                && object.isEvent()
                && target.getId() != null
                && ((EventObject) object).getStartTime() != null
                && ((EventObject) object).getEndTime() != null;
    }

    public boolean isCancel() {
        return verb.equals(Verb.cancel);
    }

    public boolean isUpdate() {
        return verb.equals(Verb.update);
    }

    public boolean isDelete() {
        return verb.equals(Verb.delete);
    }

    public boolean isTombstone() {
        return verb.equals(Verb.tombstone);
    }

    public boolean isAssignRoomAvatar() {
        return verb.equals(Verb.assign) && object != null && object.isContent();
    }

    public boolean isUnassignRoomAvatar() {
        return verb.equals(Verb.unassign) && object != null && object.isContent();
    }

    public boolean isLocusSessionSummary() {
        return verb.equals(Verb.update)
                && object != null
                && object.isLocusSessionSummary()
                && target.getId() != null;
    }

    public boolean isAddNewTeamConversation() {
        return verb.equals(Verb.add)
                && object != null && object.isConversation()
                && target != null && target.isConversation();
    }

    public ConversationContract.ActivityEntry.Type getType() {
        if (activityType != ConversationContract.ActivityEntry.Type._UNKNOWN)
            return activityType;

        else if (isAcknowledgeActivity())
            activityType = ConversationContract.ActivityEntry.Type.ACK;

        else if (isPostOrAddComment())
            activityType = ConversationContract.ActivityEntry.Type.MESSAGE;

        else if (isShareFile())
            activityType = ConversationContract.ActivityEntry.Type.FILE;

        else if (isShareImage())
            activityType = ConversationContract.ActivityEntry.Type.PHOTO;

        else if (isShareWhiteboard())
            activityType = ConversationContract.ActivityEntry.Type.WHITEBOARD;

        else if (isAddParticipant())
            activityType = ConversationContract.ActivityEntry.Type.ADD_PARTICIPANT;

        else if (isCreateConversation())
            activityType = ConversationContract.ActivityEntry.Type.CREATE_CONVERSATION;

        else if (isLeaveActivity())
            activityType = ConversationContract.ActivityEntry.Type.LEFT_CONVERSATION;

        else if (isMuteConversation())
            activityType = ConversationContract.ActivityEntry.Type.MUTE;

        else if (isUnmuteConversation())
            activityType = ConversationContract.ActivityEntry.Type.UNMUTE;

        else if (isFavoriteConversation())
            activityType = ConversationContract.ActivityEntry.Type.FAVORITE;

        else if (isUnfavoriteConversation())
            activityType = ConversationContract.ActivityEntry.Type.UNFAVORITE;

        else if (isUpdateTitleAndSummaryActivity())
            activityType = ConversationContract.ActivityEntry.Type.UPDATE_TITLE_AND_SUMMARY;

        else if (isAssignRoomAvatar())
            activityType = ConversationContract.ActivityEntry.Type.ASSIGN_ROOM_AVATAR;

        else if (isUnassignRoomAvatar())
            activityType = ConversationContract.ActivityEntry.Type.REMOVE_ROOM_AVATAR;

        else if (isUpdateContent())
            activityType = ConversationContract.ActivityEntry.Type.UPDATE_CONTENT;

        else if (isUpdateKeyActivity())
            activityType = ConversationContract.ActivityEntry.Type.UPDATE_KEY;

        else if (isScheduledSyncUp())
            activityType = ConversationContract.ActivityEntry.Type.SCHEDULED_SYNCUP;

        else if (isLocusSessionSummary())
            activityType = ConversationContract.ActivityEntry.Type.CALL_SESSION;

        else if (isTombstone())
            activityType = ConversationContract.ActivityEntry.Type.TOMBSTONE;

        else if (isImageURI())
            activityType = ConversationContract.ActivityEntry.Type.IMAGE_URI;

        else if (isAddNewTeamConversation())
            activityType = ConversationContract.ActivityEntry.Type.NEW_TEAM_CONVERSATION;

        else if (isSetTeamColor())
            activityType = ConversationContract.ActivityEntry.Type.SET_TEAM_COLOR;

        else if (isTag())
            activityType = ConversationContract.ActivityEntry.Type.TAG;

        else if (isUnTag())
            activityType = ConversationContract.ActivityEntry.Type.UNTAG;

        return activityType;
    }

    public String getActivityData(AuthenticatedUser self, Gson gson) {
        switch (getType()) {
            case ACK:
                // not used
                break;
            case MESSAGE:
                return gson.toJson(new Message(this));
            case TOMBSTONE:
                return gson.toJson(new Tombstone(this));
            case ADD_PARTICIPANT:
                Person newParticipant = (Person) getObject();
                return gson.toJson(new ParticipantUpdate(
                        getActor().getKey(),
                        newParticipant.getKey(),
                        getProvider()));
            case CREATE_CONVERSATION:
                return gson.toJson(getActor());
            case UPDATE_TITLE_AND_SUMMARY:
                return gson.toJson(new TitleUpdate(getObject().getDisplayName(), getActor().getKey(), getProvider()));
            case ASSIGN_ROOM_AVATAR:
            case REMOVE_ROOM_AVATAR:
                return gson.toJson(new RoomAvatarAssignment(getActor().getKey(), (Content) getObject(), getProvider()));
            case SET_TEAM_COLOR:
                return gson.toJson(new TeamColorUpdate(((Team) getObject()).getTeamColor(), getActor().getKey(), getProvider()));
            case LEFT_CONVERSATION:
                Person leftParticipant = (Person) getObject();
                return gson.toJson(new ParticipantUpdate(getActor().getKey(), leftParticipant.getKey(), getProvider()));
            case PHOTO:
            case FILE:
            case UPDATE_CONTENT:
            case WHITEBOARD:
                return gson.toJson(DisplayableFileSet.fromActivity(this));
            case MUTE:
            case UNMUTE:
            case FAVORITE:
            case UNFAVORITE:
                // event type not stored
                break;
            case _UNKNOWN:
                break;
            case BACKFILL_GAP:
                break;
            case FORWARDFILL_GAP:
                break;
            case UPDATE_KEY:
                return gson.toJson(ConversationKeyUpdate.fromActivity(this));
            case SCHEDULED_SYNCUP:
                return gson.toJson(EventUpdate.fromActivity(this));
            case CALL_SESSION:
                return gson.toJson(CallSession.fromLocusSessionSummary(self.getKey(), (LocusSessionSummary) this.getObject()));
            case IMAGE_URI:
                return gson.toJson(new ImageURI(this));
            case NEW_TEAM_CONVERSATION:
                return gson.toJson(new NewTeamConversation(getActor().getKey(), getObject().getId(), provider));
        }
        return "";
    }

    public String getContentDataId() {
        if (object != null && object.isContent())
            return getObject().getId();

        return null;
    }

    public String getConversationId() {
        // conversation is 'target' for most activities, 'object' for creates and...?
        String ret = null;
        if (getTarget() != null && ObjectType.conversation.equals(getTarget().getObjectType())) {
            ret = getTarget().getId();
        } else if (getObject() != null && ObjectType.conversation.equals(getObject().getObjectType())) {
            ret = getObject().getId();
        } else if (getObject() != null && ObjectType.team.equals(getObject().getObjectType())) {
            ret = ((Team) getObject()).getGeneralConversationUuid();
        }

        return ret;
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder("Activity ")
                .append(getId())
                .append(" ").append(getVerb())
                .append(" target{").append(getTarget())
                .append("} object{").append(getObject())
                .append("} actor:").append(getActor())
                .append("} source:").append(getSource().name())
                .append(" published:").append(getPublished() == null ? "0" : String.valueOf(getPublished().getTime()))
                .append(isEncrypted() ? "(Encrypted)" : "");

        return ret.toString();
    }

    public boolean isEncrypted() {
        return this.isEncrypted;
    }

    public void setEncrypted(boolean isEncrypted) {
        this.isEncrypted = isEncrypted;
    }

    @Override
    public void encrypt(KeyObject objectKey) throws IOException {
        super.encrypt(objectKey);

        if (getObject() != null) {
            getObject().encrypt(objectKey);
        }

        // Targets don't need display names, make sure one doesn't sneak in unencrypted
        if (getTarget() != null) {
            getTarget().setDisplayName(null);
        }
    }

    @Override
    public void decrypt(KeyObject key) throws IOException, ParseException, NullPointerException {
        super.decrypt(key);

        if (getObject() != null) {
            getObject().decrypt(key);
        }
    }

    public static ActorRecord.ActorKey getActorKeyFromJson(Gson gson, String data, ConversationContract.ActivityEntry.Type type) {
        try {
            switch (type) {
                case MESSAGE:
                case IMAGE_URI:
                    return gson.fromJson(data, Message.class).getActorKey();
                case TOMBSTONE:
                    return gson.fromJson(data, Tombstone.class).getActor();
                case ADD_PARTICIPANT:
                case LEFT_CONVERSATION:
                    return gson.fromJson(data, ParticipantUpdate.class).getActor();
                case CREATE_CONVERSATION:
                    return gson.fromJson(data, Person.class).getKey();
                case UPDATE_TITLE_AND_SUMMARY:
                    return gson.fromJson(data, TitleUpdate.class).getActorKey();
                case ASSIGN_ROOM_AVATAR:
                case REMOVE_ROOM_AVATAR:
                    return gson.fromJson(data, RoomAvatarAssignment.class).getActorKey();
                case SET_TEAM_COLOR:
                    return gson.fromJson(data, TeamColorUpdate.class).getActorKey();
                case PHOTO:
                case FILE:
                case UPDATE_CONTENT:
                case WHITEBOARD:
                    return gson.fromJson(data, DisplayableFileSet.class).getActorKey();
                case SCHEDULED_SYNCUP:
                    return gson.fromJson(data, EventUpdate.class).getActorKey();
                case CALL_SESSION:
                    return gson.fromJson(data, CallSession.class).getActorKey();
            }
        } catch (Exception e) {
            Ln.v(e, "Failed getting actor from json " + data + " : " + type);
            Ln.e(e, "Failed getting actor from json");
        }

        return null;
    }

    public static Provider getProviderFromJson(Gson gson, String data, ConversationContract.ActivityEntry.Type type) {
        try {
            switch (type) {
                case MESSAGE:
                case IMAGE_URI:
                    return gson.fromJson(data, Message.class).getProvider();
                case ADD_PARTICIPANT:
                case LEFT_CONVERSATION:
                    return gson.fromJson(data, ParticipantUpdate.class).getProvider();
                case UPDATE_TITLE_AND_SUMMARY:
                    return gson.fromJson(data, TitleUpdate.class).getProvider();
                case ASSIGN_ROOM_AVATAR:
                case REMOVE_ROOM_AVATAR:
                    return gson.fromJson(data, RoomAvatarAssignment.class).getProvider();
                case SET_TEAM_COLOR:
                    return gson.fromJson(data, TeamColorUpdate.class).getProvider();
                case PHOTO:
                case FILE:
                case UPDATE_CONTENT:
                case WHITEBOARD:
                    return gson.fromJson(data, DisplayableFileSet.class).getProvider();
                case SCHEDULED_SYNCUP:
                    return gson.fromJson(data, EventUpdate.class).getProvider();
            }
        } catch (Exception e) {
            Ln.v(e, "Failed getting provider from json " + data + " : " + type);
            Ln.e(e, "Failed getting provider from json");
        }

        return null;
    }

    public boolean isValid() {
        // Locus activities are deprecated in favor of locus summary activities
        if (isLocusActivity())
            return false;

        if (isLocusSessionSummary()) {
            LocusSessionSummary summary = (LocusSessionSummary) getObject();
            return summary.getParticipants().size() > 1
                    // Old locus summary activities are missing uuids and can't be parsed correctly.
                    // Check the first participant in the summary to see if we have a well formed activity
                    && summary.getParticipants().getItems().get(0).getPerson() != null
                    && summary.getParticipants().getItems().get(0).getPerson().getKey() != null;
        }

        return true;
    }

    public void setActivityType(ConversationContract.ActivityEntry.Type activityType) {
        this.activityType = activityType;
    }

    // Returns the publish time of the activity, or of the acknowledged activity if this is an ack
    public long getAckTime() {
        long ret = getPublished().getTime();

        if (isAcknowledgeActivity() && getObject() != null && getObject().getPublished().getTime() != 0) {
            ret = getObject().getPublished().getTime();
        }
        return ret;
    }

    public void setAlertType(@NonNull AlertType alertType) {
        this.alertType = alertType;
    }

    public AlertType getAlertType() {
        return alertType;
    }

    public boolean isSelfMention(AuthenticatedUser authenticatedUser) {
        return (getObject() instanceof Mentionable)
                && isMentioned((Mentionable) getObject(), authenticatedUser);
    }

    private static boolean isMentioned(Mentionable object, AuthenticatedUser authenticatedUser) {
        if (object.getMentions() == null) {
            return false;
        }

        for (Person mention : object.getMentions().getItems()) {
            if (mention.isAuthenticatedUser(authenticatedUser))
                return true;
        }
        return false;
    }
}
