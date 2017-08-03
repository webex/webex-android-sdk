package com.cisco.spark.android.sync;

import android.content.ContentProviderOperation;
import android.net.Uri;

import com.cisco.spark.android.locus.model.LocusKey;
import com.cisco.spark.android.model.RetentionPolicy;
import com.cisco.spark.android.sync.ConversationContract.ActivityEntry;
import com.cisco.spark.android.sync.ConversationContract.ContentSearchDataEntry;
import com.cisco.spark.android.sync.ConversationContract.ConversationEntry;
import com.cisco.spark.android.sync.ConversationContract.ConversationSearchDataEntry;
import com.cisco.spark.android.sync.ConversationContract.MessageSearchDataEntry;
import com.cisco.spark.android.sync.ConversationContract.OrganizationEntry;
import com.github.benoitdion.ln.Ln;

import java.util.ArrayList;

public class ConversationContentProviderOperation {

    // calculate the published days of each activity
    private static final String CAST_ACTIVITY_PUBLISHED_DAYS_STATEMENT =
            "CAST(JULIANDAY(DATETIME('now')) - JULIANDAY(DATETIME(" +
                    ActivityEntry.TABLE_NAME + "." + "ACTIVITY_PUBLISHED_TIME/1000, 'UNIXEPOCH', 'LOCALTIME')) AS INTEGER)";
    // we delete 7 more days as buffer when purge activity data
    public static final int ACTIVITY_PRUNING_BUFFER_DAYS = 7;

    private ConversationContentProviderOperation() {
    }

    public static ArrayList<ContentProviderOperation> emptyDatabase() {
        ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
        for (Uri uri : DBHelperUtils.getContentUris()) {
            operations.add(ContentProviderOperation.newDelete(uri).build());
        }
        return operations;
    }

    public static ContentProviderOperation deleteConversation(String conversationId) {
        return ContentProviderOperation.newDelete(Uri.withAppendedPath(ConversationEntry.CONTENT_URI, conversationId))
                .build();
    }

    public static ContentProviderOperation setConversationJoined(String conversationId) {
        return setConversationJoinStatus(conversationId, true);
    }

    public static ContentProviderOperation setConversationUnjoined(String conversationId) {
        return setConversationJoinStatus(conversationId, false);
    }

    public static ContentProviderOperation setConversationJoinStatus(String conversationId, boolean joined) {
        return ContentProviderOperation.newUpdate(ConversationEntry.CONTENT_URI)
                .withValue(ConversationEntry.SELF_JOINED.name(), joined ? "1" : "0")
                .withSelection(ConversationEntry.CONVERSATION_ID + " =? ", new String[]{conversationId})
                .build();
    }

    public static ContentProviderOperation clearConversationActivities(String conversationId) {
        return ContentProviderOperation.newDelete(ActivityEntry.CONTENT_URI)
                .withSelection(ActivityEntry.CONVERSATION_ID.name() + " =? ", new String[]{conversationId})
                .build();
    }

    public static ContentProviderOperation clearConversationParticipantEntries(String conversationId) {
        return ContentProviderOperation.newDelete(ConversationContract.ParticipantEntry.CONTENT_URI)
                .withSelection(ConversationContract.ParticipantEntry.CONVERSATION_ID.name() + " =? ", new String[]{conversationId})
                .build();
    }

    public static ContentProviderOperation insertEncryptionKey(String keyUri, String key, String keyId, long expirationTime) {
        return ContentProviderOperation.newInsert(ConversationContract.EncryptionKeyEntry.CONTENT_URI)
                .withValue(ConversationContract.EncryptionKeyEntry.ENCRYPTION_KEY_URI.name(), keyUri)
                .withValue(ConversationContract.EncryptionKeyEntry.ENCRYPTION_KEY.name(), key)
                .withValue(ConversationContract.EncryptionKeyEntry.ENCRYPTION_KEY_ID.name(), keyId)
                .withValue(ConversationContract.EncryptionKeyEntry.ENCRYPTION_KEY_EXPIRY_TIME.name(), expirationTime)
                .build();
    }

