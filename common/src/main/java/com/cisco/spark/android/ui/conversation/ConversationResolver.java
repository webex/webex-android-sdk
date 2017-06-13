package com.cisco.spark.android.ui.conversation;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.cisco.spark.android.R;
import com.cisco.spark.android.core.AuthenticatedUser;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.metrics.EncryptionDurationMetricManager;
import com.cisco.spark.android.metrics.value.EncryptionMetrics;
import com.cisco.spark.android.model.Activity;
import com.cisco.spark.android.model.KeyManager;
import com.cisco.spark.android.model.KeyObject;
import com.cisco.spark.android.model.Person;
import com.cisco.spark.android.model.Team;
import com.cisco.spark.android.sync.ActorRecord;
import com.cisco.spark.android.sync.ConversationAvatarContentReference;
import com.cisco.spark.android.sync.ConversationContract;
import com.cisco.spark.android.sync.TitleBuilder;
import com.cisco.spark.android.util.CryptoUtils;
import com.cisco.spark.android.util.Strings;
import com.cisco.spark.android.util.TriStateUtil;
import com.cisco.spark.android.util.UriUtils;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.github.benoitdion.ln.Ln;
import com.google.gson.Gson;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import static com.cisco.spark.android.sync.ConversationContract.ActivityEntry;
import static com.cisco.spark.android.sync.ConversationContract.vw_Conversation;

public class ConversationResolver {

    // icons in ConversationIconLayout
    private static final int CONVERSATION_ICON_MAX_AVATARS = 2;
    public final static int MAX_TOP_PARTICIPANTS = Math.max(TitleBuilder.DEFAULT_TITLE_MAX_NAMES, CONVERSATION_ICON_MAX_AVATARS);

    @Inject
    Gson gson;
    @Inject
    DeviceRegistration deviceRegistration;
    @Inject
    Context context;

    private final CursorValues cursorValues = new CursorValues();
    private boolean existsInDb = true;
    private List<ActorRecord> topParticipants;

    public Uri getLastActivityKeyUri() {
        return cursorValues.getUri(vw_Conversation.ACTIVITY_ENCRYPTION_KEY_URL);
    }

    public boolean isLastActivityEncrypted() {
        return cursorValues.getBoolean(vw_Conversation.ACTIVITY_IS_ENCRYPTED);
    }

    public void setLastActivityEncrypted(boolean isEncrypted) {
        cursorValues.put(vw_Conversation.ACTIVITY_IS_ENCRYPTED, isEncrypted ? 1L : 0L);
    }

    // Rather than keep fields for each value, we build up nv pairs based on the cursor that
    // is passed in. This allows us to make no assumptions about the contents of the cursor
    // and keeps boilerplate code to a minimum. This class stores the nv pairs.
    protected class CursorValues extends HashMap<vw_Conversation, Object> {
        protected Uri getUri(vw_Conversation col) {
            if (containsKey(col))
                return (Uri) get(col);
            return null;
        }

        protected long getLong(vw_Conversation col) {
            Object ret = get(col);
            if (ret != null)
                return (long) ret;
            return 0;
        }

        protected String getString(vw_Conversation col) {
            return (String) get(col);
        }

        protected boolean getBoolean(vw_Conversation col) {
            return getLong(col) > 0;
        }

        protected Boolean getTristateBoolean(vw_Conversation col) {
            Object ret = get(col);
            if (ret != null)
                return getLong(col) > 0;
            return null;
        }


        public int getInt(vw_Conversation col) {
            return (int) getLong(col);
        }
    }

    protected ConversationResolver(Injector injector) {
        injector.inject(this);
    }

    public ConversationResolver(Injector injector, Cursor cursor) {
        this(injector);
        initializeFromCursor(cursor);
    }

    protected void initializeFromCursor(Cursor cursor) {
        for (vw_Conversation col : vw_Conversation.values()) {
            int idx = cursor.getColumnIndex(col.name());
            if (idx >= 0) {
                putColumn(cursor, col, idx);
            }
        }
        existsInDb = true;
    }

