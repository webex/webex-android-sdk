package com.cisco.spark.android.sync;

import android.annotation.SuppressLint;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import com.cisco.crypto.scr.SecureContentReference;
import com.cisco.spark.android.core.AuthenticatedUser;
import com.cisco.spark.android.model.Activity;
import com.cisco.spark.android.model.Content;
import com.cisco.spark.android.model.Conversation;
import com.cisco.spark.android.model.ConversationTag;
import com.cisco.spark.android.model.CustodianOrgInfo;
import com.cisco.spark.android.model.KeyObject;
import com.cisco.spark.android.model.Participants;
import com.cisco.spark.android.model.Person;
import com.cisco.spark.android.sync.ConversationContract.ActivityEntry;
import com.cisco.spark.android.ui.conversation.ConversationResolver;
import com.cisco.spark.android.util.CryptoUtils;
import com.cisco.spark.android.util.NameUtils;
import com.cisco.spark.android.util.Strings;
import com.cisco.spark.android.util.UriUtils;
import com.github.benoitdion.ln.Ln;
import com.google.gson.Gson;
import com.nimbusds.jose.JOSEException;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

import static com.cisco.spark.android.sync.ConversationContract.ConversationEntry;
import static com.cisco.spark.android.sync.ConversationContract.ConversationEntry.CONVERSATION_ID;
import static com.cisco.spark.android.sync.ConversationContract.ConversationEntry.MENTION_NOTIFICATIONS;
import static com.cisco.spark.android.sync.ConversationContract.ConversationEntry.MESSAGE_NOTIFICATIONS;
import static com.cisco.spark.android.sync.ConversationContract.ConversationEntry.SYNC_OPERATION_ID;
import static com.cisco.spark.android.sync.ConversationContract.DbColumn;
import static com.cisco.spark.android.sync.ConversationContract.vw_Conversation;
import static com.cisco.spark.android.util.UriUtils.parseIfNotNull;

/**
 * This class is used by the Conversation Sync tasks. The general pattern is:
 * <p>
 * 1. Create a ConversationRecord from the DB
 * <p>
 * 2. Update the ConversationRecord by applying new activities to it, or directly
 * <p>
 * 3. Write any updated values back to the database
 * <p>
 * It's important to only write modifications and not the entire record because the database is not
 * locked and it's possible for incoming events to be processed concurrently.
 * <p>
 * For example a 'hide' event and a 'update title' event may have two separate PushSyncTasks running
 * in parallel and one should not overwrite the other.
 */
public class ConversationRecord {
    private Gson gson;
    private String id;
    private String title;
    private Uri url;
    private String spaceUrl;
    private String spaceUrlHidden;
    private EnumSet<ConversationTag> tags = EnumSet.noneOf(ConversationTag.class);
    private EnumSet<ConversationTag> dirtytags = EnumSet.noneOf(ConversationTag.class);
    private Participants participants;
    private long sortingTimestamp;
    private String lastActivityId;
    private long previewActivityPublishedTime;
    private Person oneOnOneParticipant;
    private String syncOperationId;
    private Uri locusUrl;
    private Uri defaultEncryptionKeyUrl;
    private Uri titleKeyUrl;
    private int participantCount;
    private Integer externalParticipantCount;
    private Boolean inActiveCall;
    private boolean areTitleAndSummaryEncrypted;
    private List<ActorRecord> topParticipants = new ArrayList<>();
    private boolean builtFromCursor;
    private String creatorUuid;
    private long lastRelevantActivityTimestamp;
    private TitleBuilder titleBuilder;
    private KmsResourceObject kro;
    private String teamId;
    private Boolean isTeamGuest;
    private String mainTeamRoomId;
    private String summary;
    private ConversationAvatarContentReference avatarContentReference;
    private Uri avatarEncryptionKeyUrl;
    private boolean isAvatarEncrypted;
    private boolean clearedAvatar;
    private int shareCount;
    private Boolean isParticipantListValid;
    private Uri retentionUrl;
    private String custodianOrgName;
    private String custodianOrgId;
    private Uri aclUrl;

    // We keep track of this because after the transaction is committed we write the lastReadable
    // and lastSeen dates (from the conversation record) if and only if the latest activity in this
    // batch is later than the last non-local activity we already know about.
    private long latestActivityInThisBatchTimestamp;
    private Date lastReadableActivityDateRemote;
    private Date lastSeenActivityDateRemote;

    private ConversationRecord(Gson gson, String id, TitleBuilder titleBuilder) {
        this.gson = gson;
        this.id = id;
        this.titleBuilder = titleBuilder;
    }
    public Uri getAclUrl() {
        return aclUrl;
    }

    public void setAclUrl(Uri aclUrl) {
        this.aclUrl = aclUrl;
    }

    public String getId() {
        return id;
    }

    public Uri getUrl() {
        return url;
    }

    public void setUrl(Uri url) {
        this.url = url;
    }

    public String getSyncOperationId() {
        return syncOperationId;
    }

    public String getTitle() {
        if (CryptoUtils.looksLikeCipherText(title))
            return null;

        return title;
    }

    public String getSummary() {
        if (CryptoUtils.looksLikeCipherText(summary))
            return null;

        return summary;
    }

    public void decryptTitleAndSummary(KeyObject key) throws IOException, ParseException, NullPointerException {
        boolean hasDecrypted = false;
        if (CryptoUtils.looksLikeCipherText(title)) {
            setTitle(CryptoUtils.decryptFromJwe(key, title));
            hasDecrypted = true;
        }
        if (CryptoUtils.looksLikeCipherText(summary)) {
            setSummary(CryptoUtils.decryptFromJwe(key, summary));
            hasDecrypted = true;
        }
        if (hasDecrypted) {
            setAreTitleAndSummaryEncrypted(false);
            setTitleKeyUrl(key.getKeyUrl());
        }
    }

    public void decryptAvatarScr(KeyObject key) throws IOException, ParseException, NullPointerException, JOSEException {
        if (avatarContentReference == null || !CryptoUtils.looksLikeCipherText(avatarContentReference.getScr())) {
            return;
        }

        SecureContentReference secureContentReference = SecureContentReference.fromJWE(key.getKeyBytes(), avatarContentReference.getScr());
        setAvatarScr(secureContentReference);
        setAvatarEncryptionKeyUrl(key.getKeyUrl());
        setIsAvatarEncrypted(false);
    }

    public Participants getParticipants() {
        return participants;
    }

    public void freeParticipants() {
        if (!isOneOnOne())
            participants = null;
    }

