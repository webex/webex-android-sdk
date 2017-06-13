package com.cisco.spark.android.sync;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import com.cisco.spark.android.core.AuthenticatedUser;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.model.Activity;
import com.cisco.spark.android.model.ActivityReference;
import com.cisco.spark.android.model.KeyObject;
import com.cisco.spark.android.stickies.Sticky;
import com.cisco.spark.android.stickies.StickyPad;
import com.cisco.spark.android.sync.ConversationContract.ActivityEntry;
import com.cisco.spark.android.sync.ConversationContract.ParticipantEntry;
import com.cisco.spark.android.ui.conversation.ConversationResolver;
import com.cisco.spark.android.util.CollectionUtils;
import com.cisco.spark.android.util.DateUtils;
import com.cisco.spark.android.util.Strings;
import com.cisco.spark.android.util.UriUtils;
import com.github.benoitdion.ln.Ln;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.cisco.spark.android.sync.ConversationContract.ConversationEntry;
import static com.cisco.spark.android.sync.ConversationContract.DbColumn;

public class ConversationContentProviderQueries {
    private ConversationContentProviderQueries() {
    }

    public static ActivityReference getFirstNonLocalActivity(ContentResolver contentResolver, String conversationId) {
        String where = ActivityEntry.CONVERSATION_ID + " =? "
                + " AND " + ActivityEntry.SOURCE + " !=? ";
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(ActivityEntry.CONTENT_URI,
                    ActivityEntry.DEFAULT_PROJECTION,
                    where,
                    new String[]{conversationId, String.valueOf(ActivityEntry.Source.LOCAL.ordinal())},
                    ActivityEntry.ACTIVITY_PUBLISHED_TIME + " ASC LIMIT 1"
            );
            if (cursor != null && cursor.moveToFirst()) {
                return new ActivityReference(cursor);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    public static ActivityReference getLastAckableActivityBeforeTime(ContentResolver contentResolver, String conversationId, long time) {
        // If an activity will mark a conv as unread, treat it as ackable
        ArrayList<String> typeList = new ArrayList<>();
        for (ActivityEntry.Type type : ActivityEntry.Type.values()) {
            if (Activity.isTypeAckable(type))
                typeList.add(String.valueOf(type.ordinal()));
        }
        // CallSession doesn't integrate with the Activity.should* methods and implements ConversationItem instead, manually add it here.
        typeList.add(String.valueOf(ActivityEntry.Type.CALL_SESSION.ordinal()));

        String where = ActivityEntry.ACTIVITY_TYPE + " !=? "
                + " AND " + ActivityEntry.CONVERSATION_ID + " =? "
                + " AND (" + ActivityEntry.ACTIVITY_ID + " != " + ActivityEntry.SYNC_OPERATION_ID
                + " OR " + ActivityEntry.SYNC_OPERATION_ID + " IS NULL)"
                + " AND " + ActivityEntry.ACTIVITY_PUBLISHED_TIME + " <? "
                + " AND " + ActivityEntry.ACTIVITY_TYPE + " IN (" + Strings.join(",", typeList) + ")";

        Cursor cursor = null;
        try {
            cursor = contentResolver.query(ActivityEntry.CONTENT_URI,
                    ActivityEntry.DEFAULT_PROJECTION,
                    where,
                    new String[]{Integer.toString(ActivityEntry.Type.ACK.ordinal()), conversationId, String.valueOf(time)},
                    ActivityEntry.ACTIVITY_PUBLISHED_TIME + " DESC LIMIT 1");
            if (cursor != null && cursor.moveToFirst())
                return new ActivityReference(cursor);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    public static ActivityReference getLastAckableActivity(ContentResolver contentResolver, String conversationId) {
        return getLastAckableActivityBeforeTime(contentResolver, conversationId, System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5));
    }

    public static ActivityReference getFirstForwardFillGapAfterTime(ContentResolver contentResolver, String conversationId, String timeInMsString) {
        String where = ActivityEntry.ACTIVITY_TYPE + " =? AND "
                + ActivityEntry.CONVERSATION_ID + " =? AND "
                + ActivityEntry.ACTIVITY_PUBLISHED_TIME + " >?";
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(ActivityEntry.CONTENT_URI,
                    ActivityEntry.DEFAULT_PROJECTION,
                    where,
                    new String[]{Integer.toString(ActivityEntry.Type.FORWARDFILL_GAP.ordinal()),
                            conversationId, String.valueOf(timeInMsString)},
                    ActivityEntry.ACTIVITY_PUBLISHED_TIME + " ASC LIMIT 1"
            );
            if (cursor != null && cursor.moveToFirst()) {
                return new ActivityReference(cursor);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    public static ActivityReference getLatestBackFillGapBeforeTime(ContentResolver contentResolver, String conversationId, String timeInMsString) {
        if (TextUtils.isEmpty(timeInMsString))
            return null;

        String where = ActivityEntry.ACTIVITY_TYPE + " =? AND "
                + ActivityEntry.CONVERSATION_ID + " =? AND "
                + ActivityEntry.ACTIVITY_PUBLISHED_TIME + " <?";
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(ActivityEntry.CONTENT_URI,
                    ActivityEntry.DEFAULT_PROJECTION,
                    where,
                    new String[]{Integer.toString(ActivityEntry.Type.BACKFILL_GAP.ordinal()),
                            conversationId, timeInMsString},
                    ActivityEntry.ACTIVITY_PUBLISHED_TIME + " DESC LIMIT 1"
            );
            if (cursor != null && cursor.moveToFirst()) {
                return new ActivityReference(cursor);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    public static ActivityReference getPreviousPreviewActivity(ContentResolver contentResolver, String conversationId, String currentActivityId) {
        ArrayList<String> typeList = new ArrayList<>();
        for (ActivityEntry.Type type : ActivityEntry.Type.values()) {
            if (Activity.shouldBecomeLastActivityPreview(type))
                typeList.add(String.valueOf(type.ordinal()));
        }
        // CallSession doesn't integrate with the Activity.should* methods and implements ConversationItem instead, manually add it here.
        typeList.add(String.valueOf(ActivityEntry.Type.CALL_SESSION.ordinal()));

        String where = ActivityEntry.ACTIVITY_TYPE + " !=? "
                + " AND " + ActivityEntry.CONVERSATION_ID + " =? "
                + " AND " + ActivityEntry.ACTIVITY_ID + " !=? "
                + " AND (" + ActivityEntry.ACTIVITY_ID + " != " + ActivityEntry.SYNC_OPERATION_ID
                + " OR " + ActivityEntry.SYNC_OPERATION_ID + " IS NULL)"
                + " AND " + ActivityEntry.ACTIVITY_TYPE + " IN (" + Strings.join(",", typeList) + ")";

        Cursor cursor = null;
        try {
            cursor = contentResolver.query(ActivityEntry.CONTENT_URI,
                    ActivityEntry.DEFAULT_PROJECTION,
                    where,
                    new String[]{Integer.toString(ActivityEntry.Type.ACK.ordinal()), conversationId, currentActivityId},
                    ActivityEntry.ACTIVITY_PUBLISHED_TIME + " DESC LIMIT 1");
            if (cursor != null && cursor.moveToFirst())
                return new ActivityReference(cursor);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    /**
     * If the conversation whose ID is passed into this function is a team conversation, this will
     * get the primary conversation ID for that team, otherwise it will return null
     *
     * @param contentResolver
     * @param conversationId
     * @return
     */
    public static String getTeamPrimaryConversationId(ContentResolver contentResolver, String conversationId) {
        return getOneValue(contentResolver, ConversationContract.vw_Conversation.PRIMARY_TEAM_CONVERSATION_ID,
                ConversationEntry.CONVERSATION_ID + "=? OR " + ConversationEntry.SYNC_OPERATION_ID + "=?",
                new String[]{conversationId, conversationId});
    }

    public static long getLastActivityPublished(ContentResolver contentResolver, String conversationId) {
        String where = ActivityEntry.ACTIVITY_PUBLISHED_TIME + " IS NOT NULL AND " + ActivityEntry.CONVERSATION_ID + " =?";
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(ActivityEntry.CONTENT_URI,
                    new String[]{"MAX (" + ActivityEntry.ACTIVITY_PUBLISHED_TIME.name() + ")"},
                    where,
                    new String[]{conversationId},
                    null);
            if (cursor != null && cursor.moveToFirst())
                if (!cursor.isNull(0))
                    return cursor.getLong(0);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return 0;
    }

    public static ActivityReference getLastReliableActivityBefore(ContentResolver contentResolver, String conversationId, long msTime) {
        String where = ActivityEntry.ACTIVITY_TYPE + " !=? AND "
                + ActivityEntry.CONVERSATION_ID + " =? AND "
                + ActivityEntry.ACTIVITY_PUBLISHED_TIME + " <? AND ("
                + ActivityEntry.SOURCE + "=? OR " + ActivityEntry.SOURCE + "=? )";
        Cursor cursor = null;

        try {
            cursor = contentResolver.query(ActivityEntry.CONTENT_URI,
                    ActivityEntry.DEFAULT_PROJECTION,
                    where,
                    new String[]{Integer.toString(ActivityEntry.Type.ACK.ordinal()),
                            conversationId, String.valueOf(msTime),
                            String.valueOf(ActivityEntry.Source.SYNC.ordinal()),
                            String.valueOf(ActivityEntry.Source.SYNC_PARTIAL.ordinal())},
                    ActivityEntry.ACTIVITY_PUBLISHED_TIME + " DESC LIMIT 1"
            );
            if (cursor != null && cursor.moveToFirst()) {
                return new ActivityReference(cursor);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    /**
     * Checks if the activity at time activityPublishTime has a gap just before and just after it in
     * the DB This function relies on the fact that if a gap is bordering an activity, the gap
     * timestamp is +- 1 ms from the activity's publish time
     *
     * @param contentResolver
     * @param conversationId
     * @param activityPublishTime
     * @return - True if there is a gap activity at activityPublishTime + 1 and activityPublishTime
     * - 1, false otherwise
     */
    public static boolean isActivityBorderedByGaps(ContentResolver contentResolver, String conversationId, long activityPublishTime) {
        String where = ActivityEntry.CONVERSATION_ID + " =? AND ( "
                + ActivityEntry.ACTIVITY_TYPE + " =? OR " + ActivityEntry.ACTIVITY_TYPE + " =? ) AND "
                + ActivityEntry.ACTIVITY_PUBLISHED_TIME + " <=? AND "
                + ActivityEntry.ACTIVITY_PUBLISHED_TIME + " >=? ";

        String[] args = new String[]{
                conversationId,
                String.valueOf(ActivityEntry.Type.BACKFILL_GAP.ordinal()),
                String.valueOf(ActivityEntry.Type.FORWARDFILL_GAP.ordinal()),
                String.valueOf(activityPublishTime + 1),
                String.valueOf(activityPublishTime - 1)
        };

        String count = getOneValue(contentResolver, ActivityEntry.CONTENT_URI, "COUNT(*)", where, args);

        int ret = Integer.parseInt(count);

        return ret == 2;
    }

    public static ActivityReference getActivityReference(ContentResolver contentResolver, String guid) {
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(Uri.withAppendedPath(ActivityEntry.CONTENT_URI, guid),
                    ActivityEntry.DEFAULT_PROJECTION,
                    null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                return new ActivityReference(cursor);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    /**
     *
     * @param contentResolver
     * @param conversationId
     * @return - Returns all messages within 24 hours of the timestamp of the last activity, with a maximum limit of 50
     */
    public static Cursor getMax50RecentMessagesFromConversation(ContentResolver contentResolver, String conversationId) {
        long lastActivityTimestamp = getLastActivityPublished(contentResolver, conversationId);

        Cursor cursor = null;

        cursor = contentResolver.query(ConversationEntry.getConversationActivitiesUri(conversationId),
                ActivityEntry.DEFAULT_PROJECTION,
                ActivityEntry.ACTIVITY_PUBLISHED_TIME + ">? AND " + ActivityEntry.ACTIVITY_TYPE + "=?",
                new String[]{Long.toString(lastActivityTimestamp - DateUtils.DAY_IN_MILLIS), Integer.toString(ActivityEntry.Type.MESSAGE.ordinal())},
                ConversationContract.ActivityEntry.ACTIVITY_PUBLISHED_TIME + " DESC LIMIT 50");
        if (cursor != null) {
            return cursor;
        }

        return null;
    }

    public static Uri getDefaultKeyUrlForConversation(ContentResolver contentResolver, String conversationId) {
        String strRet = getOneValue(contentResolver, ConversationEntry.DEFAULT_ENCRYPTION_KEY_URL,
                ConversationEntry.CONVERSATION_ID + "=? OR " + ConversationEntry.SYNC_OPERATION_ID + "=?",
                new String[]{conversationId, conversationId});

        return UriUtils.parseIfNotNull(strRet);
    }

    public static String getBoundKeyUrlFromActivity(ContentResolver contentResolver, String activityId) {
        return getOneValue(contentResolver, ActivityEntry.ENCRYPTION_KEY_URL,
                ActivityEntry.ACTIVITY_ID + "=? OR " + ActivityEntry.SYNC_OPERATION_ID + "=?",
                new String[]{activityId, activityId});

    }

    public static Uri getTitleEncryptionKeyUrlForConversation(ContentResolver contentResolver, String conversationId) {
        String strRet = getOneValue(contentResolver, ConversationEntry.TITLE_ENCRYPTION_KEY_URL,
                ConversationEntry.CONVERSATION_ID + "=? OR " + ConversationEntry.SYNC_OPERATION_ID + "=?",
                new String[]{conversationId, conversationId});

        return UriUtils.parseIfNotNull(strRet);
    }

    @SuppressLint("Recycle") // TODO: Remove Lint is giving us a False Posistive!
    public static KeyObject getKeyForEncryption(ContentResolver contentResolver, String conversationId) {
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(ConversationContract.vw_Conversation.CONTENT_URI, ConversationContract.vw_Conversation.DEFAULT_PROJECTION,
                    ConversationContract.vw_Conversation.CONVERSATION_ID + "=? OR " + ConversationContract.vw_Conversation.SYNC_OPERATION_ID + "=?",
                    new String[]{conversationId, conversationId}, null);
            if (cursor != null && cursor.moveToNext()) {
                String url = cursor.getString(ConversationContract.vw_Conversation.DEFAULT_ENCRYPTION_KEY_URL.ordinal());
                if (TextUtils.isEmpty(url))
                    return null;
                String value = null;
                cursor.close();
                cursor = contentResolver.query(ConversationContract.EncryptionKeyEntry.CONTENT_URI, ConversationContract.EncryptionKeyEntry.DEFAULT_PROJECTION,
                        ConversationContract.EncryptionKeyEntry.ENCRYPTION_KEY_URI + "=? ",
                        new String[]{url}, null);
                String keyId = null;
                if (cursor != null && cursor.moveToNext()) {
                    value = cursor.getString(ConversationContract.EncryptionKeyEntry.ENCRYPTION_KEY.ordinal());
                    keyId = cursor.getString(ConversationContract.EncryptionKeyEntry.ENCRYPTION_KEY_ID.ordinal());
                }
                if (TextUtils.isEmpty(value))
                    return null;

                return new KeyObject(Uri.parse(url), value, UriUtils.parseIfNotNull(keyId));

            }
        } catch (Exception e) {
            Ln.e("Failed to retrieve encryption key from database ");
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    @SuppressLint("Recycle") // TODO: Remove Lint is giving us a False Posistive!
    public static boolean isKeyBound(ContentResolver resolver, Uri keyUri) {
        Cursor cursor = null;
        try {
            cursor = resolver.query(ConversationEntry.CONTENT_URI,
                    new String[]{ConversationEntry.DEFAULT_ENCRYPTION_KEY_URL.name()},
                    ConversationEntry.DEFAULT_ENCRYPTION_KEY_URL + "=? OR " +
                            ConversationEntry.TITLE_ENCRYPTION_KEY_URL + "=?",
                    new String[]{keyUri.toString(), keyUri.toString()}, null);

            if (cursor.moveToNext())
                return true;
        } finally {
            if (cursor != null)
                cursor.close();
            cursor = null;
        }
        try {
            cursor = resolver.query(ActivityEntry.CONTENT_URI,
                    new String[]{ActivityEntry.ENCRYPTION_KEY_URL.name()},
                    ActivityEntry.ENCRYPTION_KEY_URL + "=?",
                    new String[]{keyUri.toString()}, null);

            if (cursor.moveToNext())
                return true;
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return false;
    }

    public static boolean isCurrentUserAModerator(ContentResolver resolver, AuthenticatedUser authenticatedUser, String conversationId) {
        String result = getOneValue(resolver, ConversationContract.vw_Participant.IS_MODERATOR,
                ConversationContract.vw_Participant.ACTOR_UUID + " = ? AND "
                        + ConversationContract.vw_Participant.CONVERSATION_ID + " = ?", new String[]{authenticatedUser.getKey().getUuid(), conversationId});

        boolean value = false;

        if (result != null) {
            value = Integer.parseInt(result) == 1;
        }

        return value;
    }

    public static KeyObject getSharedKmsKeyFromCache(ContentResolver resolver, String sharedKeyId) {
        Cursor cursor = null;
        try {
            cursor = resolver.query(ConversationContract.EncryptionKeyEntry.CONTENT_URI,
                    new String[]{ConversationContract.EncryptionKeyEntry.ENCRYPTION_KEY.name(), ConversationContract.EncryptionKeyEntry.ENCRYPTION_KEY_ID.name(), ConversationContract.EncryptionKeyEntry.ENCRYPTION_KEY_EXPIRY_TIME.name()},
                    ConversationContract.EncryptionKeyEntry.ENCRYPTION_KEY_URI + "=? AND " + ConversationContract.EncryptionKeyEntry.ENCRYPTION_KEY + " IS NOT NULL",
                    new String[]{sharedKeyId}, null);
            if (cursor != null && cursor.moveToNext()) {
                String keyString = cursor.getString(0).trim();
                String keyId = cursor.getString(1).trim();
                long expiryTime = cursor.getLong(2);
                if (!TextUtils.isEmpty(keyString) && !TextUtils.isEmpty(keyId))
                    return new KeyObject(UriUtils.parseIfNotNull(keyId), keyString, UriUtils.parseIfNotNull(keyId), expiryTime);
            }

        } catch (Exception e) {
            Ln.e("Failed to retrieve encryption key from EncryptionKeyEntry");
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            cursor = null;
        }
        return null;
    }

    public static KeyObject getBoundKeyFromUri(ContentResolver resolver, Uri uri) {
        Cursor cursor = null;
        try {
            cursor = resolver.query(ConversationContract.EncryptionKeyEntry.CONTENT_URI,
                    new String[]{ConversationContract.EncryptionKeyEntry.ENCRYPTION_KEY.name(), ConversationContract.EncryptionKeyEntry.ENCRYPTION_KEY_ID.name()},
                    ConversationContract.EncryptionKeyEntry.ENCRYPTION_KEY_URI + "=? AND " + ConversationContract.EncryptionKeyEntry.ENCRYPTION_KEY + " IS NOT NULL",
                    new String[]{uri.toString()}, null);

            if (cursor != null && cursor.moveToNext()) {
                String keyString = cursor.getString(0).trim();
                String keyId = cursor.getString(1).trim();
                if (!TextUtils.isEmpty(keyString) && !TextUtils.isEmpty(keyId))
                    return new KeyObject(uri, keyString, Uri.parse(keyId));
            }

        } catch (Exception e) {
            Ln.e("Failed to retrieve encryption key from EncryptionKeyEntry");
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            cursor = null;
        }
        return null;
    }

    /**
     * Return a map of participants from the given conversation
     */
    public static HashMap<ActorRecord.ActorKey, ParticipantEntry.MembershipState> getConversationParticipants(ContentResolver contentResolver, String conversationId) {
        HashMap<ActorRecord.ActorKey, ParticipantEntry.MembershipState> ret = new HashMap<ActorRecord.ActorKey, ParticipantEntry.MembershipState>();
        Cursor c = null;
        try {
            c = contentResolver.query(ParticipantEntry.CONTENT_URI,
                    ParticipantEntry.DEFAULT_PROJECTION,
                    ParticipantEntry.CONVERSATION_ID + "=?",
                    new String[]{conversationId}, null);

            while (c != null && c.moveToNext()) {
                ParticipantEntry.MembershipState state = ParticipantEntry.MembershipState.values()[c.getInt(ParticipantEntry.MEMBERSHIP_STATE.ordinal())];
                String actorUuid = c.getString(ParticipantEntry.ACTOR_UUID.ordinal());
                ActorRecord.ActorKey actorKey = new ActorRecord.ActorKey(actorUuid);
                ret.put(actorKey, state);
            }

            return ret;
        } catch (Exception e) {
            Ln.e(e, "Failed getting participants from DB");
        } finally {
            if (c != null) {
                c.close();
            }
        }

        return null;
    }

    public static ArrayList<ActorRecord.ActorKey> getActiveConversationParticipants(ContentResolver contentResolver, String conversationId) {
        ArrayList<ActorRecord.ActorKey> ret = new ArrayList<>();
        Cursor c = null;
        try {
            c = contentResolver.query(ParticipantEntry.CONTENT_URI,
                    ParticipantEntry.DEFAULT_PROJECTION,
                    ParticipantEntry.CONVERSATION_ID + "=? AND " + ParticipantEntry.MEMBERSHIP_STATE + "=? ",
                    new String[]{conversationId, Integer.toString(ParticipantEntry.MembershipState.ACTIVE.ordinal())}, null);

            while (c != null && c.moveToNext()) {
                ret.add(new ActorRecord.ActorKey(c.getString(ParticipantEntry.ACTOR_UUID.ordinal())));
            }

            return ret;
        } catch (Exception e) {
            Ln.e(e, "Failed getting participants from DB");
        } finally {
            if (c != null) {
                c.close();
            }
        }

        return null;
    }

    public static ActivityReference getFirstReliableActivityAfter(ContentResolver contentResolver, String conversationId, long msTime) {
        String where = ActivityEntry.ACTIVITY_TYPE + " !=? AND "
                + ActivityEntry.CONVERSATION_ID + " =? AND "
                + ActivityEntry.ACTIVITY_PUBLISHED_TIME + " > ? AND ("
                + ActivityEntry.SOURCE + "=? OR " + ActivityEntry.SOURCE + "=? )";
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(ActivityEntry.CONTENT_URI,
                    ActivityEntry.DEFAULT_PROJECTION,
                    where,
                    new String[]{Integer.toString(ActivityEntry.Type.BACKFILL_GAP.ordinal()),
                            conversationId, String.valueOf(msTime),
                            String.valueOf(ActivityEntry.Source.SYNC.ordinal()), String.valueOf(ActivityEntry.Source.SYNC_PARTIAL.ordinal())},
                    ActivityEntry.ACTIVITY_PUBLISHED_TIME + " ASC LIMIT 1"
            );
            if (cursor != null && cursor.moveToFirst()) {
                return new ActivityReference(cursor);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    public static long getLastSelfAckTimestamp(ContentResolver contentResolver, String conversationId) {
        return getOneLongValue(contentResolver, ConversationContract.vw_Conversation.CONTENT_URI,
                ConversationContract.vw_Conversation.TIMESTAMP_LAST_SEEN_ACTIVITY_REMOTE.name(),
                ConversationContract.vw_Conversation.CONVERSATION_ID + " = ?", new String[]{conversationId});
    }

    /**
     * Get one string value out of the database. The selection is expected to match one row,
     * otherwise the value will be pulled from the first row.
     *
     * @param resolver
     * @param oneColumnSelection The DbColumn to fetch
     * @param selection
     * @param selectionArgs
     * @return The String value of the DbColumn that matches the selection
     */
    public static String getOneValue(ContentResolver resolver, DbColumn oneColumnSelection, String selection, String[] selectionArgs) {
        return getOneValue(resolver, oneColumnSelection.contentUri(), oneColumnSelection.name(), selection, selectionArgs);
    }

    /**
     * Get one string value out of the database. The selection is expected to match one row,
     * otherwise the value will be pulled from the first row.
     *
     * @param resolver
     * @param contentUri          CONTENT_URI for the table
     * @param oneColumnProjection The column name to fetch
     * @param selection
     * @param selectionArgs
     * @return The String value for the column that matches the selection
     */
    public static String getOneValue(ContentResolver resolver, Uri contentUri, String oneColumnProjection, String selection, String[] selectionArgs) {
        Cursor c = null;
        try {
            c = resolver.query(contentUri,
                    new String[]{oneColumnProjection},
                    selection,
                    selectionArgs,
                    null);

            if (c != null && c.moveToFirst())
                return c.getString(0);

        } finally {
            if (c != null)
                c.close();
        }
        return null;
    }

    public static long getOneLongValue(ContentResolver resolver, Uri contentUri, String oneColumnProjection, String selection, String[] selectionArgs) {
        String retstr = getOneValue(resolver, contentUri, oneColumnProjection, selection, selectionArgs);
        if (retstr == null)
            return 0;

        try {
            return Long.parseLong(retstr);
        } catch (Exception e) {
            return 0;
        }
    }

    public static String getGapType(ContentResolver contentResolver, Uri gapUri) {
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(gapUri,
                    ActivityEntry.DEFAULT_PROJECTION,
                    null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                return new ActivityReference(cursor).getType().name();
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    public static Bundle getOneOnOneConversation(ContentResolver contentResolver, String oneOnOneParticipant) {
        Cursor c = null;
        Bundle bundle = new Bundle();
        try {
            c = contentResolver.query(ConversationEntry.CONTENT_URI,
                    ConversationEntry.DEFAULT_PROJECTION,
                    ConversationEntry.ONE_ON_ONE_PARTICIPANT + "=?",
                    new String[]{oneOnOneParticipant},
                    null);

            if (c != null && c.moveToFirst()) {
                return cursorToBundle(c);
            }
        } finally {
            if (c != null)
                c.close();
        }


        return bundle;
    }

    private static Bundle cursorToBundle(Cursor c) {
        Bundle bundle = new Bundle();
        for (int i = 0; i < c.getColumnCount(); ++i) {
            bundle.putString(c.getColumnName(i), c.getString(i));
        }
        return bundle;
    }

    public static int getUnreadTeamConversationsCount(ContentResolver contentResolver, String teamId) {
        String ret = getOneValue(contentResolver, ConversationEntry.CONTENT_URI, "COUNT(*)",
                ConversationEntry.TEAM_ID.name() + "=? AND " + ConversationContract.vw_Conversation.TIMESTAMP_LAST_READABLE_ACTIVITY_REMOTE + " > " + ConversationContract.vw_Conversation.TIMESTAMP_LAST_SEEN_ACTIVITY_REMOTE,
                new String[]{teamId});

        if (!TextUtils.isEmpty(ret))
            return Integer.valueOf(ret);

        return 0;
    }

    public static boolean isUserTeamModerator(ContentResolver contentResolver, String primaryTeamConversationId, String actorUUID) {
        Cursor c = null;
        try {
            c = contentResolver.query(ParticipantEntry.CONTENT_URI,
                    ParticipantEntry.DEFAULT_PROJECTION,
                    ParticipantEntry.ACTOR_UUID + "=? AND " + ParticipantEntry.CONVERSATION_ID + "=? ",
                    new String[]{actorUUID, primaryTeamConversationId},
                    null);

            if (c != null && c.moveToFirst() && c.getCount() > 0) {
                int moderator = c.getInt(ParticipantEntry.IS_MODERATOR.ordinal());
                return moderator == 1;
            }
        } finally {
            if (c != null)
                c.close();
        }
        return false;
    }

    public static int getConversationParticipantCount(ContentResolver contentResolver, String conversationId) {
        String ret = getOneValue(contentResolver, ParticipantEntry.CONTENT_URI, "COUNT(*)",
                ParticipantEntry.CONVERSATION_ID.name() + "=?" +
                        " AND " + ParticipantEntry.MEMBERSHIP_STATE + " = " + ParticipantEntry.MembershipState.ACTIVE.ordinal(),
                new String[]{conversationId});

        if (!TextUtils.isEmpty(ret))
            return Integer.valueOf(ret);

        return 0;
    }

    public static int getConversationModeratorCount(ContentResolver contentResolver, String conversationId) {
        String ret = getOneValue(contentResolver, ParticipantEntry.CONTENT_URI, "COUNT(*)",
                ParticipantEntry.CONVERSATION_ID + "= ? AND " + ParticipantEntry.IS_MODERATOR + " = ?", new String[]{conversationId, "1"});

        if (!TextUtils.isEmpty(ret))
            return Integer.valueOf(ret);

        return 0;
    }

    public static int getConversationExternalParticipantCount(ContentResolver contentResolver, String conversationId, String orgId) {
        String ret = getOneValue(contentResolver, ConversationContract.vw_Participant.CONTENT_URI, "COUNT(*)",
                ConversationContract.vw_Participant.CONVERSATION_ID + "=? AND " + "COALESCE(" + ConversationContract.vw_Participant.ORG_ID.name() + ", \"\") <> ?", new String[]{conversationId, orgId});

        if (!TextUtils.isEmpty(ret))
            return Integer.valueOf(ret);

        return 0;
    }

    public static int getConversationExternalParticipantCount(ContentResolver contentResolver, String conversationId) {
        String ret = getOneValue(contentResolver, ConversationEntry.CONTENT_URI, ConversationEntry.EXTERNAL_PARTICIPANT_COUNT.name(),
                ConversationEntry.CONVERSATION_ID + "= ?", new String[]{conversationId});

        if (!TextUtils.isEmpty(ret))
            return Integer.valueOf(ret);

        return 0;
    }

    public static Bundle getConversationById(ContentResolver contentResolver, String conversationId) {
        Cursor c = null;
        try {
            c = contentResolver.query(ConversationEntry.CONTENT_URI,
                    ConversationEntry.DEFAULT_PROJECTION,
                    ConversationEntry.CONVERSATION_ID + "=? OR " + ConversationEntry.SYNC_OPERATION_ID + " =?",
                    new String[]{conversationId, conversationId},
                    null);

            if (c != null && c.moveToFirst()) {
                return cursorToBundle(c);
            }
        } finally {
            if (c != null)
                c.close();
        }
        return null;
    }

    public static ConversationResolver getConversationResolverById(ContentResolver contentResolver, String conversationId, Injector injector) {
        Cursor c = null;
        try {
            c = contentResolver.query(ConversationContract.vw_Conversation.CONTENT_URI,
                    ConversationContract.vw_Conversation.DEFAULT_PROJECTION,
                    ConversationContract.vw_Conversation.CONVERSATION_ID + "=? OR " + ConversationContract.vw_Conversation.SYNC_OPERATION_ID + " =?",
                    new String[]{conversationId, conversationId},
                    null);

            if (c != null && c.moveToFirst()) {
                return new ConversationResolver(injector, c);
            }
        } finally {
            if (c != null)
                c.close();
        }
        return null;
    }

    public static Boolean isConversationRead(ContentResolver contentResolver, String conversationId) {
        Cursor c = null;
        try {
            c = contentResolver.query(ConversationEntry.CONTENT_URI,
                    new String[]{ConversationEntry.TIMESTAMP_LAST_SEEN_ACTIVITY_REMOTE.name(), ConversationEntry.TIMESTAMP_LAST_READABLE_ACTIVITY_REMOTE.name()},
                    ConversationEntry.CONVERSATION_ID + "=?",
                    new String[]{conversationId},
                    null
            );
            if (c != null && c.moveToFirst()) {
                String strLastSeen = c.getString(0);
                String strLastReadable = c.getString(1);
                if (strLastReadable == null || strLastSeen == null)
                    return null;

                try {
                    return (Long.valueOf(strLastSeen) >= Long.valueOf(strLastReadable));
                } catch (Exception e) {
                    Ln.d("Failed parsing lastSeen/lastReadable");
                }
            }
        } finally {
            if (c != null)
                c.close();
        }
        return null;
    }

    public static Bundle getTitleByLocusKey(ContentResolver contentResolver, String locusKey) {
        Cursor c = null;
        Bundle bundle = new Bundle();
        try {
            c = contentResolver.query(ConversationContract.vw_TitleEncryptionInfo.CONTENT_URI,
                    new String[]{ConversationContract.vw_TitleEncryptionInfo.TITLE.name(), ConversationContract.vw_TitleEncryptionInfo.TITLE_ENCRYPTION_KEY.name(), ConversationContract.vw_TitleEncryptionInfo.TITLE_ENCRYPTION_KEY_URL.name(), ConversationContract.vw_TitleEncryptionInfo.TITLE_ENCRYPTION_KEY_ID.name()},
                    ConversationContract.vw_TitleEncryptionInfo.LOCUS_URL + "=?",
                    new String[]{locusKey},
                    null);

            if (c != null && c.moveToFirst()) {
                return cursorToBundle(c);
            }
        } finally {
            if (c != null)
                c.close();
        }
        return bundle;
    }

    /**
     * @return A list of Conversation ID's that a given email is a member of.
     */
    public static List<String> getConversationsByParticipant(ContentResolver contentResolver, String email) {
        List<String> conversationIds = new ArrayList<>();
        Cursor c = null;
        try {
            c = contentResolver.query(ConversationContract.vw_Participant.CONTENT_URI,
                    new String[]{ConversationContract.vw_Participant.CONVERSATION_ID.name()},
                    ConversationContract.vw_Participant.EMAIL + "=?",
                    new String[]{email},
                    null);

            while (c.moveToNext()) {
                conversationIds.add(c.getString(0));
            }
        } finally {
            if (c != null)
                c.close();
        }
        return conversationIds;
    }

    public static long getTotalParticipants(ContentResolver contentResolver) {
        return getOneLongValue(contentResolver, ConversationContract.vw_Participant.CONTENT_URI, "COUNT(*)", null, null);
    }

    public static Set<UUID> getStickyPadIDs(ContentResolver contentResolver) {
        if (contentResolver == null) {
            return null;
        }

        Set<UUID> stickyPadIDs = new HashSet<>();
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(
                    ConversationContract.StickyEntry.CONTENT_URI,
                    new String[]{ConversationContract.StickyEntry.PAD_ID.name()},
                    null,
                    null,
                    null);

            final int columnIndex = cursor.getColumnIndex(ConversationContract.StickyEntry.PAD_ID.name());
            while (cursor != null && cursor.moveToNext()) {
                String id = cursor.getString(columnIndex);
                stickyPadIDs.add(UUID.fromString(id));
            }
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }

        return stickyPadIDs;
    }

    public static Sticky getOneSticky(ContentResolver contentResolver, String filename) {
        if (filename == null || contentResolver == null) {
            return null;
        }

        Sticky sticky = null;
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(
                    ConversationContract.StickyEntry.CONTENT_URI,
                    ConversationContract.StickyEntry.DEFAULT_PROJECTION,
                    ConversationContract.StickyEntry.STICKY_ID.name() + "=?",
                    new String[]{filename},
                    null);

            if (cursor != null) {
                sticky = new Sticky();

                String stickyID = cursor.getString(cursor.getColumnIndex(ConversationContract.StickyEntry.STICKY_ID.name()));
                String stickyDescription = cursor.getString(cursor.getColumnIndex(ConversationContract.StickyEntry.STICKY_DESCRIPTION.name()));
                Uri location = Uri.parse(cursor.getString(cursor.getColumnIndex(ConversationContract.StickyEntry.STICKY_URL.name())));

                sticky.setId(stickyID);
                sticky.setDescription(stickyDescription);
                sticky.setLocation(location);
            }
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }

        return sticky;


    }

    public static StickyPad getOneStickyPad(ContentResolver contentResolver, UUID stickyPadID) {
        if (stickyPadID == null || contentResolver == null) {
            return null;
        }

        StickyPad pad = null;
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(
                    ConversationContract.StickyEntry.CONTENT_URI,
                    ConversationContract.StickyEntry.DEFAULT_PROJECTION,
                    ConversationContract.StickyEntry.PAD_ID.name() + "=?",
                    new String[]{stickyPadID.toString()},
                    null);

            if (cursor != null) {
                pad = new StickyPad();
                pad.setId(stickyPadID);

                List<Sticky> stickies = new ArrayList<>();
                while (cursor.moveToNext()) {
                    String padDescription = cursor.getString(cursor.getColumnIndex(ConversationContract.StickyEntry.PAD_DESCRIPTION.name()));
                    pad.setDescription(padDescription);

                    String stickyID = cursor.getString(cursor.getColumnIndex(ConversationContract.StickyEntry.STICKY_ID.name()));
                    String stickyDescription = cursor.getString(cursor.getColumnIndex(ConversationContract.StickyEntry.STICKY_DESCRIPTION.name()));
                    Uri location = Uri.parse(cursor.getString(cursor.getColumnIndex(ConversationContract.StickyEntry.STICKY_URL.name())));

                    stickies.add(Sticky.createSticky(stickyID, stickyDescription, location));
                }

                pad.setStickies(stickies);
            }
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }

        return pad;
    }

    // Returns an empty list if the cursor is NULL or has no data
    public static List<StickyPad> createStickyPadsFromCursor(Cursor cursor) {

        HashMap<UUID, StickyPad> stickyPads = new HashMap<>();
        StickyPad[] padArray = new StickyPad[]{};
        if (cursor != null && !cursor.isClosed()) {
            while (cursor.moveToNext()) {
                UUID currentStickyPadID = UUID.fromString(cursor.getString(cursor.getColumnIndex(ConversationContract.StickyEntry.PAD_ID.name())));
                String padDescription = cursor.getString(cursor.getColumnIndex(ConversationContract.StickyEntry.PAD_DESCRIPTION.name()));

                StickyPad currentPad = stickyPads.get(currentStickyPadID);
                if (currentPad == null) {
                    currentPad = StickyPad.createStickyPad(currentStickyPadID, padDescription, new ArrayList<Sticky>());
                    stickyPads.put(currentStickyPadID, currentPad);
                }

                String stickyId = cursor.getString(cursor.getColumnIndex(ConversationContract.StickyEntry.STICKY_ID.name()));
                String stickyDescription = cursor.getString(cursor.getColumnIndex(ConversationContract.StickyEntry.STICKY_DESCRIPTION.name()));
                Uri location = Uri.parse(cursor.getString(cursor.getColumnIndex(ConversationContract.StickyEntry.STICKY_URL.name())));

                currentPad.getStickies().add(Sticky.createSticky(stickyId, stickyDescription, location));
            }

            padArray = stickyPads.values().toArray(new StickyPad[stickyPads.size()]);
        }

        return CollectionUtils.asList(padArray);
    }

    public static KmsResourceObject getKmsResourceObject(ContentResolver contentResolver, String conversationId) {
        Uri uri = UriUtils.parseIfNotNull(getOneValue(contentResolver,
                Uri.withAppendedPath(ConversationContract.ConversationEntry.CONTENT_URI, conversationId),
                ConversationEntry.KMS_RESOURCE_OBJECT_URI.name(),
                null, null));

        if (uri == null)
            return null;

        return new KmsResourceObject(uri);
    }

    public static String getRetentionDuration(ContentResolver resolver, String conversationId) {
        String retentionDuration = getOneValue(resolver,
                Uri.withAppendedPath(ConversationContract.ConversationEntry.CONTENT_URI, conversationId),
                ConversationEntry.RETENTION_DAYS.name(),
                null, null);
        return retentionDuration;
    }

    public static boolean isTeamModerator(ContentResolver resolver, String teamId, String userId) {
        long result = getOneLongValue(resolver,
                ParticipantEntry.CONTENT_URI,
                ParticipantEntry.IS_MODERATOR.name(),
                ParticipantEntry.CONVERSATION_ID + " = ? AND " + ParticipantEntry.ACTOR_UUID + " = ? ",
                new String[] { teamId, userId});

        return result > 0;
    }
}