    private void putColumn(Cursor cursor, vw_Conversation col, int colIndex) {

        switch (col) {
            // uri values
            case ACTIVITY_ENCRYPTION_KEY_URL:
            case DEFAULT_ENCRYPTION_KEY_URL:
            case LOCUS_URL:
            case SPACE_URL:
            case SPACE_URL_HIDDEN:
            case TITLE_ENCRYPTION_KEY_URL:
            case CONVERSATION_AVATAR_ENCRYPTION_KEY_URL:
            case URL:
            case RETENTION_URL:
                cursorValues.put(col, UriUtils.parseIfNotNull(cursor.getString(colIndex)));
                break;

            // boolean values
            case PMR:
            case ACTIVITY_IS_ENCRYPTED:
            case FAVORITE:
            case HIDDEN:
            case IN_ACTIVE_CALL:
            case IS_TITLE_ENCRYPTED:
            case LOCKED:
            case MUTED:
            case ARCHIVED:
            case IS_CONVERSATION_AVATAR_ENCRYPTED:
            case IS_TEAM_GUEST:
                if (!cursor.isNull(colIndex) && cursor.getInt(colIndex) > 0)
                    cursorValues.put(col, (long) 1);
                else
                    cursorValues.put(col, (long) 0);
                break;

            // tri-state boolean values (not set(null), false(0), true(1))
            case MESSAGE_NOTIFICATIONS:
            case MENTION_NOTIFICATIONS:
                if (!cursor.isNull(colIndex)) {
                    if (cursor.getInt(colIndex) > 0)
                        cursorValues.put(col, (long) 1);
                    else
                        cursorValues.put(col, (long) 0);
                } else {
                    cursorValues.remove(col);
                }
                break;

            case ACTIVITY_TYPE:
                ActivityEntry.Type activityType = ActivityEntry.Type._UNKNOWN;
                if (!cursor.isNull(colIndex)) {
                    activityType = ActivityEntry.Type.values()[cursor.getInt(colIndex)];
                }
                cursorValues.put(col, activityType);
                break;

            // All the rest are plain old strings and integers
            default:
                if (ConversationContract.INTEGER.equals(col.datatype())) {
                    cursorValues.put(col, cursor.getLong(colIndex));
                } else {
                    cursorValues.put(col, cursor.getString(colIndex));
                }
        }
    }

    public ActorRecord.ActorKey getLastActivityActorKey() {
        return Activity.getActorKeyFromJson(gson, getLastActivityData(), getLastActivityType());
    }

    public List<ActorRecord> getTopParticipants() {
        if (topParticipants != null)
            return topParticipants;

        String json = getTopParticipantsJson();

        if (TextUtils.isEmpty(json))
            return Collections.emptyList();

        topParticipants = ConversationContract.ConversationEntry.getTopParticipants(gson, json);
        return topParticipants;
    }

    // Useful for grabbing the actor record of the other person in a one on one
    public ActorRecord getFirstNonSelfParticipant(AuthenticatedUser self) {
        for (ActorRecord actorRecord : getTopParticipants()) {
            if (!actorRecord.isAuthenticatedUser(self) && !Person.LYRA_SPACE.equals(actorRecord.getType()))
                return actorRecord;
        }
        return null;
    }

    public ActivityEntry.Type getLastActivityType() {
        if (cursorValues.containsKey(vw_Conversation.ACTIVITY_TYPE))
            return (ActivityEntry.Type) cursorValues.get(vw_Conversation.ACTIVITY_TYPE);
        return ActivityEntry.Type._UNKNOWN;
    }

    public @Nullable String getId() {
        return cursorValues.getString(vw_Conversation.CONVERSATION_ID);
    }

    public String getAclUrl() {
        return cursorValues.getString(vw_Conversation.ACL_URL);
    }