    public static ContentProviderOperation updateEncryptionKey(String defaultEncryptionKeyUrl, String defaultEncryptionKey, String defaultEncryptionKeyId) {
        return ContentProviderOperation.newUpdate(ConversationContract.EncryptionKeyEntry.CONTENT_URI)
                .withSelection(ConversationContract.EncryptionKeyEntry.ENCRYPTION_KEY_URI + " =? AND " + ConversationContract.EncryptionKeyEntry.ENCRYPTION_KEY + " IS NULL", new String[]{defaultEncryptionKeyUrl})
                .withValue(ConversationContract.EncryptionKeyEntry.ENCRYPTION_KEY.name(), defaultEncryptionKey)
                .withValue(ConversationContract.EncryptionKeyEntry.ENCRYPTION_KEY_ID.name(), defaultEncryptionKeyId)
                .build();
    }

    public static ContentProviderOperation clearActivityEncryptedFlag(String activityId, String activityData) {
        return ContentProviderOperation.newUpdate(ActivityEntry.CONTENT_URI)
                .withSelection(ActivityEntry.ACTIVITY_ID + "=?", new String[]{activityId})
                .withValue(ActivityEntry.IS_ENCRYPTED.name(), 0)
                .withValue(ActivityEntry.ACTIVITY_DATA.name(), activityData)
                .build();
    }

    public static ContentProviderOperation clearActivityEncryptedFlag(long id, String activityData) {
        return ContentProviderOperation.newUpdate(ActivityEntry.CONTENT_URI)
                .withSelection(ActivityEntry._ID + "=?", new String[]{String.valueOf(id)})
                .withValue(ActivityEntry.IS_ENCRYPTED.name(), 0)
                .withValue(ActivityEntry.ACTIVITY_DATA.name(), activityData)
                .build();
    }

    public static ContentProviderOperation clearEncryptionKey(String encryptionKeyUri) {
        return ContentProviderOperation.newUpdate(ConversationContract.EncryptionKeyEntry.CONTENT_URI)
                .withSelection(ConversationContract.EncryptionKeyEntry.ENCRYPTION_KEY_URI + " =? ", new String[]{encryptionKeyUri})
                .withValue(ConversationContract.EncryptionKeyEntry.ENCRYPTION_KEY.name(), null)
                .withValue(ConversationContract.EncryptionKeyEntry.ENCRYPTION_KEY_ID.name(), null)
                .build();
    }

    public static ContentProviderOperation deleteConversationSearch(String conversationId) {
        return ContentProviderOperation.newDelete(Uri.withAppendedPath(ConversationSearchDataEntry.CONTENT_URI, conversationId))
                .build();
    }

    public static ContentProviderOperation updateConversationInActiveCall(LocusKey locusKey, boolean value) {
        return ContentProviderOperation.newUpdate(ConversationEntry.CONTENT_URI)
                .withSelection(ConversationEntry.LOCUS_URL + "=?", new String[]{locusKey.toString()})
                .withValue(ConversationEntry.IN_ACTIVE_CALL.name(), value)
                .build();
    }

    public static ContentProviderOperation clearBindingState() {
        Ln.i("ContentProviderOperation.clearBindingState to clear conversation bind state...");
        return ContentProviderOperation.newUpdate(ConversationEntry.CONTENT_URI)
                .withValue(ConversationEntry.BINDING_STATE.name(), 0)
                .build();
    }

    public static ContentProviderOperation updateBindingState(String conversationId, int value) {
        Ln.i("ContentProviderOperation.updateBindingState to update conversation bind state, conversationId = " + conversationId + ", value = " + value);
        return ContentProviderOperation.newUpdate(ConversationEntry.CONTENT_URI)
                .withSelection(ConversationEntry.CONVERSATION_ID + "=?", new String[]{conversationId})
                .withValue(ConversationEntry.BINDING_STATE.name(), value)
                .build();
    }