    public int getParticipantCount() {
        return participantCount;
    }

    public int getShareCount() {
        return shareCount;
    }

    public Boolean isMuted() {
        return hasTag(ConversationTag.MUTED);
    }

    public Boolean isJoined() {
        return !hasTag(ConversationTag.NOT_JOINED);
    }

    public Person getOneOnOneParticipant() {
        return oneOnOneParticipant;
    }

    public boolean isOneOnOne() {
        return hasTag(ConversationTag.ONE_ON_ONE);
    }

    public void setMuted(boolean b) {
        toggleTag(ConversationTag.MUTED, b);
    }

    private void setMessageNotificationsOn(Boolean notificationOn) {
        if (notificationOn == null) {
            toggleTag(ConversationTag.MESSAGE_NOTIFICATIONS_OFF, false);
            toggleTag(ConversationTag.MESSAGE_NOTIFICATIONS_ON, false);
        } else if (notificationOn) {
            toggleTag(ConversationTag.MESSAGE_NOTIFICATIONS_ON, true);
            toggleTag(ConversationTag.MESSAGE_NOTIFICATIONS_OFF, false);
        } else {
            toggleTag(ConversationTag.MESSAGE_NOTIFICATIONS_OFF, true);
            toggleTag(ConversationTag.MESSAGE_NOTIFICATIONS_ON, false);
        }
    }

    private void setMentionNotificationsOn(Boolean notificationOn) {
        if (notificationOn == null) {
            toggleTag(ConversationTag.MENTION_NOTIFICATIONS_OFF, false);
            toggleTag(ConversationTag.MENTION_NOTIFICATIONS_ON, false);
        } else if (notificationOn) {
            toggleTag(ConversationTag.MENTION_NOTIFICATIONS_ON, true);
        } else {
            toggleTag(ConversationTag.MENTION_NOTIFICATIONS_OFF, true);
        }
    }

    public void setJoined(boolean b) {
        toggleTag(ConversationTag.NOT_JOINED, !b);
    }

    private void toggleTag(ConversationTag tag, boolean b) {
        if (b) tags.add(tag);
        else tags.remove(tag);

        dirtytags.add(tag);
    }

    private boolean hasTag(ConversationTag tag) {
        return tags.contains(tag);
    }

    public boolean isHidden() {
        return hasTag(ConversationTag.HIDDEN);
    }

    public void setHidden(boolean b) {
        toggleTag(ConversationTag.HIDDEN, b);
    }

    public boolean isArchived() {
        return hasTag(ConversationTag.ARCHIVED);
    }

    public void setArchived(boolean b) {
        toggleTag(ConversationTag.ARCHIVED, b);
    }

    public void setFavorite(boolean b) {
        toggleTag(ConversationTag.FAVORITE, b);
    }

    public boolean isLocked() {
        return hasTag(ConversationTag.LOCKED);
    }