    /**
     * Get the title if it has been set. Note this does not use the TitleBuilder, for that see
     * getDisplayName()
     *
     * @param keyManager                      to decrypt (or begin the process of decrypting) the
     *                                        title if needed
     * @param encryptionDurationMetricManager Optional, for metrics. Only used if async decryption
     *                                        is needed. Can be null.
     * @return The room's title. If the title is encrypted and the key is in memory, it will be
     * decrypted before returning. If the title is encrypted and the key is NOT in memory, return
     * null and kick off an operation to decrypt it for next time.
     */
    public String getTitle(KeyManager keyManager, EncryptionDurationMetricManager encryptionDurationMetricManager) {
        String title = cursorValues.getString(vw_Conversation.TITLE);
        if ((!isTitleEncrypted() && !CryptoUtils.looksLikeCipherText(title)) || TextUtils.isEmpty(title)) {
            return title;
        }

        if (keyManager == null)
            return null;

        String ret = decryptTitle(title, getTitleKeyUrl(), keyManager, encryptionDurationMetricManager);
        if (encryptionDurationMetricManager != null)
            encryptionDurationMetricManager.onTitleDisplayed(getId(), getTitleKeyUrl(), TextUtils.isEmpty(ret));
        return ret;
    }

    private String decryptTitle(String title, Uri titleKeyUrl, KeyManager keyManager, EncryptionDurationMetricManager encryptionDurationMetricManager) {
        if (isTitleEncrypted()) {
            KeyObject titleKeyObject = null;

            if (keyManager != null)
                titleKeyObject = keyManager.getCachedBoundKey(titleKeyUrl);

            if (titleKeyObject != null) {
                try {
                    return CryptoUtils.decryptFromJwe(new KeyObject(titleKeyUrl, titleKeyObject.getKey(), titleKeyObject.getKeyId()), title);
                } catch (Exception e) {
                    Ln.w(e, "Failed decrypting title");
                    if (encryptionDurationMetricManager != null)
                        encryptionDurationMetricManager.onError(400, EncryptionMetrics.ERROR_DECRYPT_FAILED);
                }
            }
            return null;
        }

        return title;
    }

    public String getDisplayName() {
        return cursorValues.getString(vw_Conversation.CONVERSATION_DISPLAY_NAME);
    }

    public boolean isRead() {
        // this avoids marking things unread incorrectly during initial sync
        if (TextUtils.isEmpty(getLastActivityData()))
            return true;

        return getLastReadableActivityTimestamp() <= getLastSeenActivityTimestamp();
    }

    public long getLastReadableActivityTimestamp() {
        return cursorValues.getLong(vw_Conversation.TIMESTAMP_LAST_READABLE_ACTIVITY_REMOTE);
    }

    public String getLastActivityId() {
        return cursorValues.getString(vw_Conversation.LAST_ACTIVITY_ID);
    }

    public long getLastSeenActivityTimestamp() {
        return cursorValues.getLong(vw_Conversation.TIMESTAMP_LAST_SEEN_ACTIVITY_REMOTE);
    }

    public boolean amILastParticipant() {
        return getParticipantCount() == 1;
    }

    public long getLastActivityPublishTime() {
        return cursorValues.getLong(vw_Conversation.ACTIVITY_PUBLISHED_TIME);
    }

    public boolean isMuted() {
        return cursorValues.getBoolean(vw_Conversation.MUTED);
    }

    public boolean hasCustomNotificationOverrides() {
        Boolean messageNotifications = cursorValues.getTristateBoolean(vw_Conversation.MESSAGE_NOTIFICATIONS);
        Boolean mentionNotifications = cursorValues.getTristateBoolean(vw_Conversation.MENTION_NOTIFICATIONS);
        return TriStateUtil.isSet(messageNotifications) || TriStateUtil.isSet(mentionNotifications);
    }

    public Boolean isMessageNotificationsEnabled() {
        return cursorValues.getTristateBoolean(vw_Conversation.MESSAGE_NOTIFICATIONS);
    }

    public Boolean isMentionNotificationsEnabled() {
        return cursorValues.getTristateBoolean(vw_Conversation.MENTION_NOTIFICATIONS);
    }

    public boolean isFavorite() {
        return cursorValues.getBoolean(vw_Conversation.FAVORITE);
    }

    public boolean isHidden() {
        return cursorValues.getBoolean(vw_Conversation.HIDDEN);
    }