    public static ContentProviderOperation clearConversationInActiveCall() {
        return ContentProviderOperation.newUpdate(ConversationEntry.CONTENT_URI)
                .withValue(ConversationEntry.IN_ACTIVE_CALL.name(), 0)
                .build();
    }

    public static ContentProviderOperation updateConversationLocusUrl(Uri conversationUrl, LocusKey locusKey) {
        return ContentProviderOperation.newUpdate(ConversationEntry.CONTENT_URI)
                .withSelection(ConversationEntry.URL + "=?", new String[]{conversationUrl.toString()})
                .withValue(ConversationEntry.LOCUS_URL.name(), locusKey.toString())
                .build();
    }

    public static ContentProviderOperation updateConversationSearchEntryTitle(String title, String conversationId) {
        return ContentProviderOperation.newUpdate(ConversationSearchDataEntry.CONTENT_URI)
                .withSelection(ConversationSearchDataEntry.suggest_intent_data.name() + "=?", new String[]{conversationId})
                .withValue(ConversationSearchDataEntry.CONVERSATION_NAME.name(), title)
                .build();
    }


    public static ContentProviderOperation insertMessageSearchActivity(String conversationId, String messageText, String postedBy, String postedByUUID, String activityId, long messagePublishedTime, String conversationName, ActivityEntry.Type type, int fileSharedCount, String oneOnOneParticipant) {
        return ContentProviderOperation.newInsert(MessageSearchDataEntry.CONTENT_URI)
                .withValue(MessageSearchDataEntry.MESSAGE_TEXT.name(), messageText)
                .withValue(MessageSearchDataEntry.suggest_text_1.name(), messageText)
                .withValue(MessageSearchDataEntry.suggest_intent_data.name(), conversationId)
                .withValue(MessageSearchDataEntry.ACTIVITY_ID.name(), activityId)
                .withValue(MessageSearchDataEntry.MESSAGE_POSTED_BY.name(), postedBy)
                .withValue(MessageSearchDataEntry.MESSAGE_POSTED_BY_UUID.name(), postedByUUID)
                .withValue(MessageSearchDataEntry.MESSAGE_PUBLISHED_TIME.name(), messagePublishedTime)
                .withValue(MessageSearchDataEntry.CONVERSATION_NAME.name(), conversationName)
                .withValue(MessageSearchDataEntry.suggest_intent_extra_data.name(), type.name())
                .withValue(MessageSearchDataEntry.FILES_SHARED_COUNT.name(), fileSharedCount)
                .withValue(MessageSearchDataEntry.ONE_ON_ONE_PARTICIPANT.name(), oneOnOneParticipant)
                .withYieldAllowed(true)
                .build();
    }

    public static ContentProviderOperation updateMessageSearchActivity(String activityId, String conversationId, String messageText, String postedBy, String postedByUUID, long messagePublishedTime, String conversationName, ActivityEntry.Type type, int fileSharedCount, String oneOnOneParticipant) {
        return ContentProviderOperation.newUpdate(MessageSearchDataEntry.CONTENT_URI)
                .withSelection(MessageSearchDataEntry.ACTIVITY_ID + "=? AND " + MessageSearchDataEntry.CONVERSATION_NAME + "!=?", new String[]{activityId, conversationName})
                .withValue(MessageSearchDataEntry.MESSAGE_TEXT.name(), messageText)
                .withValue(MessageSearchDataEntry.suggest_text_1.name(), messageText)
                .withValue(MessageSearchDataEntry.suggest_intent_data.name(), conversationId)
                .withValue(MessageSearchDataEntry.MESSAGE_POSTED_BY.name(), postedBy)
                .withValue(MessageSearchDataEntry.MESSAGE_POSTED_BY_UUID.name(), postedByUUID)
                .withValue(MessageSearchDataEntry.MESSAGE_PUBLISHED_TIME.name(), messagePublishedTime)
                .withValue(MessageSearchDataEntry.CONVERSATION_NAME.name(), conversationName)
                .withValue(MessageSearchDataEntry.suggest_intent_extra_data.name(), type.name())
                .withValue(MessageSearchDataEntry.FILES_SHARED_COUNT.name(), fileSharedCount)
                .withValue(MessageSearchDataEntry.ONE_ON_ONE_PARTICIPANT.name(), oneOnOneParticipant)
                .build();
    }