    public boolean isFavorite() {
        return hasTag(ConversationTag.FAVORITE);
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    public void setMainTeamRoomId(String mainTeamRoomId) {
        this.mainTeamRoomId = mainTeamRoomId;
    }

    public void setLocked(boolean b) {
        toggleTag(ConversationTag.LOCKED, b);
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public void setAreTitleAndSummaryEncrypted(boolean areTitleAndSummaryEncrypted) {
        this.areTitleAndSummaryEncrypted = areTitleAndSummaryEncrypted;
    }

    public boolean isAreTitleAndSummaryEncrypted() {
        return areTitleAndSummaryEncrypted;
    }

    public void setLastActivity(String lastActivityId) {
        if (lastActivityId == null)
            return;
        this.lastActivityId = lastActivityId;
    }

    public void setPreviewActivityPublishedTime(long previewActivityPublishedTime) {
        this.previewActivityPublishedTime = previewActivityPublishedTime;
    }

    public void setAvatarScr(SecureContentReference scr) {
        this.avatarContentReference.setSecureContentReference(scr);
    }

    public void setAvatarScrString(String scr) {
        this.avatarContentReference.setScr(scr);
    }

    public void setAvatarUri(Uri uri) {
        this.avatarContentReference.setUrl(uri);
    }

    public void setAvatarEncryptionKeyUrl(Uri avatarEncryptionKeyUrl) {
        this.avatarEncryptionKeyUrl = avatarEncryptionKeyUrl;
    }

    public Uri getAvatarEncryptionKeyUrl() {
        return avatarEncryptionKeyUrl;
    }

    public ConversationAvatarContentReference getAvatarContentReference() {
        return avatarContentReference;
    }

    public boolean isAvatarEncrypted() {
        return isAvatarEncrypted;
    }

    public void setIsAvatarEncrypted(boolean isAvatarEncrypted) {
        this.isAvatarEncrypted = isAvatarEncrypted;
    }

    /**
     * Sets the time the conversation was last 'active' for sorting purposes. Not necessarily the
     * same as the time the last activity's published_time.
     */
    public void setSortingTimestamp(Date sortingTimestamp) {
        if (sortingTimestamp == null)
            return;

        this.sortingTimestamp = Math.max(sortingTimestamp.getTime(), this.sortingTimestamp);
    }

    public String getTeamId() {
        return teamId;
    }

    public String getMainTeamRoomId() {
        return mainTeamRoomId;
    }

    public long getPreviewActivityPublishedTime() {
        return this.previewActivityPublishedTime;
    }

    public Uri getLocusUrl() {
        return locusUrl;
    }

    public String getCreatorUuid() {
        return creatorUuid;
    }

    public Uri getDefaultEncryptionKeyUrl() {
        return defaultEncryptionKeyUrl;
    }

    public void setDefaultEncryptionKeyUrl(Uri defaultEncryptionKeyUrl) {
        this.defaultEncryptionKeyUrl = defaultEncryptionKeyUrl;
    }

    public static ConversationRecord buildFromCursor(Cursor cursor, Gson gson, TitleBuilder titleBuilder) {
        ConversationRecord record = new ConversationRecord(gson, cursor.getString(vw_Conversation.CONVERSATION_ID.ordinal()), titleBuilder);

        record.participantCount = cursor.getInt(vw_Conversation.PARTICIPANT_COUNT.ordinal());
        record.lastActivityId = cursor.getString(vw_Conversation.LAST_ACTIVITY_ID.ordinal());
        record.title = cursor.getString(vw_Conversation.TITLE.ordinal());
        record.avatarContentReference = gson.fromJson(cursor.getString(vw_Conversation.CONVERSATION_AVATAR_CONTENT_REFERENCE.ordinal()), ConversationAvatarContentReference.class);
        record.summary = cursor.getString(vw_Conversation.SUMMARY.ordinal());
        record.url = UriUtils.parseIfNotNull(cursor.getString(ConversationEntry.URL.ordinal()));
        record.spaceUrl = cursor.getString(vw_Conversation.SPACE_URL.ordinal());
        record.spaceUrlHidden = cursor.getString(vw_Conversation.SPACE_URL_HIDDEN.ordinal());
        record.syncOperationId = cursor.getString(vw_Conversation.SYNC_OPERATION_ID.ordinal());
        record.locusUrl = parseIfNotNull(cursor.getString(vw_Conversation.LOCUS_URL.ordinal()));
        record.defaultEncryptionKeyUrl = UriUtils.parseIfNotNull(cursor.getString(vw_Conversation.DEFAULT_ENCRYPTION_KEY_URL.ordinal()));
        record.titleKeyUrl = UriUtils.parseIfNotNull(cursor.getString(vw_Conversation.TITLE_ENCRYPTION_KEY_URL.ordinal()));
        record.avatarEncryptionKeyUrl = UriUtils.parseIfNotNull(cursor.getString(vw_Conversation.CONVERSATION_AVATAR_ENCRYPTION_KEY_URL.ordinal()));
        record.previewActivityPublishedTime = cursor.getLong(vw_Conversation.ACTIVITY_PUBLISHED_TIME.ordinal());
        record.sortingTimestamp = cursor.getLong(vw_Conversation.SORTING_TIMESTAMP.ordinal());
        record.lastSeenActivityDateRemote = new Date(cursor.getLong(vw_Conversation.TIMESTAMP_LAST_SEEN_ACTIVITY_REMOTE.ordinal()));
        record.lastReadableActivityDateRemote = new Date(cursor.getLong(vw_Conversation.TIMESTAMP_LAST_READABLE_ACTIVITY_REMOTE.ordinal()));
        record.inActiveCall = cursor.getInt(vw_Conversation.IN_ACTIVE_CALL.ordinal()) != 0;
        record.areTitleAndSummaryEncrypted = cursor.getInt(vw_Conversation.IS_TITLE_ENCRYPTED.ordinal()) != 0;
        record.isAvatarEncrypted = cursor.getInt(vw_Conversation.IS_CONVERSATION_AVATAR_ENCRYPTED.ordinal()) != 0;
        record.avatarContentReference = gson.fromJson(cursor.getString(vw_Conversation.CONVERSATION_AVATAR_CONTENT_REFERENCE.ordinal()), ConversationAvatarContentReference.class);
        record.avatarEncryptionKeyUrl = UriUtils.parseIfNotNull(cursor.getString(vw_Conversation.CONVERSATION_AVATAR_ENCRYPTION_KEY_URL.ordinal()));
        record.creatorUuid = cursor.getString(vw_Conversation.CREATOR_UUID.ordinal());
        record.teamId = cursor.getString(vw_Conversation.TEAM_ID.ordinal());
        record.isTeamGuest = cursor.getInt(vw_Conversation.IS_TEAM_GUEST.ordinal()) != 0;
        record.mainTeamRoomId = cursor.getString(vw_Conversation.PRIMARY_TEAM_CONVERSATION_ID.ordinal());
        record.shareCount = cursor.getInt(vw_Conversation.SHARE_COUNT.ordinal());
        record.topParticipants = ConversationContract.ConversationEntry.getTopParticipants(gson, cursor.getString(vw_Conversation.TOP_PARTICIPANTS.ordinal()));
        if (record.topParticipants == null)
            record.topParticipants = new ArrayList<>();
        record.lastRelevantActivityTimestamp = cursor.isNull(vw_Conversation.LAST_RELEVANT_ACTIVITY_TIMESTAMP.ordinal()) ? 0 : cursor.getLong(vw_Conversation.LAST_RELEVANT_ACTIVITY_TIMESTAMP.ordinal());
        record.custodianOrgId = cursor.getString(vw_Conversation.CUSTODIAN_ORG_ID.ordinal());
        record.custodianOrgName = cursor.getString(vw_Conversation.CUSTODIAN_ORG_NAME.ordinal());
        record.retentionUrl = UriUtils.parseIfNotNull(cursor.getString(vw_Conversation.RETENTION_URL.ordinal()));
        record.aclUrl = UriUtils.parseIfNotNull(cursor.getString(vw_Conversation.ACL_URL.ordinal()));
        Uri kroUri = parseIfNotNull(cursor.getString(vw_Conversation.KMS_RESOURCE_OBJECT_URI.ordinal()));
        if (kroUri != null)
            record.kro = new KmsResourceObject(kroUri);

        record.toggleTag(ConversationTag.NOT_JOINED, cursor.getInt(vw_Conversation.SELF_JOINED.ordinal()) == 0);

        record.toggleTag(ConversationTag.MUTED, cursor.getInt(vw_Conversation.MUTED.ordinal()) != 0);


        if (!cursor.isNull(vw_Conversation.MESSAGE_NOTIFICATIONS.ordinal())) {
            if (cursor.getInt(vw_Conversation.MESSAGE_NOTIFICATIONS.ordinal()) != 0) {
                record.tags.add(ConversationTag.MESSAGE_NOTIFICATIONS_ON);
            } else {
                record.tags.add(ConversationTag.MESSAGE_NOTIFICATIONS_OFF);
            }
        }

        if (!cursor.isNull(vw_Conversation.MENTION_NOTIFICATIONS.ordinal())) {
            if (cursor.getInt(vw_Conversation.MENTION_NOTIFICATIONS.ordinal()) != 0) {
                record.tags.add(ConversationTag.MENTION_NOTIFICATIONS_ON);
            } else {
                record.tags.add(ConversationTag.MENTION_NOTIFICATIONS_OFF);
            }
        }

        record.toggleTag(ConversationTag.HIDDEN, cursor.getInt(vw_Conversation.HIDDEN.ordinal()) != 0);
        record.toggleTag(ConversationTag.ARCHIVED, cursor.getInt(vw_Conversation.ARCHIVED.ordinal()) != 0);
        record.toggleTag(ConversationTag.ONE_ON_ONE, Strings.notEmpty(cursor.getString(vw_Conversation.ONE_ON_ONE_PARTICIPANT.ordinal())));
        record.toggleTag(ConversationTag.LOCKED, cursor.getInt(vw_Conversation.LOCKED.ordinal()) != 0);
        record.toggleTag(ConversationTag.FAVORITE, cursor.getInt(vw_Conversation.FAVORITE.ordinal()) != 0);
        record.dirtytags.clear();
        if (record.isOneOnOne()) {
            record.oneOnOneParticipant = new Person(cursor.getString(vw_Conversation.ONE_ON_ONE_PARTICIPANT.ordinal()));
        }

        // Small optimization to skip needless inserts
        record.builtFromCursor = true;

        return record;
    }

    @SuppressLint("Recycle") // TODO: Remove, Lint is giving a false positive.
    public static ConversationRecord buildFromContentResolver(ContentResolver contentResolver, Gson gson, String id, TitleBuilder titleBuilder) {
        Cursor cursor = null;

        ConversationRecord ret = null;

        try {
            cursor = contentResolver.query(Uri.withAppendedPath(vw_Conversation.CONTENT_URI, id), vw_Conversation.DEFAULT_PROJECTION, null, null, null);
            if (cursor != null && cursor.moveToFirst())
                ret = buildFromCursor(cursor, gson, titleBuilder);
        } finally {
            if (cursor != null)
                cursor.close();
            cursor = null;
        }

        if (ret == null) {
            Ln.w("Failed creating conversation record for " + id);
            return null;
        }

        try {
            ret.participants = new Participants();
            cursor = contentResolver.query(ConversationContract.vw_Participant.CONTENT_URI, ConversationContract.vw_Participant.DEFAULT_PROJECTION, ConversationContract.vw_Participant.CONVERSATION_ID + "=?", new String[]{id}, null);
            while (cursor != null && cursor.moveToNext()) {
                ActorRecord actor = new ActorRecord(cursor);
                ret.participants.add(actor);
                if (ret.oneOnOneParticipant != null && ret.oneOnOneParticipant.equals(new Person(actor))) {
                    ret.oneOnOneParticipant = new Person(actor);
                }
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return ret;
    }

    public Uri getTitleKeyUrl() {
        return titleKeyUrl;
    }

    public void setTitleKeyUrl(Uri titleKeyUrl) {
        this.titleKeyUrl = titleKeyUrl;
    }

    public static ConversationRecord buildFromConversation(Gson gson, Conversation conversation, AuthenticatedUser self, TitleBuilder titleBuilder) {
        ConversationRecord record = new ConversationRecord(gson, conversation.getId(), titleBuilder);
        record.participants = new Participants();
        record.update(conversation, self);
        if (conversation.isOneOnOne() && conversation.getParticipants() != null) {
            for (Person person : conversation.getParticipants().getItems()) {
                if (!person.isAuthenticatedUser(self)) {
                    record.oneOnOneParticipant = person;
                    record.tags.add(ConversationTag.ONE_ON_ONE);
                }
            }
        }
        return record;
    }

    public void update(Conversation conversation, AuthenticatedUser self) {
        title = conversation.getDisplayName();
        setJoined(conversation.isJoined());
        setMuted(conversation.isMuted());
        setMentionNotificationsOn(conversation.isMentionNotificationsOn());
        setMessageNotificationsOn(conversation.isMessageNotificationsOn());
        setHidden(conversation.isHidden());
        setArchived(conversation.isArchived());
        setLocked(conversation.isLocked());
        setFavorite(conversation.isFavorite());
        setSummary(conversation.getSummary());
        if (conversation.getAvatar() != null && !TextUtils.isEmpty(conversation.getAvatarEncryptionKeyUrl().toString()) && !conversation.getAvatar().getFiles().getItems().isEmpty()) {
            avatarContentReference = new ConversationAvatarContentReference();
            setAvatarEncryptionKeyUrl(conversation.getAvatarEncryptionKeyUrl());

            // Right now there should only be one avatar for a conversation so just grab the first we see
            // This may be encrypted
            setAvatarScrString(conversation.getAvatar().getFiles().getItems().get(0).getScr());
        }
        if (conversation.getTeam() != null) {
            setTeamId(conversation.getTeam().getId());
            if (conversation.getTeam().getGeneralConversationUuid() != null) {
                setMainTeamRoomId(conversation.getTeam().getGeneralConversationUuid());
            }
        }
        isTeamGuest = conversation.isTeamGuestMember();
        url = conversation.getUrl();
        participants.set(conversation.getParticipants().getItems());
        locusUrl = conversation.getLocusUrl();
        defaultEncryptionKeyUrl = conversation.getDefaultActivityEncryptionKeyUrl();
        titleKeyUrl = conversation.getTitleEncryptionKeyUrl();
        creatorUuid = conversation.getCreatorUUID();
        kro = conversation.getKmsResourceObject();
        setLastRelevantActivityTimestamp(conversation.getLastRelevantActivityTimestamp());
        if (!TextUtils.isEmpty(conversation.getClientTempId()))
            syncOperationId = conversation.getClientTempId();

        // note: topParticipants here will be used for inserts only
        topParticipants.clear();
        for (Person person : conversation.getParticipants().getItems()) {
            addTopParticipant(new ActorRecord(person), self);

            if (topParticipants.size() > ConversationResolver.MAX_TOP_PARTICIPANTS)
                break;
        }

        lastReadableActivityDateRemote = conversation.getLastReadableActivityDate();
        lastSeenActivityDateRemote = maxDate(lastSeenActivityDateRemote, conversation.getLastSeenActivityDate());
        CustodianOrgInfo custodianOrgInfo = conversation.getCustodianOrg();
        if (custodianOrgInfo != null) {
            custodianOrgId = custodianOrgInfo.getOrgId();
            custodianOrgName = custodianOrgInfo.getOrgName();
        }
        aclUrl = conversation.getAclUrl();
        retentionUrl = conversation.getRetentionUrl();

    }

    /**
     * Given an activity from the head of the stream, modify the conversation record as needed
     *
     * @param activity An Activity that operates on this conversation
     */
    public void applyActivity(Activity activity, AuthenticatedUser self) {
        if (activity == null)
            return;

        if (activity.isMuteConversation() || activity.isUnmuteConversation()) {
            if (activity.getObject() instanceof Conversation) {

                Conversation conversation = (Conversation) activity.getObject();
                List<ConversationTag> newUpdatedTags = conversation.getTags();
                setMuted(activity.isMuteConversation());

                // The mute unmute verb activity will put the changed notification tags in its object.tag
                // apply these here as a special case so we get the correct tag state until we do
                // a new sync.

                for (ConversationTag newUpdatedTag : newUpdatedTags) {
                    if (newUpdatedTag == ConversationTag.MESSAGE_NOTIFICATIONS_OFF) {
                        tags.add(newUpdatedTag);
                        tags.remove(ConversationTag.MESSAGE_NOTIFICATIONS_ON);
                        dirtytags.add(newUpdatedTag);
                    }
                    if (newUpdatedTag == ConversationTag.MESSAGE_NOTIFICATIONS_ON) {
                        tags.add(newUpdatedTag);
                        tags.remove(ConversationTag.MESSAGE_NOTIFICATIONS_OFF);
                        dirtytags.add(newUpdatedTag);
                    }
                    if (newUpdatedTag == ConversationTag.MENTION_NOTIFICATIONS_OFF) {
                        tags.add(newUpdatedTag);
                        tags.remove(ConversationTag.MENTION_NOTIFICATIONS_ON);
                        dirtytags.add(newUpdatedTag);
                    }
                    if (newUpdatedTag == ConversationTag.MENTION_NOTIFICATIONS_ON) {
                        tags.add(newUpdatedTag);
                        tags.remove(ConversationTag.MENTION_NOTIFICATIONS_OFF);
                        dirtytags.add(newUpdatedTag);
                    }
                }
            } else {
                Ln.w("Invalid mute/unmute activity, does not contain a correct conversation object");
            }
        } else if (activity.isFavoriteConversation() || activity.isUnfavoriteConversation()) {
            setFavorite(activity.isFavoriteConversation());
        } else if (activity.isHideConversation() || activity.isUnhideConversation()) {
            setHidden(activity.isHideConversation());
        } else if (activity.isArchiveConversation() || activity.isUnarchiveConversation()) {
            setArchived(activity.isArchiveConversation());
            setHidden(activity.isArchiveConversation());
        } else if (activity.isUpdateTitleAndSummaryActivity()) {
            setTitle(activity.getObject().getDisplayName());
            setSummary(((Conversation) activity.getObject()).getSummary());
            if (activity.getEncryptionKeyUrl() != null)
                setTitleKeyUrl(activity.getEncryptionKeyUrl());
            setAreTitleAndSummaryEncrypted(activity.getEncryptionKeyUrl() != null);
        } else if (activity.isAssignRoomAvatar()) {
            setAvatarEncryptionKeyUrl(activity.getEncryptionKeyUrl());
            setAvatarScrString(((Content) activity.getObject()).getFiles().getItems().get(0).getScr());
            setIsAvatarEncrypted(activity.getEncryptionKeyUrl() != null);
        } else if (activity.isUnassignRoomAvatar()) {
            clearedAvatar = true;
            setIsAvatarEncrypted(false);
        } else if (activity.isUpdateKeyActivity()) {
            if (activity.getObject() != null && activity.getObject().isConversation()) {
                setDefaultEncryptionKeyUrl(((Conversation) activity.getObject()).getDefaultActivityEncryptionKeyUrl());
            }
        } else if (activity.isCreateConversation()) {
            Conversation conv = (Conversation) activity.getObject();

            if (TextUtils.isEmpty(title) && !TextUtils.isEmpty(conv.getDisplayName())) {
                setTitle(activity.getObject().getDisplayName());
            }

            participants = new Participants();
            participants.addAll(conv.getParticipants().getItems());
        } else if (activity.isLockConversation() || activity.isUnlockConversation()) {
            setLocked(activity.isLockConversation());
        } else if (activity.isAcknowledgeActivity() && activity.isFromSelf(self) && activity.getSource() != ActivityEntry.Source.LOCAL) {
            if (activity.getObject() != null)
                lastSeenActivityDateRemote = maxDate(lastSeenActivityDateRemote, activity.getObject().getPublished());
        } else if (activity.isTag() || activity.isUnTag()) {
            // The tag untag operation holds the tags being tagged or untagged in object.tags
            if (activity.getObject() instanceof Conversation) {
                Conversation conversation = (Conversation) activity.getObject();
                List<ConversationTag> conversationTags = conversation.getTags();

                if (activity.isTag()) {
                    tags.addAll(conversationTags);
                    dirtytags.addAll(conversationTags);
                } else {
                    tags.removeAll(conversationTags);
                    dirtytags.addAll(conversationTags);
                }
            } else {
                Ln.w("Invalid tag/untag activity, does not contain a correct conversation object");
            }
        } else if (activity.isMoveRoomToTeam()) {
            setTeamId(activity.getTarget().getId());
        } else if (activity.isRemoveRoomFromTeam()) {
            setTeamId("");
        }

        if (activity.isFromSelf(self) && activity.getSource() != ActivityEntry.Source.LOCAL && activity.getProvider() == null) {
            lastSeenActivityDateRemote = maxDate(lastSeenActivityDateRemote, activity.getPublished());
        }

        if (activity.shouldBecomeLastActivityPreview(self)) {
            if (lastActivityId == null || previewActivityPublishedTime <= activity.getPublished().getTime()) {
                setLastActivity(activity.getId());
                previewActivityPublishedTime = activity.getPublished().getTime();
            }
        }

        if (activity.shouldAffectRoomSortingOrder(self)) {
            setSortingTimestamp(activity.getPublished());
        }

        if (activity.shouldBecomeLastActivityPreview(self) && participants.contains(activity.getActor()))
            addTopParticipant(new ActorRecord(activity.getActor()), self);

        if (isOneOnOne() && !activity.isHideConversation() && !activity.isAcknowledgeActivity() &&
                (activity.getSource().isPushNotification() || activity.getSource() == ActivityEntry.Source.LOCAL)) {
            setHidden(false);
        }

        if (activity.getTarget() != null && activity.getTarget().isConversation()) {
            Conversation target = (Conversation) activity.getTarget();
            setLastRelevantActivityTimestamp(target.getLastRelevantActivityTimestamp());
        }

        if (activity.isFromSelf(self)) {
            lastSeenActivityDateRemote = maxDate(lastSeenActivityDateRemote, activity.getPublished());
        }

        if (activity.getSource() != ActivityEntry.Source.LOCAL && activity.getPublished() != null) {
            latestActivityInThisBatchTimestamp = Math.max(activity.getPublished().getTime(), latestActivityInThisBatchTimestamp);
        }
    }

    private Date maxDate(Date date1, Date date2) {
        if (date1 == null)
            return date2;
        if (date2 == null)
            return date1;
        if (date1.after(date2))
            return date1;
        return date2;
    }

    private void addTopParticipant(ActorRecord actorRecord, AuthenticatedUser self) {
        if (actorRecord.isAuthenticatedUser(self))
            return;

        topParticipants.remove(actorRecord);
        topParticipants.add(0, actorRecord);
        while (topParticipants.size() > ConversationResolver.MAX_TOP_PARTICIPANTS)
            topParticipants.remove(topParticipants.size() - 1);
    }

    public ContentProviderOperation.Builder getInsertOperation() {
        ContentValues cv = getGeneralContentValues();
        if (lastActivityId != null) {
            cv.put(ConversationEntry.LAST_ACTIVITY_ID.name(), lastActivityId);
        }
        if (sortingTimestamp > 0) {
            cv.put(ConversationEntry.SORTING_TIMESTAMP.name(), sortingTimestamp);
        }
        if (topParticipants != null && !topParticipants.isEmpty()) {
            cv.put(ConversationEntry.TOP_PARTICIPANTS.name(), gson.toJson(topParticipants));
        }
        if (lastRelevantActivityTimestamp > 0) {
            cv.put(ConversationEntry.LAST_RELEVANT_ACTIVITY_TIMESTAMP.name(), lastRelevantActivityTimestamp);
        }
        if (!TextUtils.isEmpty(teamId)) {
            cv.put(ConversationEntry.TEAM_ID.name(), teamId);
        }
        if (getLastReadableActivityDateRemote() != null) {
            cv.put(ConversationEntry.TIMESTAMP_LAST_READABLE_ACTIVITY_REMOTE.name(), lastReadableActivityDateRemote.getTime());
        }
        if (getLastSeenActivityDateRemote() != null) {
            cv.put(ConversationEntry.TIMESTAMP_LAST_SEEN_ACTIVITY_REMOTE.name(), lastSeenActivityDateRemote.getTime());
        }
        if (syncOperationId != null) {
            cv.put(ConversationEntry.SYNC_OPERATION_ID.name(), syncOperationId);
        }
        cv.putAll(getCountUpdateContentValues());

        return ContentProviderOperation.newInsert(ConversationEntry.CONTENT_URI)
                .withValues(cv);
    }

    /**
     * Read/Unread state is controlled by the server using the lastReadableActivity and
     * lastSeenActivity fields. Complicating factors:
     * <p>
     * - Activities are often delivered out of order, so the timestamps may be stale - The
     * lastReadableActivity timestamp can go backwards if activities are deleted - Locally generated
     * timestamps are unreliable
     * <p>
     * To determine whether we should overwrite the lastReadableActivity and lastSeenActivity fields
     * in the database, we track the timestamp fo the latest activity that came with the
     * conversation and compare it to the latest activity we already have a record of.
     * <p>
     * If we received a new last activity from the server, write the properties from the
     * conversation object to the db.
     * <p>
     * Local (provisional, outgoing) activities are are excluded from this calculation in
     * ConversationRecord.applyActivity.
     */
    public void addUpdateLastReadableTimestamp(Batch batch) {
        if (lastReadableActivityDateRemote == null || lastReadableActivityDateRemote.getTime() == 0 || latestActivityInThisBatchTimestamp == 0)
            return;

        String selection = new StringBuilder()
                .append("NOT EXISTS ( ")
                .append("SELECT 1 FROM ")
                .append(ActivityEntry.TABLE_NAME)
                .append(" WHERE ")
                .append(ActivityEntry.CONVERSATION_ID.toString())
                .append(" = ?")
                .append(" AND ")
                .append(ActivityEntry.SOURCE.toString())
                .append(" != ")
                .append(ActivityEntry.Source.LOCAL.ordinal())
                .append(" AND ")
                .append(ActivityEntry.ACTIVITY_PUBLISHED_TIME.toString())
                .append(" > ? ")
                .append(" )")
                .toString();

        String[] args = new String[]{getId(), String.valueOf(latestActivityInThisBatchTimestamp)};

        ContentProviderOperation.Builder builder =
                ContentProviderOperation.newUpdate(Uri.withAppendedPath(ConversationEntry.CONTENT_URI, getId()))
                        .withValue(ConversationEntry.TIMESTAMP_LAST_READABLE_ACTIVITY_REMOTE.name(), lastReadableActivityDateRemote.getTime())
                        .withSelection(selection, args);

        batch.add(builder.build());
    }

    /**
     * Update operations are split into several statements because some fields require specific
     * selection parameters to ensure integrity. It also ensures that related values (like key and
     * key url) stay in sync.
     *
     * @param batch
     * @param withYield
     */
    public void addUpdateOperations(Batch batch, boolean withYield) {
        // General fields
        ContentValues cv = getGeneralContentValues();
        if (cv.size() > 0) {
            ContentProviderOperation.Builder builder = ContentProviderOperation.newUpdate(ConversationEntry.CONTENT_URI)
                    .withSelection(CONVERSATION_ID + "=? OR " + SYNC_OPERATION_ID + "=?",
                            new String[]{getId(), getSyncOperationId() == null ? getId() : getSyncOperationId()})
                    .withValues(cv)
                    .withYieldAllowed(withYield);
            batch.add(builder.build());
        }

        // Last Activity field. Update separately to ensure it only moves forward.
        if (!TextUtils.isEmpty(lastActivityId)) {
            ContentProviderOperation.Builder builder = ContentProviderOperation.newUpdate(ConversationEntry.CONTENT_URI)
                    .withSelection("(CONVERSATION_ID=? or SYNC_OPERATION_ID=?) "

                                    // make sure we go forward in time
                                    + " and (select COALESCE(ACTIVITY_PUBLISHED_TIME, 0) from vw_Conversation where CONVERSATION_ID=?) < ?"

                                    // make sure the activity id exists, in case it's a stale clientTempId
                                    + " and EXISTS (select ACTIVITY_ID from ActivityEntry WHERE ACTIVITY_ID=?)",
                            new String[]{getId(), getSyncOperationId() == null ? getId() : getSyncOperationId(), getId(), String.valueOf(previewActivityPublishedTime), lastActivityId})
                    .withValue(ConversationEntry.LAST_ACTIVITY_ID.name(), lastActivityId)
                    .withYieldAllowed(withYield);
            batch.add(builder.build());
        }

        addTimestampUpdateOperation(batch, ConversationEntry.SORTING_TIMESTAMP, sortingTimestamp, withYield);
        addTimestampUpdateOperation(batch, ConversationEntry.LAST_RELEVANT_ACTIVITY_TIMESTAMP, lastRelevantActivityTimestamp, withYield);
        if (lastSeenActivityDateRemote != null)
            addTimestampUpdateOperation(batch, ConversationEntry.TIMESTAMP_LAST_SEEN_ACTIVITY_REMOTE, lastSeenActivityDateRemote.getTime(), withYield);
    }

    // Add an update operation to the batch for a single timestamp column. Timestamp columns are guaranteed
    // to only move forward in time.  The column must not allow nulls.
    private void addTimestampUpdateOperation(Batch batch, ConversationEntry col, long val, boolean withYield) {
        if (val == 0)
            return;

        String syncOpId = getSyncOperationId();
        if (TextUtils.isEmpty(syncOpId))
            syncOpId = getId();

        ContentProviderOperation.Builder builder = ContentProviderOperation.newUpdate(ConversationEntry.CONTENT_URI)
                .withSelection("(CONVERSATION_ID=? OR SYNC_OPERATION_ID=?) "
                                + " AND " + col.name() + " < ?",
                        new String[]{getId(), syncOpId, String.valueOf(val)})
                .withValue(col.name(), val)
                .withYieldAllowed(withYield);
        batch.add(builder.build());
    }

    public void addCountUpdateOperations(Batch batch) {
        ContentValues cv = getCountUpdateContentValues();
        if (cv.size() > 0) {
            ContentProviderOperation.Builder builder = ContentProviderOperation.newUpdate(ConversationEntry.CONTENT_URI)
                    .withSelection(CONVERSATION_ID + "=? OR " + SYNC_OPERATION_ID + "=?",
                            new String[]{getId(), getSyncOperationId() == null ? getId() : getSyncOperationId()})
                    .withValues(cv)
                    .withYieldAllowed(true);
            batch.add(builder.build());
        }
    }


    public void addShareCountOperation(Batch batch, int shareCount) {
        ContentValues cv = new ContentValues();
        if (shareCount > 0) {
            cv.put(ConversationEntry.SHARE_COUNT.name(), String.valueOf(shareCount));
        }

        if (cv.size() > 0) {
            ContentProviderOperation.Builder builder = ContentProviderOperation.newUpdate(ConversationEntry.CONTENT_URI)
                    .withSelection(CONVERSATION_ID + "=? ",
                            new String[]{getId()})
                    .withValues(cv)
                    .withYieldAllowed(true);
            batch.add(builder.build());
        }
    }

    private ContentValues getCountUpdateContentValues() {
        ContentValues cv = new ContentValues();
        if (participantCount > 0) {
            cv.put(ConversationEntry.PARTICIPANT_COUNT.name(), String.valueOf(participantCount));
        }
        if (externalParticipantCount != null) {
            cv.put(ConversationEntry.EXTERNAL_PARTICIPANT_COUNT.name(), externalParticipantCount);
        }
        return cv;
    }

    /**
     * Builds a CV of conversation values that have been modified. It's important to only write data
     * that is dirty. Otherwise we may collide with another sync transaction and write stale data.
     *
     * @return a ContentValues object populated with values that need writing.
     */
    private ContentValues getGeneralContentValues() {
        ContentValues cv = new ContentValues();
        cv.put(CONVERSATION_ID.name(), getId());

        if (dirtytags.contains(ConversationTag.MUTED))
            cv.put(ConversationEntry.MUTED.name(), isMuted());

        updateTagsForNotificationIfDirty(cv, ConversationTag.MESSAGE_NOTIFICATIONS_OFF, MESSAGE_NOTIFICATIONS.name(), false, ConversationTag.MESSAGE_NOTIFICATIONS_ON);
        updateTagsForNotificationIfDirty(cv, ConversationTag.MESSAGE_NOTIFICATIONS_ON, MESSAGE_NOTIFICATIONS.name(), true, ConversationTag.MESSAGE_NOTIFICATIONS_OFF);
        updateTagsForNotificationIfDirty(cv, ConversationTag.MENTION_NOTIFICATIONS_OFF, MENTION_NOTIFICATIONS.name(), false, ConversationTag.MENTION_NOTIFICATIONS_ON);
        updateTagsForNotificationIfDirty(cv, ConversationTag.MENTION_NOTIFICATIONS_ON, MENTION_NOTIFICATIONS.name(), true, ConversationTag.MENTION_NOTIFICATIONS_OFF);

        if (dirtytags.contains(ConversationTag.HIDDEN))
            cv.put(ConversationEntry.HIDDEN.name(), isHidden());

        if (dirtytags.contains(ConversationTag.ARCHIVED))
            cv.put(ConversationEntry.ARCHIVED.name(), isArchived());

        if (dirtytags.contains(ConversationTag.LOCKED))
            cv.put(ConversationEntry.LOCKED.name(), isLocked());

        if (dirtytags.contains(ConversationTag.FAVORITE))
            cv.put(ConversationEntry.FAVORITE.name(), isFavorite());

        if (dirtytags.contains(ConversationTag.NOT_JOINED))
            cv.put(ConversationEntry.SELF_JOINED.name(), isJoined());

        if (url != null) {
            cv.put(ConversationEntry.URL.name(), url.toString());
        }
        if (spaceUrl != null) {
            cv.put(ConversationEntry.SPACE_URL.name(), spaceUrl);
        }
        if (spaceUrlHidden != null) {
            cv.put(ConversationEntry.SPACE_URL_HIDDEN.name(), spaceUrlHidden);
        }

        if (isOneOnOne()) {
            if (oneOnOneParticipant != null) {
                cv.put(ConversationEntry.ONE_ON_ONE_PARTICIPANT.name(), oneOnOneParticipant.getEmail());
            } else if (participants != null && !participants.get().isEmpty()) {
                cv.put(ConversationEntry.ONE_ON_ONE_PARTICIPANT.name(), participants.get().get(0).getEmail());
            } else {
                Ln.e("No participants?");
            }
        }
        if (locusUrl != null) {
            cv.put(ConversationEntry.LOCUS_URL.name(), locusUrl.toString());
        }
        if (kro != null) {
            cv.put(ConversationEntry.KMS_RESOURCE_OBJECT_URI.name(), kro.getUri().toString());
        }
        if (creatorUuid != null) {
            cv.put(ConversationEntry.CREATOR_UUID.name(), creatorUuid);
        }

        if (title != null || titleKeyUrl != null) {
            putStringOrNull(cv, ConversationEntry.TITLE, title);
            putStringOrNull(cv, ConversationEntry.TITLE_ENCRYPTION_KEY_URL, titleKeyUrl == null ? null : titleKeyUrl.toString());
        }
        if (summary != null) {
            putStringOrNull(cv, ConversationEntry.SUMMARY, summary);
        }

        if (clearedAvatar || avatarContentReference != null || avatarEncryptionKeyUrl != null) {
            putStringOrNull(cv, ConversationEntry.AVATAR_ENCRYPTION_KEY_URL, clearedAvatar ? null : avatarEncryptionKeyUrl == null ? null : avatarEncryptionKeyUrl.toString());
            String json = null;
            if (avatarContentReference != null) {
                json = gson.toJson(avatarContentReference, ConversationAvatarContentReference.class);
            }
            putStringOrNull(cv, ConversationEntry.CONVERSATION_AVATAR_CONTENT_REFERENCE, clearedAvatar ? null : json);
        }
        cv.put(ConversationEntry.IS_AVATAR_ENCRYPTED.name(), isAvatarEncrypted);

        if (defaultEncryptionKeyUrl != null) {
            putStringOrNull(cv, ConversationEntry.DEFAULT_ENCRYPTION_KEY_URL, defaultEncryptionKeyUrl.toString());
        }

        if (inActiveCall != null) {
            cv.put(ConversationEntry.IN_ACTIVE_CALL.name(), inActiveCall);
        }
        cv.put(ConversationEntry.IS_TITLE_ENCRYPTED.name(), areTitleAndSummaryEncrypted);

        String displayName = getDisplayName();

        if (!TextUtils.isEmpty(displayName)) {
            cv.put(ConversationEntry.CONVERSATION_DISPLAY_NAME.name(), displayName);
        }

        if (teamId != null) {
            putStringOrNull(cv, ConversationEntry.TEAM_ID, TextUtils.isEmpty(teamId) ? null : teamId);
        }

        if (isTeamGuest != null) {
            cv.put(ConversationEntry.IS_TEAM_GUEST.name(), isTeamGuest ? 1 : 0);
        }

        if (isParticipantListValid != null) {
            cv.put(ConversationEntry.IS_PARTICIPANT_LIST_VALID.name(), isParticipantListValid ? 1 : 0);
        }

        if (retentionUrl != null) {
            cv.put(ConversationEntry.RETENTION_URL.name(), retentionUrl.toString());
        }
        if (custodianOrgId != null) {
            cv.put(ConversationEntry.CUSTODIAN_ORG_ID.name(), custodianOrgId);
        }
        if (custodianOrgName != null) {
            cv.put(ConversationEntry.CUSTODIAN_ORG_NAME.name(), custodianOrgName);
        }

        if (aclUrl != null) {
            cv.put(ConversationEntry.ACL_URL.name(), aclUrl.toString());
        }
        return cv;
    }

    /**
     * Handles updating the tags based on the dirty tags for a tag operation
     *
     * @param cv
     * @param notificationTagA for instance Message_OFF
     * @param name             corresponding column in db
     * @param value            if A is OFF value is false, otherwise true.
     * @param notificationTagB for instance Message_ON
     */
    private void updateTagsForNotificationIfDirty(ContentValues cv, ConversationTag notificationTagA, String name, boolean value, ConversationTag notificationTagB) {
        if (dirtytags.contains(notificationTagA)) {
            if (hasTag(notificationTagA)) {
                // Tag A is set, set value and remove opposite tag B if set for consistency
                cv.put(name, value);
                if (hasTag(notificationTagB)) {
                    tags.remove(notificationTagB);
                }
            } else {
                if (!hasTag(notificationTagB)) {
                    // We do not have tag A set, nor opposite tag B, remove value to use default
                    cv.putNull(name);
                }
            }
        }
    }

    private void putStringOrNull(ContentValues cv, DbColumn col, String val) {
        if (val == null) {
            cv.putNull(col.name());
        } else {
            cv.put(col.name(), val);
        }
    }

    public void setParticipantCount(int participantCount) {
        if (participantCount > 0)
            this.participantCount = participantCount;
    }

    public void setExternalParticipantCount(int participantCount) {
        this.externalParticipantCount = participantCount;
    }

    public String getLastActivityId() {
        return lastActivityId;
    }

    public void setLastRelevantActivityTimestamp(Date lastRelevantActivityTimestamp) {
        if (lastRelevantActivityTimestamp != null)
            this.lastRelevantActivityTimestamp = Math.max(lastRelevantActivityTimestamp.getTime(), this.lastRelevantActivityTimestamp);
    }

    public long getSortingTimestamp() {
        return sortingTimestamp;
    }

    public boolean isBuiltFromCursor() {
        return builtFromCursor;
    }

    public EnumSet<ConversationTag> getTags() {
        return EnumSet.copyOf(this.tags);
    }

    private String getDisplayName() {
        String displayName = null;

        if (isOneOnOne()) {
            if (topParticipants.size() == 1) {
                displayName = NameUtils.getShortName(topParticipants.get(0).getDisplayName());
            } else {
                displayName = NameUtils.getShortName(extractPersonName(topParticipants));
            }

            if (!TextUtils.isEmpty(displayName) && !Strings.isEmailAddress(displayName)) {
                return displayName;
            }
            displayName = null;
        }

        if (!CryptoUtils.looksLikeCipherText(title) && !TextUtils.isEmpty(title)) {
            return title;
        }

        if (titleBuilder == null || topParticipants == null || topParticipants.isEmpty())
            return null;

        return titleBuilder.build(displayName, topParticipants, getParticipantCount(), getCreatorUuid());
    }

    private String extractPersonName(List<ActorRecord> participants) {
        String displayName = null;
        for (ActorRecord actor : participants) {
            if (Person.PERSON.equals(actor.getType())) {
                displayName = actor.getDisplayName();
            }
        }
        return displayName;
    }

    public Date getLastReadableActivityDateRemote() {
        return lastReadableActivityDateRemote;
    }

    public Date getLastSeenActivityDateRemote() {
        return lastSeenActivityDateRemote;
    }

    public KmsResourceObject getKmsResourceObject() {
        return kro;
    }

    public void setKmsResourceObject(KmsResourceObject kmsResourceObject) {
        this.kro = kmsResourceObject;
    }

    public void applyConversationObject(Conversation conv) {
        lastReadableActivityDateRemote = conv.getLastReadableActivityDate();
        lastSeenActivityDateRemote = maxDate(lastSeenActivityDateRemote, conv.getLastSeenActivityDate());
    }

    public void setParticipantsListValid(boolean participantsListValid) {
        this.isParticipantListValid = participantsListValid;
    }
}