    public boolean isLocked() {
        return cursorValues.getBoolean(vw_Conversation.LOCKED);
    }

    public boolean isTeamConversation() {
        return deviceRegistration.getFeatures().areTeamsEnabled() && !TextUtils.isEmpty(getParentTeamId());
    }

    public boolean isTeamGuest() {
        return cursorValues.getBoolean(vw_Conversation.IS_TEAM_GUEST);
    }

    public boolean isPrimaryTeamConversation() {
        return isTeamConversation() && getId().equals(getPrimaryTeamConversationId());
    }

    public boolean isJoined() {
        return cursorValues.getBoolean(vw_Conversation.SELF_JOINED);
    }

    public boolean isArchived() {
        return cursorValues.getBoolean(vw_Conversation.ARCHIVED);
    }

    public Uri getUrl() {
        return cursorValues.getUri(vw_Conversation.URL);
    }

    public Uri getSpaceUrl() {
        return cursorValues.getUri(vw_Conversation.SPACE_URL);
    }

    public Uri getSpaceUrlHidden() {
        return cursorValues.getUri(vw_Conversation.SPACE_URL_HIDDEN);
    }

    public String getSyncOperationId() {
        return cursorValues.getString(vw_Conversation.SYNC_OPERATION_ID);
    }

    public boolean isOneOnOne() {
        return !TextUtils.isEmpty(getOneOnOneParticipant());
    }

    public boolean isGroup() {
        return !isOneOnOne();
    }

    public Uri getLocusUrl() {
        return cursorValues.getUri(vw_Conversation.LOCUS_URL);
    }

    public Uri getDefaultEncryptionKeyUrl() {
        return cursorValues.getUri(vw_Conversation.DEFAULT_ENCRYPTION_KEY_URL);
    }

    public int getParticipantCount() {
        return cursorValues.getInt(vw_Conversation.PARTICIPANT_COUNT);
    }

    public boolean exists() {
        return existsInDb;
    }

    public String getLastActivityData() {
        return cursorValues.getString(vw_Conversation.ACTIVITY_DATA);
    }

    public boolean isProvisional() {
        return TextUtils.equals(getId(), getSyncOperationId());
    }

    public boolean isMixed() {
        return getExternalParticipantCount() > 0;
    }

    public boolean hasAvatar() {
        return deviceRegistration.getFeatures().hasCustomRoomAvatarsEnabled() && !TextUtils.isEmpty(cursorValues.getString(vw_Conversation.CONVERSATION_AVATAR_CONTENT_REFERENCE));
    }

    public boolean isAvatarEncrypted() {
        return hasAvatar() && cursorValues.getBoolean(vw_Conversation.IS_CONVERSATION_AVATAR_ENCRYPTED);
    }

    public ConversationAvatarContentReference getAvatarContentReference() {
        return gson.fromJson(cursorValues.getString(vw_Conversation.CONVERSATION_AVATAR_CONTENT_REFERENCE), ConversationAvatarContentReference.class);
    }

    public boolean isTitleEncrypted() {
        return cursorValues.getBoolean(vw_Conversation.IS_TITLE_ENCRYPTED);
    }

    public boolean hasTitle() {
        return cursorValues.getString(vw_Conversation.TITLE) != null && !TextUtils.isEmpty(cursorValues.getString(vw_Conversation.TITLE));
    }

    public boolean isCustomNotificationsSet() {
        final Boolean messagesOverride = isMessageNotificationsEnabled();
        final Boolean mentionsOverride = isMentionNotificationsEnabled();

        final boolean messagesSet = messagesOverride != null;
        final boolean mentionsSet = mentionsOverride != null;

        return messagesSet || mentionsSet;
    }