    public static ContentProviderOperation deleteMessageSearchActivityWithTempID(String tempId) {
        return ContentProviderOperation.newDelete(MessageSearchDataEntry.CONTENT_URI)
                .withSelection(MessageSearchDataEntry.ACTIVITY_ID + "= ?", new String[]{tempId})
                .build();
    }

    public static ContentProviderOperation deleteContentSearchActivityWithTempID(String tempId) {
        return ContentProviderOperation.newDelete(ContentSearchDataEntry.CONTENT_URI)
                .withSelection(ContentSearchDataEntry.ACTIVITY_ID + "= ?", new String[]{tempId})
                .build();
    }

    public static ContentProviderOperation deleteGroupSpaceActivitiesBeyondRetentionDate() {
        return ContentProviderOperation.newDelete(ActivityEntry.CONTENT_URI)
                .withSelection(ConversationContract.fqname(ActivityEntry.ACTIVITY_TYPE) + "!=" + ActivityEntry.Type.CREATE_CONVERSATION.ordinal() +
                        " AND " + ConversationContract.fqname(ActivityEntry.CONVERSATION_ID) + " IN (" +
                        " SELECT " + ConversationContract.fqname(ConversationEntry.CONVERSATION_ID) +
                        " FROM " + ConversationEntry.TABLE_NAME +
                        " WHERE " + ConversationContract.fqname(ActivityEntry.CONVERSATION_ID) + "=" + ConversationContract.fqname(ConversationEntry.CONVERSATION_ID) +
                        " AND " + ConversationContract.fqname(ConversationEntry.ONE_ON_ONE_PARTICIPANT) + " IS NULL" +
                        " AND " + ConversationContract.fqname(ConversationEntry.RETENTION_DAYS) + ">0" +
                        " AND " + CAST_ACTIVITY_PUBLISHED_DAYS_STATEMENT + "+?>" + ConversationContract.fqname(ConversationEntry.RETENTION_DAYS) +
                        " )", new String[]{String.valueOf(ACTIVITY_PRUNING_BUFFER_DAYS)})
                .build();
    }

    public static ContentProviderOperation deleteActivityWithId(String activityId) {
        return ContentProviderOperation.newDelete(ActivityEntry.CONTENT_URI)
                .withSelection(ActivityEntry.ACTIVITY_ID + "= ?", new String[]{activityId})
                .build();
    }

    public static ContentProviderOperation updateRetentionPolicyWithOrgId(String orgId, RetentionPolicy retentionPolicy) {
        return ContentProviderOperation.newUpdate(OrganizationEntry.CONTENT_URI)
                .withSelection(OrganizationEntry.ORG_ID + "=?", new String[]{String.valueOf(orgId)})
                .withValue(OrganizationEntry.RETENTION_URL.name(), retentionPolicy.getRetentionUrl())
                .withValue(OrganizationEntry.RETENTION_DAYS.name(), retentionPolicy.getRetentionDays())
                .withValue(OrganizationEntry.LAST_RETENTION_SYNC_TIMESTAMP.name(), System.currentTimeMillis())
                .build();
    }

    public static ContentProviderOperation updateConversationRetentionPolicy(RetentionPolicy retentionPolicy) {
        return ContentProviderOperation.newUpdate(ConversationEntry.CONTENT_URI)
                .withSelection(ConversationEntry.RETENTION_URL + "=?", new String[]{String.valueOf(retentionPolicy.getRetentionUrl())})
                .withValue(ConversationEntry.RETENTION_DAYS.name(), retentionPolicy.getRetentionDays())
                .withValue(ConversationEntry.LAST_RETENTION_SYNC_TIMESTAMP.name(), System.currentTimeMillis())
                .build();
    }

}