    public
    @ColorInt
    int getTeamBackgroundColor() {
        String hexValue = cursorValues.getString(vw_Conversation.TEAM_COLOR);
        if (hexValue == null) {
            Ln.i("Team %s has no team color defined, returning default instead", cursorValues.getString(vw_Conversation.TEAM_ID));
            return Team.getDefaultTeamColor(context);
        } else {
            if ("#FFFFFF".equals(hexValue) || (hexValue.length() > 6 && "#00".equals(hexValue.substring(0, 2)))) {
                Ln.i("Team %s has a questionable team color: " + hexValue);
            }
            return Color.parseColor(hexValue);
        }
    }

    public boolean isTeamDisplayNameEncrypted() {
        return cursorValues.getInt(vw_Conversation.IS_TEAM_DISPLAY_NAME_ENCRYPTED) == 1;
    }

    public String getTeamDisplayName() {
        return cursorValues.getString(vw_Conversation.TEAM_DISPLAY_NAME);
    }

    public String getSummary() {
        return cursorValues.getString(vw_Conversation.SUMMARY);
    }

    public String getParentTeamId() {
        return cursorValues.getString(vw_Conversation.TEAM_ID);
    }

    public String getTeamIconLabel() {
        if (isTitleEncrypted() || TextUtils.isEmpty(getDisplayName())) {
            return null;
        }

        if (getPrimaryTeamConversationId() != null && getPrimaryTeamConversationId().equals(getId())) {
            return context.getString(R.string.general).toUpperCase(Locale.getDefault()).substring(0, 1);
        }

        String displayName = getDisplayName().toUpperCase(Locale.getDefault());

        // Emojis are surrogate pairs, so if the first two characters are a surrogate pair, use both instead of just one
        if (displayName.length() > 1 && Character.isSurrogatePair(displayName.charAt(0), displayName.charAt(1))) {
            return displayName.substring(0, 2);
        } else {
            return displayName.substring(0, 1);
        }
    }

    public String getIconLabel() {
        return getTeamIconLabel();
    }

    public String getPrimaryTeamConversationId() {
        return cursorValues.getString(vw_Conversation.PRIMARY_TEAM_CONVERSATION_ID);
    }

    public int getExternalParticipantCount() {
        return cursorValues.getInt(vw_Conversation.EXTERNAL_PARTICIPANT_COUNT);
    }

    public String getOneOnOneParticipant() {
        return cursorValues.getString(vw_Conversation.ONE_ON_ONE_PARTICIPANT);
    }

    public Uri getTitleKeyUrl() {
        return cursorValues.getUri(vw_Conversation.TITLE_ENCRYPTION_KEY_URL);
    }

    public void setConversationId(String id) {
        cursorValues.put(vw_Conversation.CONVERSATION_ID, id);
    }

    public void setConversationUrl(Uri conversationUrl) {
        cursorValues.put(vw_Conversation.URL, conversationUrl);
    }

    private String getTopParticipantsJson() {
        return cursorValues.getString(vw_Conversation.TOP_PARTICIPANTS);
    }

    public String getCreatorUuid() {
        return cursorValues.getString(vw_Conversation.CREATOR_UUID);
    }

    public String getKmsResourceObjectUrl() {
        return cursorValues.getString(vw_Conversation.KMS_RESOURCE_OBJECT_URI);
    }

    public void setLocusUrl(Uri locusUrl) {
        cursorValues.put(vw_Conversation.LOCUS_URL, locusUrl);
    }

    public boolean isParticipantListComplete() {
        return cursorValues.getBoolean(vw_Conversation.IS_PARTICIPANT_LIST_VALID);
    }

    public String getCustodianOrgId() {
        return cursorValues.getString(vw_Conversation.CUSTODIAN_ORG_ID);

    }

    public String getCustodianOrgName() {
        return cursorValues.getString(vw_Conversation.CUSTODIAN_ORG_NAME);
    }

    public Uri getRetentionUrl() {
        return cursorValues.getUri(vw_Conversation.RETENTION_URL);
    }

    public void setExistsInDb(boolean existsInDb) {
        this.existsInDb = existsInDb;
    }

    public int getBindState() {
        return cursorValues.getInt(vw_Conversation.BINDING_STATE);
    }

    public String toString() {
        return (isOneOnOne() ? Strings.md5(getOneOnOneParticipant()) : "group") + getId();
    }
}
