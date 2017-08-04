package com.cisco.spark.android.sync;

import android.net.Uri;
import android.provider.BaseColumns;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.SparseArray;

import com.cisco.spark.android.BuildConfig;
import com.cisco.spark.android.flag.FlagOperation;
import com.cisco.spark.android.model.Verb;
import com.cisco.spark.android.presence.operation.FetchPresenceStatusOperation;
import com.cisco.spark.android.presence.operation.SendPresenceEventOperation;
import com.cisco.spark.android.presence.operation.SubscribePresenceStatusOperation;
import com.cisco.spark.android.sync.operationqueue.ActivityFillOperation;
import com.cisco.spark.android.sync.operationqueue.AddPersonOperation;
import com.cisco.spark.android.sync.operationqueue.AssignRoomAvatarOperation;
import com.cisco.spark.android.sync.operationqueue.AudioMuteOperation;
import com.cisco.spark.android.sync.operationqueue.AudioVolumeOperation;
import com.cisco.spark.android.sync.operationqueue.AvatarUpdateOperation;
import com.cisco.spark.android.sync.operationqueue.CatchUpSyncOperation;
import com.cisco.spark.android.sync.operationqueue.ContentUploadOperation;
import com.cisco.spark.android.sync.operationqueue.CustomNotificationsTagOperation;
import com.cisco.spark.android.sync.operationqueue.DeleteActivityOperation;
import com.cisco.spark.android.sync.operationqueue.FeatureToggleOperation;
import com.cisco.spark.android.sync.operationqueue.FetchActivityContextOperation;
import com.cisco.spark.android.sync.operationqueue.FetchMentionsOperation;
import com.cisco.spark.android.sync.operationqueue.FetchSpaceUrlOperation;
import com.cisco.spark.android.sync.operationqueue.FetchUnjoinedTeamRoomsOperation;
import com.cisco.spark.android.sync.operationqueue.GetAvatarUrlsOperation;
import com.cisco.spark.android.sync.operationqueue.GetRetentionPolicyInfoOperation;
import com.cisco.spark.android.sync.operationqueue.IncrementShareCountOperation;
import com.cisco.spark.android.sync.operationqueue.IntegrateContactsOperation;
import com.cisco.spark.android.sync.operationqueue.JoinTeamRoomOperation;
import com.cisco.spark.android.sync.operationqueue.KeyFetchOperation;
import com.cisco.spark.android.sync.operationqueue.MapEventToConversationOperation;
import com.cisco.spark.android.sync.operationqueue.MarkReadOperation;
import com.cisco.spark.android.sync.operationqueue.MoveRoomToTeamOperation;
import com.cisco.spark.android.sync.operationqueue.NewConversationOperation;
import com.cisco.spark.android.sync.operationqueue.NewConversationWithRepostedMessagesOperation;
import com.cisco.spark.android.sync.operationqueue.PostCommentOperation;
import com.cisco.spark.android.sync.operationqueue.PostContentActivityOperation;
import com.cisco.spark.android.sync.operationqueue.PostGenericMetricOperation;
import com.cisco.spark.android.sync.operationqueue.RemoteSearchOperation;
import com.cisco.spark.android.sync.operationqueue.RemoveParticipantOperation;
import com.cisco.spark.android.sync.operationqueue.RemoveRoomAvatarOperation;
import com.cisco.spark.android.sync.operationqueue.RoomBindOperation;
import com.cisco.spark.android.sync.operationqueue.ScheduledEventActivityOperation;
import com.cisco.spark.android.sync.operationqueue.SendDtmfOperation;
import com.cisco.spark.android.sync.operationqueue.SetTitleAndSummaryOperation;
import com.cisco.spark.android.sync.operationqueue.SetupSharedKeyWithKmsOperation;
import com.cisco.spark.android.sync.operationqueue.TagOperation;
import com.cisco.spark.android.sync.operationqueue.ToggleActivityOperation;
import com.cisco.spark.android.sync.operationqueue.ToggleParticipantActivityOperation;
import com.cisco.spark.android.sync.operationqueue.TokenRefreshOperation;
import com.cisco.spark.android.sync.operationqueue.TokenRevokeOperation;
import com.cisco.spark.android.sync.operationqueue.UnboundKeyFetchOperation;
import com.cisco.spark.android.sync.operationqueue.UnsetFeatureToggleOperation;
import com.cisco.spark.android.sync.operationqueue.UpdateEncryptionKeyOperation;
import com.cisco.spark.android.sync.operationqueue.UpdateTeamColorOperation;
import com.cisco.spark.android.sync.operationqueue.VideoThumbnailOperation;
import com.cisco.spark.android.sync.operationqueue.core.FetchAcksOperation;
import com.cisco.spark.android.sync.operationqueue.core.FetchParticipantsOperation;
import com.cisco.spark.android.sync.operationqueue.core.Operation;
import com.cisco.spark.android.sync.queue.SyncFlagsOperation;
import com.cisco.spark.android.sync.queue.SyncTaskOperation;
import com.cisco.spark.android.wdm.RegisterDeviceOperation;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

/**
 * Field and table name constants for {@link ConversationContentProvider}.
 */

/**
 * Data migration
 * <p>
 * There are some strict tests around data migration because failure to guarantee a
 * consistent upgrade path can cause mysterious and difficult problems in production.
 * <p>
 * Relevant tests:
 * <p>
 * LargeDatasetTest.testOpenLargeDataset
 * Opens a canned large DB file with the minimum supported schema version and ensures data integrity. Also a pretty
 * decent test of large dataset handling.
 * <p>
 * DatabaseHelperTest.testFullMigration
 * Opens a series of canned DB files to ensure all migration paths to the current schema version are successful
 * <p>
 * ConversationContractTest.testDbSchemaSanity
 * Ensures the current schema version has an upgrade module in DatabaseHelper
 * Ensures the schema hash has been updated
 * Ensures the uniqueness of ConversationContract match codes
 * <p>
 * <p>
 * Updating the schema
 * <p>
 * - Update the schema as needed in ConversationContract
 * - Increment ConversationContract.DB_SCHEMA_VERSION
 * - run ./gradlew test ; testDbSchemaSanity should fail. Open the test log and get the new schema hash.
 * - Paste the new schema hash into ConversationContract.DB_SCHEMA_HASH.
 * - In DatabaseHelper, add a new UpgradeModule to the upgradeModules array. See that file for add'l notes
 * <p>
 * If an upgrade module can't be easily written, see the next section.
 * <p>
 * <p>
 * The nuclear option: Wipe the DB and start fresh
 * <p>
 * Do this if data migration is not practical.  Users upgrading to this version will start with a clean database,
 * as if they had logged out and back in again.
 * <p>
 * - Follow the normal steps for updating the schema. The upgrade module can be left blank
 * - Update DatabaseHelper.MINIMUM_SCHEMA_VERSION_FOR_MIGRATION to the ConversationContract.DB_SCHEMA_VERSION
 * - Enable ConversationSyncTest#testBuildNewMinSchemaVersionSampleDb
 * - Run ./scripts/buildNewMinVersionDb.sh. This writes a new canned DB to the test resources. This will take a while.
 * - Disable ConversationSyncTest#testBuildNewMinSchemaVersionSampleDb again (add a _ to the beginning of the test name)
 * - Include the updated DB file as part of your commit "./app/src/androidTest/res/raw/min_schema_version_db_encrypted.db".
 * - testOpenLargeDataset will fail if this file's schema version is below the minimum.
 */


public class ConversationContract {

    // We use a checksum to detect when the schema changes, as a reminder to increment the
    // schema version and add migration as needed.  We put these on a single line to ensure
    // we get a merge conflict if two branches update to the same schema version.
    //
    // To get the new hash, run the ConversationContractTest.testDbSchemaSanity unit test
    public static final String[] SCHEMA_VERSION_AND_HASH = {"154", "812186ce7bc28b75a3c72e38b709f4c9"};

    public static final int SCHEMA_VERSION = Integer.valueOf(SCHEMA_VERSION_AND_HASH[0]);
    public static final String DB_SCHEMA_HASH = SCHEMA_VERSION_AND_HASH[1];

    //sdk
    public static final String CONTENT_AUTHORITY = "com.cisco.wx2.sdk.video.api.android.sync.conversation";
    //public static final String CONTENT_AUTHORITY = "com.cisco.spark.android.sync.conversation";

    private static final String CONTENT_TYPE_PREFIX = "vnd.android.cursor.dir/vnd.com.cisco.wx2.";
    private static final String CONTENT_ITEM_TYPE_PREFIX = "vnd.android.cursor.item/vnd.com.cisco.wx2.";

    public static final String TEXT = "TEXT";
    public static final String INTEGER = "INTEGER";

    private static final int URI_MATCH_ID_BASE = 100;
    private static final int ID_URI_MATCH_ID_BASE = 1000;

    private static int matchIdCounter = 0;

    public static boolean isIdUri(int uriMatchCode) {
        return uriMatchCode >= ID_URI_MATCH_ID_BASE;
    }

    public static boolean isView(int uriMatchCode) {
        for (ViewColumn[] view : allViews) {
            if (view[0].uriMatchCode() == uriMatchCode)
                return true;
            if (view[0].idUriMatchCode() == uriMatchCode)
                return true;
        }
        return false;
    }

    public static boolean isTable(int uriMatchCode) {
        for (DbColumn[] table : allTables) {
            if (table[0].uriMatchCode() == uriMatchCode)
                return true;
            if (table[0].idUriMatchCode() == uriMatchCode)
                return true;
        }
        return false;
    }

    public static boolean isVirtualTable(int uriMatchCode) {
        for (DbColumn[] vtable : allVirtualTables) {
            if (vtable[0].uriMatchCode() == uriMatchCode)
                return true;
            if (vtable[0].idUriMatchCode() == uriMatchCode)
                return true;
        }
        return false;
    }

    public static String getTableNameFromMatchCode(int uriMatchCode) {
        DbColumn col = getIDColumnForMatch(uriMatchCode, null);
        if (col != null)
            return col.tablename();
        return null;
    }

    private static int getNextMatchId() {
        return URI_MATCH_ID_BASE + (matchIdCounter++);
    }

    public static final int PRIMARY_KEY = 0x01; // flags can be ORd together
    public static final int AUTOINCREMENT = 0x02;
    public static final int UNIQUE = 0x04;
    public static final int COMPLEX_KEY = 0x08;
    public static final int NOT_NULL = 0x10;
    public static final int DEFAULT_0 = 0x20;
    public static final int DEFAULT_1 = 0x40;
    public static final int DEFAULT_EMPTY_STRING = 0x80;
    public static final int TIMESTAMP = 0x100;


    public interface DbColumn {
        String datatype();

        int flags();

        String name();

        String tablename();

        int uriMatchCode();

        int idUriMatchCode();

        String contentType();

        String contentItemType();

        Uri contentUri();

        int ordinal();
    }

    /**
     * public static String[] getProjection
     * <p/>
     * Get the default projection e.g. for ActorEntry like this: String[] projection =
     * ConversationContract.getProjection(ActorEntry.values());
     *
     * @param values     Values from the table enum
     * @param additional Other columns to fetch from the query e.g. in a join
     * @return String array to be used as a projection in a content provider query
     */
    public static <T extends Enum<T>> String[] getProjection(T[] values, String... additional) {
        int i = 0;
        String[] result = new String[values.length + additional.length];
        for (T value : values) {
            result[i++] = value.name();
        }

        for (String name : additional) {
            result[i++] = name;
        }

        return result;
    }

    public enum ConversationEntry implements DbColumn, BaseColumns {
        _id(INTEGER, PRIMARY_KEY | AUTOINCREMENT),
        CONVERSATION_ID(TEXT, UNIQUE | NOT_NULL),
        URL(TEXT, 0),
        TITLE(TEXT, 0),                                                     // title, may be encrypted
        CONVERSATION_DISPLAY_NAME(TEXT, 0),                                 // title for display, may be default
        LAST_ACTIVITY_ID(TEXT, 0),                                          // id of the activity to show in the room list subhead
        SORTING_TIMESTAMP(TEXT, NOT_NULL | DEFAULT_0),                      // for sorting the room list
        MUTED(INTEGER, 0),
        MESSAGE_NOTIFICATIONS(INTEGER, 0),
        MENTION_NOTIFICATIONS(INTEGER, 0),
        ONE_ON_ONE_PARTICIPANT(TEXT, UNIQUE),
        SPACE_URL(TEXT, 0),
        SPACE_URL_HIDDEN(TEXT, 0),
        LOCUS_URL(TEXT, 0),
        TITLE_ENCRYPTION_KEY_URL(TEXT, 0),
        IS_TITLE_ENCRYPTED(INTEGER, NOT_NULL | DEFAULT_0),
        DEFAULT_ENCRYPTION_KEY_URL(TEXT, 0),
        SYNC_OPERATION_ID(TEXT, UNIQUE),
        FAVORITE(INTEGER, 0),
        PARTICIPANT_COUNT(INTEGER, NOT_NULL | DEFAULT_0),
        EXTERNAL_PARTICIPANT_COUNT(INTEGER, NOT_NULL | DEFAULT_0),
        TIMESTAMP_LAST_SEEN_ACTIVITY_REMOTE(INTEGER, NOT_NULL | DEFAULT_0),         // Server Time of the latest activity we acked. Local time never goes here
        TIMESTAMP_LAST_READABLE_ACTIVITY_REMOTE(INTEGER, NOT_NULL | DEFAULT_0),     // Server Time of the last activity that can mark a room unread. Local time never goes here
        IN_ACTIVE_CALL(INTEGER, NOT_NULL | DEFAULT_0),
        HIDDEN(INTEGER, NOT_NULL | DEFAULT_0),                                      // Primarily used when a user clicks "leave" for a 1:1. This has the effect of hiding the conversation from
                                                                                    // the conversation list and a few other places for the user that "leaves". It's still discoverable through
                                                                                    // search and can be un-hidden by sending a message
        PMR(INTEGER, NOT_NULL | DEFAULT_0),
        LOCKED(INTEGER, NOT_NULL | DEFAULT_0),
        TOP_PARTICIPANTS(TEXT, 0),                                                  // json-ified List<ActorRecord> most recently active
        CREATOR_UUID(TEXT, 0),
        LAST_RELEVANT_ACTIVITY_TIMESTAMP(INTEGER, NOT_NULL | DEFAULT_0),
        KMS_RESOURCE_OBJECT_URI(TEXT, 0),
        TEAM_ID(TEXT, 0),
        SUMMARY(TEXT, 0),
        SELF_JOINED(INTEGER, NOT_NULL | DEFAULT_1),
        ARCHIVED(INTEGER, NOT_NULL | DEFAULT_0),                                    // Slightly different behavior from HIDDEN, this is used for teams. A user can archive a team or team room and it
                                                                                    // will be hidden everywhere in the UI except for the archived rooms/teams pages, where it can be unarchived. Archived
                                                                                    // rooms should not be discoverable in search and will be archived for all members, not just the user who archived the room

        CONVERSATION_AVATAR_CONTENT_REFERENCE(TEXT, 0),                             // JSON representation of ConversationAvatarContentReference.class
        AVATAR_ENCRYPTION_KEY_URL(TEXT, 0),
        IS_AVATAR_ENCRYPTED(INTEGER, NOT_NULL | DEFAULT_0),
        SHARE_COUNT(INTEGER, NOT_NULL | DEFAULT_0),                                 // for Direct Share
        IS_PARTICIPANT_LIST_VALID(INTEGER, NOT_NULL | DEFAULT_0),                   // Server timestamp of the last time the full participants list was valid, or 0 if needs updating

        CUSTODIAN_ORG_ID(TEXT, 0),
        CUSTODIAN_ORG_NAME(TEXT, 0),
        RETENTION_URL(TEXT,0),
        BINDING_STATE(INTEGER, DEFAULT_0),
        RETENTION_DAYS(INTEGER, DEFAULT_0),
        ACL_URL(TEXT, 0),                                                           // ACL url for conversation
        IS_TEAM_GUEST(INTEGER, 0),
        LAST_RETENTION_SYNC_TIMESTAMP(INTEGER, DEFAULT_0);

        public static final String[] DEFAULT_PROJECTION = getProjection(values());

        public static final String TABLE_NAME = ConversationEntry.class.getSimpleName();
        public static final int URI_MATCHCODE = getNextMatchId();
        public static final int URI_IDMATCHCODE = URI_MATCHCODE + ID_URI_MATCH_ID_BASE;

        //Special Case for content://conversationauthority/conversationentry/*/activities
        public static final int URI_MATCH_CONVERSATION_ACTIVITIES = getNextMatchId();

        public static Uri getConversationActivitiesUri(String conversationId) {
            return  Uri
                    .parse("content://" + CONTENT_AUTHORITY + "/ConversationActivity").buildUpon().appendPath(conversationId).appendPath("activities").build();
        }

        public static final Uri CONTENT_URI = Uri
                .parse("content://" + CONTENT_AUTHORITY + "/" + TABLE_NAME);

        public String type = "";
        int flags = 0;

        ConversationEntry(String type, int flags) {
            this.type = type;
            this.flags = flags;
        }

        // TODO: there might be a better place for this somewhere else
        public static List<ActorRecord> getTopParticipants(Gson gson, String topParticipantsJson) {
            List<ActorRecord> topParticipants = null;
            if (!TextUtils.isEmpty(topParticipantsJson)) {
                Type type = new TypeToken<ArrayList<ActorRecord>>() { }.getType();
                topParticipants = gson.fromJson(topParticipantsJson, type);
            }
            return topParticipants;
        }

        @Override
        public String tablename() {
            return TABLE_NAME;
        }

        @Override
        public int uriMatchCode() {
            return URI_MATCHCODE;
        }

        @Override
        public int idUriMatchCode() {
            return URI_IDMATCHCODE;
        }

        @Override
        public String datatype() {
            return type;
        }

        @Override
        public int flags() {
            return flags;
        }

        @Override
        public String contentType() {
            return CONTENT_TYPE_PREFIX + TABLE_NAME.toLowerCase(Locale.US);
        }

        @Override
        public String contentItemType() {
            return CONTENT_ITEM_TYPE_PREFIX + TABLE_NAME.toLowerCase(Locale.US);
        }

        @Override
        public Uri contentUri() {
            return CONTENT_URI;
        }
    }

    public enum ActivityEntry implements DbColumn, BaseColumns {
        _id(INTEGER, PRIMARY_KEY | AUTOINCREMENT),
        ACTIVITY_ID(TEXT, UNIQUE | NOT_NULL),
        ACTOR_ID(TEXT, NOT_NULL | DEFAULT_EMPTY_STRING),
        CONVERSATION_ID(TEXT, 0),
        ACTIVITY_PUBLISHED_TIME(INTEGER, 0),
        ACTIVITY_DATA(TEXT, 0),
        ACTIVITY_TYPE(INTEGER, 0),
        SOURCE(INTEGER, NOT_NULL | DEFAULT_0),
        CONTENT_DATA_ID(TEXT, 0),
        ENCRYPTION_KEY_URL(TEXT, 0),
        IS_ENCRYPTED(INTEGER, NOT_NULL | DEFAULT_0),
        SYNC_OPERATION_ID(TEXT, UNIQUE),
        IS_MENTION(INTEGER, NOT_NULL | DEFAULT_0),

        SYNC_STATE(INTEGER, 0),
        SPARK_WIDGET_MEETING_ID(TEXT, 0);

        // **DANGER** These are stored in the DB as ordinals, modify with care to avoid upgrade issues
        // When adding a new type be sure to revise the Activity.should* and getType functions as needed.
        public enum Type {
            _UNKNOWN(null),
            BACKFILL_GAP(null),  // used client-side only to keep track of holes in the stream. This gap triggers a backfill
            ACK(Verb.acknowledge),
            MESSAGE(Verb.post),
            ADD_PARTICIPANT(Verb.add),
            CREATE_CONVERSATION(Verb.create),
            UPDATE_TITLE_AND_SUMMARY(Verb.update),
            UPDATE_KEY(Verb.updateKey),
            LEFT_CONVERSATION(Verb.leave),
            PHOTO(Verb.share),
            FILE(Verb.share),
            MUTE(Verb.mute),
            UNMUTE(Verb.unmute),
            UPDATE_CONTENT(Verb.update),
            FAVORITE(Verb.favorite),
            UNFAVORITE(Verb.unfavorite),
            SCHEDULE_SPARK_MEETING(Verb.schedule),
            CALL_SESSION(Verb.update),
            TOMBSTONE(Verb.tombstone),
            FORWARDFILL_GAP(null), // used client-side only to keep track of holes in the stream. This gap triggers a forwardfill
            DEPRECATED_IMAGE_URI(Verb.post), // deprecated. Do not use.
            NEW_TEAM_CONVERSATION(Verb.add),
            SET_TEAM_COLOR(Verb.update),
            ASSIGN_ROOM_AVATAR(Verb.assign),
            REMOVE_ROOM_AVATAR(Verb.unassign),
            WHITEBOARD(Verb.share),
            TAG(Verb.tag),
            UNTAG(Verb.untag),
            ADD_LYRASPACE(null),
            REMOVE_LYRASPACE(null),
            UPDATE_SPARK_MEETING(Verb.update),
            DELETE_SPARK_MEETING(Verb.delete);

            public final String verb;

            Type(String verb) {
                this.verb = verb;
            }

            public Class<? extends Message> getSyncClass() {
                switch (this) {
                    case FILE:
                    case PHOTO:
                    case UPDATE_CONTENT:
                    case WHITEBOARD:
                        return DisplayableFileSet.class;
                    case SCHEDULE_SPARK_MEETING:
                    case UPDATE_SPARK_MEETING:
                    case DELETE_SPARK_MEETING:
                        return SparkMeetingWidget.class;
                    case ASSIGN_ROOM_AVATAR:
                    case REMOVE_ROOM_AVATAR:
                        return RoomAvatarAssignment.class;
                    case ACK:
                    case MESSAGE:
                    case ADD_PARTICIPANT:
                    case CREATE_CONVERSATION:
                    case UPDATE_TITLE_AND_SUMMARY:
                    case SET_TEAM_COLOR:
                    case UPDATE_KEY:
                    case LEFT_CONVERSATION:
                    case MUTE:
                    case UNMUTE:
                    case _UNKNOWN:
                    case BACKFILL_GAP:
                    default:
                        return Message.class;
            }
        }

        public boolean isEncryptable() {
            return this == MESSAGE
                    || this == PHOTO
                    || this == FILE
                    || this == UPDATE_TITLE_AND_SUMMARY
                    || this == ASSIGN_ROOM_AVATAR
                    || this == SET_TEAM_COLOR
                    || this == WHITEBOARD
                    || this == SCHEDULE_SPARK_MEETING
                    || this == UPDATE_SPARK_MEETING
                    || this == DELETE_SPARK_MEETING;
            }

            public boolean isSearchable() {
                return this == PHOTO
                        || this == FILE
                        || this == MESSAGE
                        || this == WHITEBOARD;
            }
        }

        // Source enum is ordered in descending order of reliability. For example if an activity arrives
        // via GCM and again later via Mercury, the Mercury version will overwrite the earlier one.
        //
        // **DANGER** These are stored in the DB as ordinals, modify with care to avoid upgrade issues
        public enum Source {

            // The activity was fetched as part of a catch-up or backfill sync; i.e. as part of
            // a block of activities that is guaranteed to be complete. Eventually all activities
            // are promoted to this state. The High Water Mark used by the sync logic is based on
            // the latest activity with source==SYNC.
            SYNC,

            // The activity was fetched as part of a catch-up or backfill sync, but that sync is
            // still in progress. This state is useful for breaking up huge DB transactions into
            // smaller ones while maintaining DB integrity. Once all the 'partial' transactions
            // are complete, a final step promotes SYNC_PARTIAL to SYNC.
            SYNC_PARTIAL,

            // Activity was received via Mercury. Eventually there will be a catch-up sync and it will
            // be promoted to SYNC.
            MERCURY,

            // Activity was pulled down as part of a Shell Sync task, used on startup to quickly fetch
            // a bare bones version of the conversations for speedy display.
            SHELL,

            // Activity was received via GCM, the least reliable transport.
            GCM,

            // Activity was generated locally. It should be updated when the activity is echoed back
            // from the server. Some data (e.g. timestamp, from the local device clock) is not reliable.
            LOCAL;

            public boolean isPushNotification() {
                return this == MERCURY || this == GCM;
            }
        }

        public static final String[] DEFAULT_PROJECTION = getProjection(values());

        public static final String TABLE_NAME = ActivityEntry.class.getSimpleName();
        public static final int URI_MATCHCODE = getNextMatchId();
        public static final int URI_IDMATCHCODE = URI_MATCHCODE + ID_URI_MATCH_ID_BASE;

        public static final Uri CONTENT_URI = Uri
                .parse("content://" + CONTENT_AUTHORITY + "/" + TABLE_NAME);

        public String type = "";
        int flags = 0;

        ActivityEntry(String type, int flags) {
            this.type = type;
            this.flags = flags;
        }

        @Override
        public String tablename() {
            return TABLE_NAME;
        }

        @Override
        public int uriMatchCode() {
            return URI_MATCHCODE;
        }

        @Override
        public int idUriMatchCode() {
            return URI_IDMATCHCODE;
        }

        @Override
        public String datatype() {
            return type;
        }

        @Override
        public int flags() {
            return flags;
        }

        @Override
        public String contentType() {
            return CONTENT_TYPE_PREFIX + TABLE_NAME.toLowerCase(Locale.US);
        }

        @Override
        public String contentItemType() {
            return CONTENT_ITEM_TYPE_PREFIX + TABLE_NAME.toLowerCase(Locale.US);
        }

        @Override
        public Uri contentUri() {
            return CONTENT_URI;
        }
    }

    public enum FlagEntry implements DbColumn, BaseColumns {
        _id(INTEGER, PRIMARY_KEY | AUTOINCREMENT),
        ACTIVITY_ID(TEXT, UNIQUE | NOT_NULL),
        FLAG_STATE(INTEGER, NOT_NULL | DEFAULT_0),
        FLAG_UPDATE_TIME(INTEGER, 0),
        FLAG_ID(TEXT, 0);

        public static final String[] DEFAULT_PROJECTION = getProjection(values());

        public static final String TABLE_NAME = FlagEntry.class.getSimpleName();
        public static final int URI_MATCHCODE = getNextMatchId();
        public static final int URI_IDMATCHCODE = URI_MATCHCODE + ID_URI_MATCH_ID_BASE;

        public static final Uri CONTENT_URI = Uri
                .parse("content://" + CONTENT_AUTHORITY + "/" + TABLE_NAME);

        public String type = "";
        int flags = 0;

        FlagEntry(String type, int flags) {
            this.type = type;
            this.flags = flags;
        }

        @Override
        public String tablename() {
            return TABLE_NAME;
        }

        @Override
        public int uriMatchCode() {
            return URI_MATCHCODE;
        }

        @Override
        public int idUriMatchCode() {
            return URI_IDMATCHCODE;
        }

        @Override
        public String datatype() {
            return type;
        }

        @Override
        public int flags() {
            return flags;
        }

        @Override
        public String contentType() {
            return CONTENT_TYPE_PREFIX + TABLE_NAME.toLowerCase(Locale.US);
        }

        @Override
        public String contentItemType() {
            return CONTENT_ITEM_TYPE_PREFIX + TABLE_NAME.toLowerCase(Locale.US);
        }

        @Override
        public Uri contentUri() {
            return CONTENT_URI;
        }
    }

    public enum ActorEntry implements DbColumn, BaseColumns {
        _id(INTEGER, PRIMARY_KEY | AUTOINCREMENT),
        ACTOR_UUID(TEXT, UNIQUE | NOT_NULL),
        EMAIL(TEXT, 0),
        DISPLAY_NAME(TEXT, 0),
        ENTITLEMENT_SQUARED(INTEGER, DEFAULT_0 | NOT_NULL),
        CI_NOTFOUND(INTEGER, DEFAULT_0 | NOT_NULL),
        ORG_ID(TEXT, 0),
        PRESENCE_STATUS(INTEGER, DEFAULT_0 | NOT_NULL),
        PRESENCE_LAST_ACTIVE(INTEGER, DEFAULT_0 | NOT_NULL),
        PRESENCE_EXPIRATION_DATE(INTEGER, DEFAULT_0 | NOT_NULL),
        RAW_CONTACT_ID(INTEGER, 0),                                 // Used if actor syncs to device contacts
        TYPE(TEXT, 0);

        public static final String[] DEFAULT_PROJECTION = getProjection(values());

        public static final String TABLE_NAME = ActorEntry.class.getSimpleName();
        public static final int URI_MATCHCODE = getNextMatchId();
        public static final int URI_IDMATCHCODE = URI_MATCHCODE + ID_URI_MATCH_ID_BASE;

        public static final Uri CONTENT_URI = Uri
                .parse("content://" + CONTENT_AUTHORITY + "/" + TABLE_NAME);

        public String type = "";
        int flags = 0;

        ActorEntry(String type, int flags) {
            this.type = type;
            this.flags = flags;
        }

        @Override
        public String tablename() {
            return TABLE_NAME;
        }

        @Override
        public int uriMatchCode() {
            return URI_MATCHCODE;
        }

        @Override
        public int idUriMatchCode() {
            return URI_IDMATCHCODE;
        }

        @Override
        public String datatype() {
            return type;
        }

        @Override
        public int flags() {
            return flags;
        }

        @Override
        public String contentType() {
            return CONTENT_TYPE_PREFIX + TABLE_NAME.toLowerCase(Locale.US);
        }

        @Override
        public String contentItemType() {
            return CONTENT_ITEM_TYPE_PREFIX + TABLE_NAME.toLowerCase(Locale.US);
        }

        @Override
        public Uri contentUri() {
            return CONTENT_URI;
        }
    }

    public enum SyncOperationEntry implements DbColumn, BaseColumns {
        _id(INTEGER, PRIMARY_KEY | AUTOINCREMENT),
        SYNC_OPERATION_ID(TEXT, NOT_NULL | UNIQUE),
        RETRIES(INTEGER, NOT_NULL | DEFAULT_0),     // used only by SyncOperationManager
        OPERATION_TYPE(INTEGER, 0),                    //  ActionType enum
        SYNC_STATE(INTEGER, NOT_NULL | DEFAULT_0),  //  SyncState enum
        START_TIME(INTEGER, NOT_NULL | DEFAULT_0),
        DATA(TEXT, 0);

        // **DANGER** These are stored in the DB as ordinals, modify with care to avoid upgrade issues

        public enum SyncState {
            /**
             * The initial state. Includes the operation's onEnqueue() method, notifying other
             * operations that it's been added, writing it to the SyncOperationEntry table and
             * adding to the in memory queue. At the end of this state the operation will have a
             * local representation in the UI (e.g. message is displayed, conversation marked read,
             * etc).  Ideally the operation is only in this state for <100ms.
             */
            UNINITIALIZED,

            /**
             * The operation is not ready because some condition has not been met; for example
             * a depended operation is incomplete or the network is unavailable and we cannot
             * proceed without it.  See Operation.checkPrepared()
             */
            PREPARING,

            /**
             * The operation is waiting for doWork() to be called.  Reasons for the operation to
             * be in this state:
             * <p/>
             * - The OperationWalker thread is busy with other operations
             * - This operation depends on another operation still in progress
             * - The operation requires network connectivity and we're offline
             * - The operation is waiting for its retry delay after failing
             */
            READY,

            /**
             * The operation is still executing. Operations can execute synchronously when the
             * OperationWalker thread calls doWork() or asynchronously by spinning up a thread in
             * doWork() before returning.
             * <p/>
             * When an operation runs asynchronously it will receive periodic calls to
             * checkProgress() instead of doWork() as long as it remains in the EXECUTING state.
             */
            EXECUTING,

            /**
             * Terminal state. The operation has failed. Operation.getFailureReason() may have more
             * information. See SyncStateFailureReason enum below.
             * <p/>
             * Operations may be resurrected from this state with restartOperation()
             */
            FAULTED,

            /**
             * Terminal state. The operation has completed successfully.
             */
            SUCCEEDED;

            public boolean isTerminal() {
                return this == FAULTED || this == SUCCEEDED;
            }

            public boolean isPreExecute() {
                return this == UNINITIALIZED || this == PREPARING || this == READY;
            }

            public boolean isIdle() {
                return this.isTerminal() || this == PREPARING;
            }
        }

        public enum SyncStateFailureReason {
            /**
             * The operation failed for an unknown reason.
             */
            UNKNOWN,

            /**
             * The opperation is in a suceeded/non-terminal state.
             */
            NONE,

            /**
             * The operation was canceled without executing because it is no longer necessary.
             */
            CANCELED,

            /**
             * The operation will never succeed because a dependency failed.
             */
            DEPENDENCY,

            /**
             * The operation ran out of retry attempts.
             */
            NO_MORE_RETRIES,

            /**
             * The operation timed out
             */
            TIMED_OUT,

            /**
             * The operation threw an exception
             */
            EXCEPTION,

            /**
             * The operation failed due to an invalid response.
             */
            INVALID_RESPONSE
        }

        // **DANGER** These are stored in the DB as ordinals, modify with care to avoid upgrade issues
        public enum OperationType {
            ADD_PARTICIPANT(AddPersonOperation.class),
            ASSIGN_MODERATOR(ToggleParticipantActivityOperation.class),
            CONTENT_UPLOAD(ContentUploadOperation.class),
            CONVERSATION_FAVORITE(ToggleActivityOperation.class),
            CONVERSATION_HIDE(ToggleActivityOperation.class),
            CONVERSATION_LOCK(ToggleActivityOperation.class),
            CONVERSATION_MUTE(ToggleActivityOperation.class),
            CONVERSATION_TITLE_AND_SUMMARY(SetTitleAndSummaryOperation.class),
            CREATE_CONVERSATION(NewConversationOperation.class),
            DELETE_ACTIVITY(DeleteActivityOperation.class),
            FEATURE_TOGGLE(FeatureToggleOperation.class),
            FETCH_KEY(KeyFetchOperation.class),
            FETCH_SPACE_URL(FetchSpaceUrlOperation.class),
            FETCH_UNBOUND_KEYS(UnboundKeyFetchOperation.class),
            MAP_EVENT_TO_CONVERSATION(MapEventToConversationOperation.class),
            MARK_CONVERSATION_READ(MarkReadOperation.class),
            MESSAGE(PostCommentOperation.class),
            MESSAGE_WITH_CONTENT(PostContentActivityOperation.class),
            REMOVE_PARTICIPANT(RemoveParticipantOperation.class),
            SCHEDULED_EVENT(ScheduledEventActivityOperation.class),
            SEND_DTMF(SendDtmfOperation.class),
            SET_AVATAR(AvatarUpdateOperation.class),
            SET_UP_SHARED_KMS_KEY(SetupSharedKeyWithKmsOperation.class),
            DEPRECATED_STICKIES(null), // Do not use. Remove next time minimum schema version is increased.
            UPDATE_ENCRYPTION_KEY(UpdateEncryptionKeyOperation.class),
            VIDEO_THUMBNAIL(VideoThumbnailOperation.class),
            ACTIVITY_FILL(ActivityFillOperation.class),
            FETCH_ACTIVITY_CONTEXT(FetchActivityContextOperation.class),
            FETCH_MENTIONS(FetchMentionsOperation.class),
            REGISTER_DEVICE(RegisterDeviceOperation.class),
            FETCH_UNJOINED_TEAMS(FetchUnjoinedTeamRoomsOperation.class),
            DEPRECATED_KMS_CREATE_RESOURCE(null),
            CONVERSATION_ARCHIVE(ToggleActivityOperation.class),
            REMOTE_SEARCH_QUERY(RemoteSearchOperation.class),
            JOIN_TEAM_ROOM(JoinTeamRoomOperation.class),
            DEPRECATED_FETCH_STICKY_PACK(null), // Do not use. Remove next time minimum schema version is increased.
            SET_TEAM_COLOR(UpdateTeamColorOperation.class),
            FETCH_USER_PRESENCE(FetchPresenceStatusOperation.class),
            SUSBSCRIBE_USER_PRESENCE(SubscribePresenceStatusOperation.class),
            MOVE_ROOM_TO_TEAM(MoveRoomToTeamOperation.class),
            CREATE_CONVERSATION_WITH_REPOSTS(NewConversationWithRepostedMessagesOperation.class),
            TAG(TagOperation.class),
            UNSET_FEATURE_TOGGLE(UnsetFeatureToggleOperation.class),
            INTEGRATE_CONTACTS(IntegrateContactsOperation.class),
            INCREMENT_SHARE_COUNT(IncrementShareCountOperation.class),
            ASSIGN_ROOM_AVATAR(AssignRoomAvatarOperation.class),
            REMOVE_ROOM_AVATAR(RemoveRoomAvatarOperation.class),
            FLAG(FlagOperation.class),
            SYNC_FLAGs(SyncFlagsOperation.class),
            SEND_PRESENCE_EVENT(SendPresenceEventOperation.class),
            REFRESH_TOKEN(TokenRefreshOperation.class),
            REVOKE_TOKEN(TokenRevokeOperation.class),
            GET_AVATAR_URLS(GetAvatarUrlsOperation.class),
            INCREMENTAL_SYNC(CatchUpSyncOperation.class),
            GENERAL_SYNC_TASK(SyncTaskOperation.class),
            FETCH_PARTICIPANTS(FetchParticipantsOperation.class),
            POST_GENERIC_METRIC(PostGenericMetricOperation.class),
            GET_RETENTION_POLICY(GetRetentionPolicyInfoOperation.class),
            FETCH_ACKS(FetchAcksOperation.class),
            AUDIO_MUTE(AudioMuteOperation.class),
            AUDIO_VOLUME(AudioVolumeOperation.class),
            TAG_NOTIFICATIONS(CustomNotificationsTagOperation.class),
            DELETE_WHITEBOARD(null),// Do not use. Remove next time minimum schema version is increased.
            ROOM_BIND(RoomBindOperation.class);

            public final Class<? extends Operation> operationClass;

            OperationType(Class<? extends Operation> operationClass) {
                this.operationClass = operationClass;
            }

            public boolean isMessage() {
                return this == MESSAGE_WITH_CONTENT || this == MESSAGE;
            }
        }

        public static final String[] DEFAULT_PROJECTION = getProjection(values());

        public static final String TABLE_NAME = SyncOperationEntry.class.getSimpleName();
        public static final int URI_MATCHCODE = getNextMatchId();
        public static final int URI_IDMATCHCODE = URI_MATCHCODE + ID_URI_MATCH_ID_BASE;

        public static final Uri CONTENT_URI = Uri
                .parse("content://" + CONTENT_AUTHORITY + "/" + TABLE_NAME);

        public String type = "";
        int flags = 0;

        SyncOperationEntry(String type, int flags) {
            this.type = type;
            this.flags = flags;
        }

        @Override
        public String tablename() {
            return TABLE_NAME;
        }

        @Override
        public int uriMatchCode() {
            return URI_MATCHCODE;
        }

        @Override
        public int idUriMatchCode() {
            return URI_IDMATCHCODE;
        }

        @Override
        public String datatype() {
            return type;
        }

        @Override
        public int flags() {
            return flags;
        }

        @Override
        public String contentType() {
            return CONTENT_TYPE_PREFIX + TABLE_NAME.toLowerCase(Locale.US);
        }

        @Override
        public String contentItemType() {
            return CONTENT_ITEM_TYPE_PREFIX + TABLE_NAME.toLowerCase(Locale.US);
        }

        @Override
        public Uri contentUri() {
            return CONTENT_URI;
        }

    }

    /*
     * Mapping table for actors : conversations, including read receipts
     */
    public enum ParticipantEntry implements DbColumn, BaseColumns {
        _id(INTEGER, PRIMARY_KEY | AUTOINCREMENT),
        CONVERSATION_ID(TEXT, NOT_NULL | COMPLEX_KEY),
        ACTOR_UUID(TEXT, NOT_NULL | COMPLEX_KEY),
        LASTACK_ACTIVITY_ID(TEXT, 0),                          // Latest activity ID acknowledged by this user in this room
        LASTACK_ACTIVITY_TIME(INTEGER, NOT_NULL | DEFAULT_0),  // Publish time of lastack_activity_id
        LAST_ACTIVE_TIME(INTEGER, NOT_NULL | DEFAULT_0),       // Last time this participant did something interesting in this room
        MEMBERSHIP_STATE(INTEGER, NOT_NULL | DEFAULT_0),       // Ordinal value from MembershipState, default ACTIVE
        TIME_MODIFIED(INTEGER, NOT_NULL | TIMESTAMP),
        IS_MODERATOR(INTEGER, NOT_NULL | DEFAULT_0);

        public static final String[] DEFAULT_PROJECTION = getProjection(values());

        public static final String TABLE_NAME = ParticipantEntry.class.getSimpleName();
        public static final int URI_MATCHCODE = getNextMatchId();
        public static final int URI_IDMATCHCODE = URI_MATCHCODE + ID_URI_MATCH_ID_BASE;

        public static final Uri CONTENT_URI = Uri
                .parse("content://" + CONTENT_AUTHORITY + "/" + TABLE_NAME);

        // **DANGER** These are stored in the DB as ordinals, modify with care to avoid upgrade issues
        public static enum MembershipState {
            ACTIVE,
            LEFT
        }

        public String type = "";
        int flags = 0;

        ParticipantEntry(String type, int flags) {

            this.type = type;
            this.flags = flags;
        }

        @Override
        public String tablename() {
            return TABLE_NAME;
        }

        @Override
        public int uriMatchCode() {
            return URI_MATCHCODE;
        }

        @Override
        public int idUriMatchCode() {
            return URI_IDMATCHCODE;
        }

        @Override
        public String datatype() {
            return type;
        }

        @Override
        public int flags() {
            return flags;
        }

        @Override
        public String contentType() {
            return CONTENT_TYPE_PREFIX + TABLE_NAME.toLowerCase(Locale.US);
        }

        @Override
        public String contentItemType() {
            return CONTENT_ITEM_TYPE_PREFIX + TABLE_NAME.toLowerCase(Locale.US);
        }

        @Override
        public Uri contentUri() {
            return CONTENT_URI;
        }
    }

    public enum OrganizationEntry implements DbColumn, BaseColumns {
        _id(INTEGER, PRIMARY_KEY | AUTOINCREMENT),
        ORG_ID(TEXT, NOT_NULL | UNIQUE),
        ORG_NAME(TEXT, DEFAULT_EMPTY_STRING),
        RETENTION_URL(TEXT, DEFAULT_EMPTY_STRING),
        RETENTION_DAYS(INTEGER, DEFAULT_0),
        LAST_RETENTION_SYNC_TIMESTAMP(INTEGER, DEFAULT_0);

        public static final String[] DEFAULT_PROJECTION = getProjection(values());

        public static final String TABLE_NAME = OrganizationEntry.class.getSimpleName();
        public static final int URI_MATCHCODE = getNextMatchId();
        public static final int URI_IDMATCHCODE = URI_MATCHCODE + ID_URI_MATCH_ID_BASE;

        public static final Uri CONTENT_URI = Uri
                .parse("content://" + CONTENT_AUTHORITY + "/" + TABLE_NAME);

        public String type = "";
        int flags = 0;

        OrganizationEntry(String type, int flags) {
            this.type = type;
            this.flags = flags;
        }

        @Override
        public String tablename() {
            return TABLE_NAME;
        }

        @Override
        public int uriMatchCode() {
            return URI_MATCHCODE;
        }

        @Override
        public int idUriMatchCode() {
            return URI_IDMATCHCODE;
        }

        @Override
        public String datatype() {
            return type;
        }

        @Override
        public int flags() {
            return flags;
        }

        @Override
        public String contentType() {
            return CONTENT_TYPE_PREFIX + TABLE_NAME.toLowerCase(Locale.US);
        }

        @Override
        public String contentItemType() {
            return CONTENT_ITEM_TYPE_PREFIX + TABLE_NAME.toLowerCase(Locale.US);
        }

        @Override
        public Uri contentUri() {
            return CONTENT_URI;
        }
    }

    public enum ContentDataCacheEntry implements DbColumn, BaseColumns {
        _id(INTEGER, PRIMARY_KEY | AUTOINCREMENT),
        DATA_LOCAL_URI(TEXT, 0),
        DATA_REMOTE_URI(TEXT, NOT_NULL | COMPLEX_KEY),
        DATA_REAL_URI(TEXT, DEFAULT_EMPTY_STRING),
        DATA_SIZE(INTEGER, 0),
        TYPE(INTEGER, DEFAULT_0 | COMPLEX_KEY), // ordinal from Cache enum
        LAST_ACCESS(INTEGER, DEFAULT_0),
        REMOTE_LAST_MODIFIED(TEXT, 0);

        // **DANGER** These are stored in the DB as ordinals, modify with care to avoid upgrade issues
        public enum Cache {
            THUMBNAIL,
            MEDIA,
            AVATAR
        }

        public static final String[] DEFAULT_PROJECTION = getProjection(values());

        public static final String TABLE_NAME = ContentDataCacheEntry.class.getSimpleName();
        public static final int URI_MATCHCODE = getNextMatchId();
        public static final int URI_IDMATCHCODE = URI_MATCHCODE + ID_URI_MATCH_ID_BASE;

        public static final Uri CONTENT_URI = Uri
                .parse("content://" + CONTENT_AUTHORITY + "/" + TABLE_NAME);

        public String type = "";
        int flags = 0;

        ContentDataCacheEntry(String type, int flags) {

            this.type = type;
            this.flags = flags;
        }

        @Override
        public String tablename() {
            return TABLE_NAME;
        }

        @Override
        public int uriMatchCode() {
            return URI_MATCHCODE;
        }

        @Override
        public int idUriMatchCode() {
            return URI_IDMATCHCODE;
        }

        @Override
        public String datatype() {
            return type;
        }

        @Override
        public int flags() {
            return flags;
        }

        @Override
        public String contentType() {
            return CONTENT_TYPE_PREFIX + TABLE_NAME.toLowerCase(Locale.US);
        }

        @Override
        public String contentItemType() {
            return CONTENT_ITEM_TYPE_PREFIX + TABLE_NAME.toLowerCase(Locale.US);
        }

        @Override
        public Uri contentUri() {
            return CONTENT_URI;
        }
    }

    /**
     * A mapping between conversations and meetings. MEETING_ID is arbitrary and is currently just
     * the meeting title. It needs to be more robust to avoid false positives.
     */
    public enum ConversationMeetingEntry implements DbColumn, BaseColumns {
        _id(INTEGER, PRIMARY_KEY | AUTOINCREMENT),
        CONVERSATION_ID(TEXT, NOT_NULL),
        MEETING_ID(TEXT, NOT_NULL);

        public static final String[] DEFAULT_PROJECTION = getProjection(values());

        public static final String TABLE_NAME = ConversationMeetingEntry.class.getSimpleName();
        public static final int URI_MATCHCODE = getNextMatchId();
        public static final int URI_IDMATCHCODE = URI_MATCHCODE + ID_URI_MATCH_ID_BASE;

        public static final Uri CONTENT_URI = Uri
                .parse("content://" + CONTENT_AUTHORITY + "/" + TABLE_NAME);

        public String type = "";
        int flags = 0;

        ConversationMeetingEntry(String type, int flags) {
            this.type = type;
            this.flags = flags;
        }

        @Override
        public String tablename() {
            return TABLE_NAME;
        }

        @Override
        public int uriMatchCode() {
            return URI_MATCHCODE;
        }

        @Override
        public int idUriMatchCode() {
            return URI_IDMATCHCODE;
        }

        @Override
        public String datatype() {
            return type;
        }

        @Override
        public int flags() {
            return flags;
        }

        @Override
        public String contentType() {
            return CONTENT_TYPE_PREFIX + TABLE_NAME.toLowerCase(Locale.US);
        }

        @Override
        public String contentItemType() {
            return CONTENT_ITEM_TYPE_PREFIX + TABLE_NAME.toLowerCase(Locale.US);
        }

        @Override
        public Uri contentUri() {
            return CONTENT_URI;
        }
    }

    public enum CallHistory implements DbColumn, BaseColumns {
        _id(INTEGER, PRIMARY_KEY | AUTOINCREMENT),
        URL(TEXT, NOT_NULL | UNIQUE),
        START_TIME(INTEGER, NOT_NULL | TIMESTAMP),
        END_TIME(INTEGER, NOT_NULL | TIMESTAMP),
        DURATION_SECS(INTEGER, NOT_NULL | DEFAULT_0),
        JOINED_DURATION_SECS(INTEGER, NOT_NULL | DEFAULT_0),
        DIRECTION(TEXT, NOT_NULL),
        DISPOSTION(TEXT, NOT_NULL),
        PARTICIPANT_COUNT(INTEGER, NOT_NULL | DEFAULT_0),
        CALLBACK_ADDRESS(TEXT, 0),
        OTHER_UUID(TEXT, 0),
        OTHER_EMAIL(TEXT, 0),
        OTHER_NAME(TEXT, 0),
        OTHER_IS_EXTERNAL(INTEGER, NOT_NULL | DEFAULT_0),
        OTHER_PRIMARY_DISPLAY_STRING(TEXT, 0),
        OTHER_SECONDARY_DISPLAY_STRING(TEXT, 0),
        OTHER_PHONE_NUMBER(TEXT, 0),
        OTHER_SIP_URL(TEXT, 0),
        OTHER_TEL_URL(TEXT, 0),
        OTHER_OWNER_ID(TEXT, 0),
        CONVERSATION_URL(TEXT, 0);


        public static final String[] DEFAULT_PROJECTION = getProjection(values());

        public static final String TABLE_NAME = CallHistory.class.getSimpleName();
        public static final int URI_MATCHCODE = getNextMatchId();
        public static final int URI_IDMATCHCODE = URI_MATCHCODE + ID_URI_MATCH_ID_BASE;

        public static final Uri CONTENT_URI = Uri
                .parse("content://" + CONTENT_AUTHORITY + "/" + TABLE_NAME);

        public String type = "";
        int flags = 0;

        CallHistory(String type, int flags) {
            this.type = type;
            this.flags = flags;
        }

        @Override
        public String tablename() {
            return TABLE_NAME;
        }

        @Override
        public int uriMatchCode() {
            return URI_MATCHCODE;
        }

        @Override
        public int idUriMatchCode() {
            return URI_IDMATCHCODE;
        }

        @Override
        public String datatype() {
            return type;
        }

        @Override
        public int flags() {
            return flags;
        }

        @Override
        public String contentType() {
            return CONTENT_TYPE_PREFIX + TABLE_NAME.toLowerCase(Locale.US);
        }

        @Override
        public String contentItemType() {
            return CONTENT_ITEM_TYPE_PREFIX + TABLE_NAME.toLowerCase(Locale.US);
        }

        @Override
        public Uri contentUri() {
            return CONTENT_URI;
        }
    }

   /*
    * Mapping between Encryption Key Uris and key value/key ID
    *
    */

    public enum EncryptionKeyEntry implements DbColumn, BaseColumns {
        _id(INTEGER, PRIMARY_KEY | AUTOINCREMENT),
        ENCRYPTION_KEY(TEXT, 0),
        ENCRYPTION_KEY_URI(TEXT, NOT_NULL | UNIQUE),
        ENCRYPTION_KEY_ID(TEXT, 0),
        ENCRYPTION_KEY_EXPIRY_TIME(INTEGER, 0);

        public static final String[] DEFAULT_PROJECTION = getProjection(values());

        public static final String TABLE_NAME = EncryptionKeyEntry.class.getSimpleName();
        public static final int URI_MATCHCODE = getNextMatchId();
        public static final int URI_IDMATCHCODE = URI_MATCHCODE + ID_URI_MATCH_ID_BASE;

        public static final Uri CONTENT_URI = Uri
                .parse("content://" + CONTENT_AUTHORITY + "/" + TABLE_NAME);

        public String type = "";
        int flags = 0;

        EncryptionKeyEntry(String type, int flags) {
            this.type = type;
            this.flags = flags;
        }

        @Override
        public String tablename() {
            return TABLE_NAME;
        }

        @Override
        public int uriMatchCode() {
            return URI_MATCHCODE;
        }

        @Override
        public int idUriMatchCode() {
            return URI_IDMATCHCODE;
        }

        @Override
        public String datatype() {
            return type;
        }

        @Override
        public int flags() {
            return flags;
        }

        @Override
        public String contentType() {
            return CONTENT_TYPE_PREFIX + TABLE_NAME.toLowerCase(Locale.US);
        }

        @Override
        public String contentItemType() {
            return CONTENT_ITEM_TYPE_PREFIX + TABLE_NAME.toLowerCase(Locale.US);
        }

        @Override
        public Uri contentUri() {
            return CONTENT_URI;
        }
    }

    /* Serves as content table for Message search FTS
    *
    */

    public enum MessageSearchDataEntry implements DbColumn, BaseColumns {
        _id(INTEGER, PRIMARY_KEY | AUTOINCREMENT),
        suggest_text_1(TEXT, 0),
        suggest_intent_data(TEXT, 0),
        ACTIVITY_ID(TEXT, NOT_NULL | UNIQUE),
        CONVERSATION_NAME(TEXT, 0),
        MESSAGE_TEXT(TEXT, NOT_NULL | DEFAULT_0),
        MESSAGE_POSTED_BY(TEXT, 0),
        MESSAGE_PUBLISHED_TIME(INTEGER, 0),
        MESSAGE_POSTED_BY_UUID(TEXT, 0),
        suggest_intent_extra_data(TEXT, 0),
        FILES_SHARED_COUNT(INTEGER, 0),
        ONE_ON_ONE_PARTICIPANT(TEXT, 0);

        public static final String[] DEFAULT_PROJECTION = getProjection(values());

        public static final String TABLE_NAME = MessageSearchDataEntry.class.getSimpleName();
        public static final int URI_MATCHCODE = getNextMatchId();
        public static final int URI_IDMATCHCODE = URI_MATCHCODE + ID_URI_MATCH_ID_BASE;

        public static final Uri CONTENT_URI = Uri
                .parse("content://" + CONTENT_AUTHORITY + "/" + TABLE_NAME);

        public String type = "";
        int flags = 0;

        MessageSearchDataEntry(String type, int flags) {
            this.type = type;
            this.flags = flags;
        }

        @Override
        public String tablename() {
            return TABLE_NAME;
        }

        @Override
        public int uriMatchCode() {
            return URI_MATCHCODE;
        }

        @Override
        public int idUriMatchCode() {
            return URI_IDMATCHCODE;
        }

        @Override
        public String datatype() {
            return type;
        }

        @Override
        public int flags() {
            return flags;
        }

        @Override
        public String contentType() {
            return CONTENT_TYPE_PREFIX + TABLE_NAME.toLowerCase(Locale.US);
        }

        @Override
        public String contentItemType() {
            return CONTENT_ITEM_TYPE_PREFIX + TABLE_NAME.toLowerCase(Locale.US);
        }

        @Override
        public Uri contentUri() {
            return CONTENT_URI;
        }
    }

     /*
    * Serves as content table for content search FTS
    *
    */

    public enum ContentSearchDataEntry implements DbColumn, BaseColumns {
        _id(INTEGER, PRIMARY_KEY | AUTOINCREMENT),
        suggest_text_1(TEXT, 0),
        suggest_intent_data(TEXT, 0),
        ACTIVITY_ID(TEXT, UNIQUE | NOT_NULL),
        CONTENT_TITLE(TEXT, NOT_NULL | DEFAULT_0),
        CONTENT_SHARED_BY(TEXT, 0),
        CONTENT_TYPE(TEXT, 0),
        CONTENT_PUBLISHED_TIME(INTEGER, 0),
        suggest_intent_extra_data(TEXT, 0),
        CONVERSATION_NAME(TEXT, 0),
        CONTENT_POSTED_BY_UUID(TEXT, 0);

        public static final String[] DEFAULT_PROJECTION = getProjection(values());

        public static final String TABLE_NAME = ContentSearchDataEntry.class.getSimpleName();
        public static final int URI_MATCHCODE = getNextMatchId();
        public static final int URI_IDMATCHCODE = URI_MATCHCODE + ID_URI_MATCH_ID_BASE;

        public static final Uri CONTENT_URI = Uri
                .parse("content://" + CONTENT_AUTHORITY + "/" + TABLE_NAME);

        public String type = "";
        int flags = 0;


        ContentSearchDataEntry(String type, int flags) {
            this.type = type;
            this.flags = flags;
        }

        @Override
        public String tablename() {
            return TABLE_NAME;
        }

        @Override
        public int uriMatchCode() {
            return URI_MATCHCODE;
        }

        @Override
        public int idUriMatchCode() {
            return URI_IDMATCHCODE;
        }

        @Override
        public String datatype() {
            return type;
        }

        @Override
        public int flags() {
            return flags;
        }

        @Override
        public String contentType() {
            return CONTENT_TYPE_PREFIX + TABLE_NAME.toLowerCase(Locale.US);
        }

        @Override
        public String contentItemType() {
            return CONTENT_ITEM_TYPE_PREFIX + TABLE_NAME.toLowerCase(Locale.US);
        }

        @Override
        public Uri contentUri() {
            return CONTENT_URI;
        }
    }

    public enum TeamEntry implements DbColumn, BaseColumns {
        _id(INTEGER, PRIMARY_KEY | AUTOINCREMENT),
        TEAM_ID(TEXT, NOT_NULL | UNIQUE),
        COLOR(TEXT, 0),
        PRIMARY_CONVERSATION_ID(TEXT, NOT_NULL);

        public static final String[] DEFAULT_PROJECTION = getProjection(values());

        public static final String TABLE_NAME = TeamEntry.class.getSimpleName();
        public static final int URI_MATCHCODE = getNextMatchId();
        public static final int URI_IDMATCHCODE = URI_MATCHCODE + ID_URI_MATCH_ID_BASE;

        public static final Uri CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY + "/" + TABLE_NAME);

        public String type = "";
        int flags = 0;

        TeamEntry(String type, int flags) {
            this.type = type;
            this.flags = flags;
        }

        @Override
        public String tablename() {
            return TABLE_NAME;
        }

        @Override
        public int uriMatchCode() {
            return URI_MATCHCODE;
        }

        @Override
        public int idUriMatchCode() {
            return URI_IDMATCHCODE;
        }

        @Override
        public String datatype() {
            return type;
        }

        @Override
        public int flags() {
            return flags;
        }

        @Override
        public String contentType() {
            return CONTENT_TYPE_PREFIX + TABLE_NAME.toLowerCase(Locale.US);
        }

        @Override
        public String contentItemType() {
            return CONTENT_ITEM_TYPE_PREFIX + TABLE_NAME.toLowerCase(Locale.US);
        }

        @Override
        public Uri contentUri() {
            return CONTENT_URI;
        }

    }

    public enum LocusMeetingInfoEntry implements DbColumn, BaseColumns {
        _id(INTEGER, PRIMARY_KEY | AUTOINCREMENT),
        LOCUS_URL(TEXT, UNIQUE),
        LOCUS_ID(TEXT, UNIQUE),
        WEBEX_MEETING_URL(TEXT, UNIQUE),
        SIP_MEETING_URI(TEXT, UNIQUE),
        OWNER_ID(TEXT, 0),
        MEETING_NUMBER(TEXT, 0),
        MEETING_NAME(TEXT, 0),
        NUMERIC_CODE(TEXT, 0),
        URI(TEXT, 0),
        LOCAL_DIAL_IN_NUMBER(TEXT, 0),
        CALL_IN_NUMBER_INFO(TEXT, 0),
        IS_PMR(INTEGER, NOT_NULL | DEFAULT_0),
        LAST_WRITE_TIME(INTEGER, NOT_NULL);

        public static final String[] DEFAULT_PROJECTION = getProjection(values());

        public static final String TABLE_NAME = LocusMeetingInfoEntry.class.getSimpleName();
        public static final int URI_MATCHCODE = getNextMatchId();
        public static final int URI_IDMATCHCODE = URI_MATCHCODE + ID_URI_MATCH_ID_BASE;

        public static final Uri CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY + "/" + TABLE_NAME);

        public String type = "";
        int flags = 0;

        LocusMeetingInfoEntry(String type, int flags) {
            this.type = type;
            this.flags = flags;
        }

        @Override
        public String tablename() {
            return TABLE_NAME;
        }

        @Override
        public int uriMatchCode() {
            return URI_MATCHCODE;
        }

        @Override
        public int idUriMatchCode() {
            return URI_IDMATCHCODE;
        }

        @Override
        public String datatype() {
            return type;
        }

        @Override
        public int flags() {
            return flags;
        }

        @Override
        public String contentType() {
            return CONTENT_TYPE_PREFIX + TABLE_NAME.toLowerCase(Locale.US);
        }

        @Override
        public String contentItemType() {
            return CONTENT_ITEM_TYPE_PREFIX + TABLE_NAME.toLowerCase(Locale.US);
        }

        @Override
        public Uri contentUri() {
            return CONTENT_URI;
        }

    }

    public enum CalendarMeetingInfoEntry implements DbColumn, BaseColumns {
        _id(INTEGER, PRIMARY_KEY | AUTOINCREMENT),
        CALENDAR_MEETING_ID(TEXT, UNIQUE | NOT_NULL),
        SERIES_ID(TEXT, DEFAULT_0),
        START_TIME(INTEGER, DEFAULT_0),
        DURATION_MINUTES(INTEGER, DEFAULT_0),
        ORGANIZER_ID(TEXT, 0),
        ENCRYPTION_KEY_URL(TEXT, 0),
        SUBJECT(TEXT, 0),
        IS_RECURRING(INTEGER, NOT_NULL | DEFAULT_0),
        CALL_URI(TEXT, 0),
        NOTES(TEXT, 0),
        LOCATION(TEXT, 0),
        LINKS(TEXT, 0),
        PARTICIPANTS(TEXT, 0),
        MEETING_SENSITIVITY(TEXT, 0),
        CONVERSATION_ID(TEXT, 0),
        SPACE_URI(TEXT, 0),
        SPACE_URL(TEXT, 0),
        SPACE_MEET_URL(TEXT, 0),
        WEBEX_URI(TEXT, 0),
        WEBEX_URL(TEXT, 0),
        IS_DELETED(INTEGER, NOT_NULL | DEFAULT_0),
        LAST_MODIFIED_TIME(INTEGER, DEFAULT_0),
        IS_ENCRYPTED(INTEGER, NOT_NULL | DEFAULT_0);

        public static final String[] DEFAULT_PROJECTION = getProjection(values());

        public static final String TABLE_NAME = CalendarMeetingInfoEntry.class.getSimpleName();
        public static final int URI_MATCHCODE = getNextMatchId();
        public static final int URI_IDMATCHCODE = URI_MATCHCODE + ID_URI_MATCH_ID_BASE;

        public static final Uri CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY + "/" + TABLE_NAME);

        public String type = "";
        int flags = 0;

        CalendarMeetingInfoEntry(String type, int flags) {
            this.type = type;
            this.flags = flags;
        }

        @Override
        public String tablename() {
            return TABLE_NAME;
        }

        @Override
        public int uriMatchCode() {
            return URI_MATCHCODE;
        }

        @Override
        public int idUriMatchCode() {
            return URI_IDMATCHCODE;
        }

        @Override
        public String datatype() {
            return type;
        }

        @Override
        public int flags() {
            return flags;
        }

        @Override
        public String contentType() {
            return CONTENT_TYPE_PREFIX + TABLE_NAME.toLowerCase(Locale.US);
        }

        @Override
        public String contentItemType() {
            return CONTENT_ITEM_TYPE_PREFIX + TABLE_NAME.toLowerCase(Locale.US);
        }

        @Override
        public Uri contentUri() {
            return CONTENT_URI;
        }
    }

    /* Serves as content table for Conversation search FTS
    *
    */
    public enum ConversationSearchDataEntry implements DbColumn, BaseColumns {
        _id(INTEGER, PRIMARY_KEY | AUTOINCREMENT),
        suggest_text_1(TEXT, 0),
        suggest_intent_data(TEXT, UNIQUE | NOT_NULL),           //Contains the conversation id
        CONVERSATION_NAME(TEXT, NOT_NULL | DEFAULT_0),
        PARTICIPANT_NAMES(TEXT, NOT_NULL | DEFAULT_0),
        PARTICIPANT_EMAILS(TEXT, NOT_NULL | DEFAULT_0),
        suggest_intent_extra_data(TEXT, 0),
        LAST_ACTIVE_TIME(INTEGER, 0),
        LOCUS_URL(TEXT, 0),
        ONE_ON_ONE_PARTICIPANT_UUID(TEXT, 0);

        // Suggestions use alpha ASC ordering so name these types appropriately
        public static final String TYPE_ONE_ON_ONE = "A_ONE_ON_ONE";
        public static final String TYPE_GROUP = "B_GROUP";

        public static final String SNIP_NAME = "SNIP_NAME";
        public static final String OFFSETS = "OFFSETS";
        public static final String NAME_DELIMITER = ";";

        public static final String[] DEFAULT_PROJECTION = getProjection(values());


        public static final String TABLE_NAME = ConversationSearchDataEntry.class.getSimpleName();
        public static final int URI_MATCHCODE = getNextMatchId();
        public static final int URI_IDMATCHCODE = URI_MATCHCODE + ID_URI_MATCH_ID_BASE;

        public static final String[] PROJECTION_WITH_SNIPPET =
                getProjection(values(),
                        "SNIPPET(" + TABLE_NAME + ", \"<b>\", \"</b>\", \"<b>...</b>\", " + CONVERSATION_NAME.ordinal() + ") AS " + SNIP_NAME,
                        "OFFSETS(" + TABLE_NAME + ") AS " + OFFSETS);

        public static final Uri CONTENT_URI = Uri
                .parse("content://" + CONTENT_AUTHORITY + "/" + TABLE_NAME);

        public String type = "";
        int flags = 0;

        ConversationSearchDataEntry(String type, int flags) {
            this.type = type;
            this.flags = flags;
        }

        @Override
        public String tablename() {
            return TABLE_NAME;
        }

        @Override
        public int uriMatchCode() {
            return URI_MATCHCODE;
        }

        @Override
        public int idUriMatchCode() {
            return URI_IDMATCHCODE;
        }

        @Override
        public String datatype() {
            return type;
        }

        @Override
        public int flags() {
            return flags;
        }

        @Override
        public String contentType() {
            return CONTENT_TYPE_PREFIX + TABLE_NAME.toLowerCase(Locale.US);
        }

        @Override
        public String contentItemType() {
            return CONTENT_ITEM_TYPE_PREFIX + TABLE_NAME.toLowerCase(Locale.US);
        }

        @Override
        public Uri contentUri() {
            return CONTENT_URI;
        }
    }

    /*
    * INDEXES
    *
    * To index a column or set of columns not marked UNIQUE or PRIMARY_KEY add them here.
     */

    public static final DbColumn[][] TABLE_INDEXES = new DbColumn[][]{
            new DbColumn[]{ActivityEntry.CONVERSATION_ID, ActivityEntry.ACTIVITY_PUBLISHED_TIME},
            new DbColumn[]{ActivityEntry.ACTIVITY_TYPE, ActivityEntry.ACTIVITY_PUBLISHED_TIME},
            new DbColumn[]{ConversationMeetingEntry.CONVERSATION_ID},
            new DbColumn[]{ConversationMeetingEntry.MEETING_ID},
            new DbColumn[]{ConversationEntry.TEAM_ID}
    };

    /*
    * VIEWS
    */
    public interface ViewColumn extends DbColumn {
        String getViewSql();

        DbColumn sourceColumn();
    }

    public enum vw_ContentCacheSize implements ViewColumn {
        TYPE(ContentDataCacheEntry.TYPE),
        DATA_SIZE(ContentDataCacheEntry.DATA_SIZE);

        public static final String[] DEFAULT_PROJECTION = getProjection(vw_ContentCacheSize.values());

        public DbColumn sourceCol;
        public static final String VIEW_NAME = vw_ContentCacheSize.class.getSimpleName();
        public static final int URI_MATCHCODE = getNextMatchId();
        public static final int URI_IDMATCHCODE = URI_MATCHCODE + ID_URI_MATCH_ID_BASE;

        public static final Uri CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY + "/" + VIEW_NAME);

        vw_ContentCacheSize(DbColumn _sourceCol) {
            sourceCol = _sourceCol;
        }

        @Override
        public String getViewSql() {
            return "CREATE VIEW " + VIEW_NAME + " AS SELECT "
                    + " TOTAL(DATA_SIZE) AS DATA_SIZE, MAX(TYPE) AS TYPE "
                    + " FROM " + ContentDataCacheEntry.TABLE_NAME
                    + " GROUP BY " + ContentDataCacheEntry.TABLE_NAME + "." + ContentDataCacheEntry.TYPE;
        }

        @Override
        public int uriMatchCode() {
            return URI_MATCHCODE;
        }

        @Override
        public int idUriMatchCode() {
            return URI_IDMATCHCODE;
        }

        @Override
        public String tablename() {
            return VIEW_NAME;
        }

        @Override
        public DbColumn sourceColumn() {
            return sourceCol;
        }

        @Override
        public String datatype() {
            return sourceCol.datatype();
        }

        @Override
        public int flags() {
            return sourceCol.flags();
        }

        @Override
        public String contentType() {
            return CONTENT_TYPE_PREFIX + VIEW_NAME.toLowerCase(Locale.US);
        }

        @Override
        public String contentItemType() {
            return CONTENT_ITEM_TYPE_PREFIX + VIEW_NAME.toLowerCase(Locale.US);
        }

        @Override
        public Uri contentUri() {
            return CONTENT_URI;
        }
    }

    public enum vw_Conversation implements ViewColumn {
        _id(ConversationEntry._id),
        CONVERSATION_ID(ConversationEntry.CONVERSATION_ID),
        URL(ConversationEntry.URL),
        TITLE(ConversationEntry.TITLE),
        MUTED(ConversationEntry.MUTED),
        MESSAGE_NOTIFICATIONS(ConversationEntry.MESSAGE_NOTIFICATIONS),
        MENTION_NOTIFICATIONS(ConversationEntry.MENTION_NOTIFICATIONS),
        HIDDEN(ConversationEntry.HIDDEN),
        ARCHIVED(ConversationEntry.ARCHIVED),
        PMR(ConversationEntry.PMR),
        LOCKED(ConversationEntry.LOCKED),
        ONE_ON_ONE_PARTICIPANT(ConversationEntry.ONE_ON_ONE_PARTICIPANT),
        SPACE_URL(ConversationEntry.SPACE_URL),
        SPACE_URL_HIDDEN(ConversationEntry.SPACE_URL_HIDDEN),
        LOCUS_URL(ConversationEntry.LOCUS_URL),
        DEFAULT_ENCRYPTION_KEY_URL(ConversationEntry.DEFAULT_ENCRYPTION_KEY_URL),
        TITLE_ENCRYPTION_KEY_URL(ConversationEntry.TITLE_ENCRYPTION_KEY_URL),
        IS_TITLE_ENCRYPTED(ConversationEntry.IS_TITLE_ENCRYPTED),
        SYNC_OPERATION_ID(ConversationEntry.SYNC_OPERATION_ID),
        FAVORITE(ConversationEntry.FAVORITE),
        LAST_ACTIVITY_ID(ConversationEntry.LAST_ACTIVITY_ID),
        SORTING_TIMESTAMP(ConversationEntry.SORTING_TIMESTAMP),
        PARTICIPANT_COUNT(ConversationEntry.PARTICIPANT_COUNT),
        EXTERNAL_PARTICIPANT_COUNT(ConversationEntry.EXTERNAL_PARTICIPANT_COUNT),
        TIMESTAMP_LAST_SEEN_ACTIVITY_REMOTE(ConversationEntry.TIMESTAMP_LAST_SEEN_ACTIVITY_REMOTE),
        TIMESTAMP_LAST_READABLE_ACTIVITY_REMOTE(ConversationEntry.TIMESTAMP_LAST_READABLE_ACTIVITY_REMOTE),
        TOP_PARTICIPANTS(ConversationEntry.TOP_PARTICIPANTS),
        CREATOR_UUID(ConversationEntry.CREATOR_UUID),
        LAST_RELEVANT_ACTIVITY_TIMESTAMP(ConversationEntry.LAST_RELEVANT_ACTIVITY_TIMESTAMP),
        CONVERSATION_DISPLAY_NAME(ConversationEntry.CONVERSATION_DISPLAY_NAME),
        KMS_RESOURCE_OBJECT_URI(ConversationEntry.KMS_RESOURCE_OBJECT_URI),
        TEAM_ID(ConversationEntry.TEAM_ID),
        SUMMARY(ConversationEntry.SUMMARY),
        SELF_JOINED(ConversationEntry.SELF_JOINED),
        CONVERSATION_AVATAR_CONTENT_REFERENCE(ConversationEntry.CONVERSATION_AVATAR_CONTENT_REFERENCE),
        CONVERSATION_AVATAR_ENCRYPTION_KEY_URL(ConversationEntry.AVATAR_ENCRYPTION_KEY_URL),
        IS_CONVERSATION_AVATAR_ENCRYPTED(ConversationEntry.IS_AVATAR_ENCRYPTED),
        SHARE_COUNT(ConversationEntry.SHARE_COUNT),
        IS_PARTICIPANT_LIST_VALID(ConversationEntry.IS_PARTICIPANT_LIST_VALID),
        ACL_URL(ConversationEntry.ACL_URL),

        // Values for the "last activity" in the room list
        ACTIVITY_DATA(ActivityEntry.ACTIVITY_DATA),
        ACTIVITY_PUBLISHED_TIME(ActivityEntry.ACTIVITY_PUBLISHED_TIME),
        ACTIVITY_TYPE(ActivityEntry.ACTIVITY_TYPE),
        ACTIVITY_ENCRYPTION_KEY_URL(ActivityEntry.ENCRYPTION_KEY_URL),
        ACTIVITY_IS_ENCRYPTED(ActivityEntry.IS_ENCRYPTED),
        IN_ACTIVE_CALL(ConversationEntry.IN_ACTIVE_CALL),

        // Values for team related properties
        TEAM_COLOR(TeamEntry.COLOR),
        PRIMARY_TEAM_CONVERSATION_ID(TeamEntry.PRIMARY_CONVERSATION_ID),
        TEAM_DISPLAY_NAME(null),
        IS_TEAM_DISPLAY_NAME_ENCRYPTED(null),

        //Values for room ownership and retention
        CUSTODIAN_ORG_ID(ConversationEntry.CUSTODIAN_ORG_ID),
        CUSTODIAN_ORG_NAME(ConversationEntry.CUSTODIAN_ORG_NAME),
        RETENTION_URL(ConversationEntry.RETENTION_URL),
        BINDING_STATE(ConversationEntry.BINDING_STATE),
        RETENTION_DAYS(ConversationEntry.RETENTION_DAYS),
        IS_TEAM_GUEST(ConversationEntry.IS_TEAM_GUEST),
        LAST_RETENTION_SYNC_TIMESTAMP(ConversationEntry.LAST_RETENTION_SYNC_TIMESTAMP);


        public static final String[] DEFAULT_PROJECTION = getProjection(vw_Conversation.values());

        public DbColumn sourceCol;
        public static final String VIEW_NAME = vw_Conversation.class.getSimpleName();
        public static final int URI_MATCHCODE = getNextMatchId();
        public static final int URI_IDMATCHCODE = URI_MATCHCODE + ID_URI_MATCH_ID_BASE;

        public static final Uri CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY + "/" + VIEW_NAME);

        vw_Conversation(DbColumn _sourceCol) {
            sourceCol = _sourceCol;
        }

        @Override
        public String getViewSql() {
            return "CREATE VIEW " + VIEW_NAME + " AS SELECT " + getTableColumns(values())
                    + ", primaryTeamConv.TITLE AS TEAM_DISPLAY_NAME"
                    + ", primaryTeamConv.IS_TITLE_ENCRYPTED AS IS_TEAM_DISPLAY_NAME_ENCRYPTED"
                    + " FROM " + ConversationEntry.TABLE_NAME
                    + " LEFT JOIN " + ActivityEntry.TABLE_NAME
                    + " ON " + fqname(ConversationEntry.LAST_ACTIVITY_ID) + " = " + fqname(ActivityEntry.ACTIVITY_ID)
                    + " LEFT JOIN " + TeamEntry.TABLE_NAME
                    + " ON " + fqname(ConversationEntry.TEAM_ID) + " = " + fqname(TeamEntry.TEAM_ID)
                    + " LEFT JOIN " + TeamEntry.TABLE_NAME + " team ON " + fqname(ConversationEntry.TEAM_ID) + " = team." + TeamEntry.TEAM_ID
                    + " LEFT JOIN " + ConversationEntry.TABLE_NAME + " primaryTeamConv ON team." + TeamEntry.PRIMARY_CONVERSATION_ID + " = primaryTeamConv." + ConversationEntry.CONVERSATION_ID;
        }

        @Override
        public int uriMatchCode() {
            return URI_MATCHCODE;
        }

        @Override
        public int idUriMatchCode() {
            return URI_IDMATCHCODE;
        }

        @Override
        public String tablename() {
            return VIEW_NAME;
        }

        @Override
        public DbColumn sourceColumn() {
            return sourceCol;
        }

        @Override
        public String datatype() {
            switch (this) {
                case TEAM_DISPLAY_NAME:
                    return TEXT;
                case IS_TEAM_DISPLAY_NAME_ENCRYPTED:
                    return INTEGER;
                default:
                    return sourceCol.datatype();
            }
        }

        @Override
        public int flags() {
            switch (this) {
                case TEAM_DISPLAY_NAME:
                    return 0;
                case IS_TEAM_DISPLAY_NAME_ENCRYPTED:
                    return NOT_NULL | DEFAULT_0;
                default:
                    return sourceCol.flags();
            }
        }

        @Override
        public String contentType() {
            return CONTENT_TYPE_PREFIX + VIEW_NAME.toLowerCase(Locale.US);
        }

        @Override
        public String contentItemType() {
            return CONTENT_ITEM_TYPE_PREFIX + VIEW_NAME.toLowerCase(Locale.US);
        }

        @Override
        public Uri contentUri() {
            return CONTENT_URI;
        }
    }

    public enum vw_Participant implements ViewColumn {
        _id(ActorEntry._id),
        CONVERSATION_ID(ParticipantEntry.CONVERSATION_ID),
        ACTOR_UUID(ActorEntry.ACTOR_UUID),
        DISPLAY_NAME(ActorEntry.DISPLAY_NAME),
        TYPE(ActorEntry.TYPE),
        LASTACK_ACTIVITY_ID(ParticipantEntry.LASTACK_ACTIVITY_ID),
        LASTACK_ACTIVITY_TIME(ParticipantEntry.LASTACK_ACTIVITY_TIME),
        LAST_ACTIVE_TIME(ParticipantEntry.LAST_ACTIVE_TIME),
        IS_MODERATOR(ParticipantEntry.IS_MODERATOR),
        EMAIL(ActorEntry.EMAIL),
        ORG_ID(ActorEntry.ORG_ID),
        ENTITLEMENT_SQUARED(ActorEntry.ENTITLEMENT_SQUARED),
        CI_NOTFOUND(ActorEntry.CI_NOTFOUND),
        PRESENCE_STATUS(ActorEntry.PRESENCE_STATUS),
        PRESENCE_LAST_ACTIVE(ActorEntry.PRESENCE_LAST_ACTIVE),
        PRESENCE_EXPIRATION_DATE(ActorEntry.PRESENCE_EXPIRATION_DATE);

        public static final String[] DEFAULT_PROJECTION = getProjection(vw_Participant.values());

        public DbColumn sourceCol;
        public static final String VIEW_NAME = vw_Participant.class.getSimpleName();
        public static final int URI_MATCHCODE = getNextMatchId();
        public static final int URI_IDMATCHCODE = URI_MATCHCODE + ID_URI_MATCH_ID_BASE;

        public static final Uri CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY + "/" + VIEW_NAME);

        vw_Participant(DbColumn _sourceCol) {
            sourceCol = _sourceCol;
        }

        @Override
        public String getViewSql() {
            return "CREATE VIEW " + VIEW_NAME + " AS SELECT " + getTableColumns(values())
                    + " FROM " + ActorEntry.TABLE_NAME
                    + " JOIN " + ParticipantEntry.TABLE_NAME
                    + " ON " + fqname(ActorEntry.ACTOR_UUID) + " = " + fqname(ParticipantEntry.ACTOR_UUID)
                    + " WHERE " + ParticipantEntry.MEMBERSHIP_STATE + " = " + ParticipantEntry.MembershipState.ACTIVE.ordinal();
        }

        @Override
        public int uriMatchCode() {
            return URI_MATCHCODE;
        }

        @Override
        public int idUriMatchCode() {
            return URI_IDMATCHCODE;
        }

        @Override
        public String tablename() {
            return VIEW_NAME;
        }

        @Override
        public DbColumn sourceColumn() {
            return sourceCol;
        }

        @Override
        public String datatype() {
            return sourceCol.datatype();
        }

        @Override
        public int flags() {
            return sourceCol.flags();
        }

        @Override
        public String contentType() {
            return CONTENT_TYPE_PREFIX + VIEW_NAME.toLowerCase(Locale.US);
        }

        @Override
        public String contentItemType() {
            return CONTENT_ITEM_TYPE_PREFIX + VIEW_NAME.toLowerCase(Locale.US);
        }

        @Override
        public Uri contentUri() {
            return CONTENT_URI;
        }
    }

    public enum vw_PeopleInSmallRooms implements ViewColumn {
        _id(ActorEntry._id),
        EMAIL(ActorEntry.EMAIL),
        DISPLAY_NAME(ActorEntry.DISPLAY_NAME),
        UUID(ActorEntry.ACTOR_UUID),
        TYPE(ActorEntry.TYPE),
        PRESENCE_STATUS(ActorEntry.PRESENCE_STATUS),
        PRESENCE_LAST_ACTIVE(ActorEntry.PRESENCE_LAST_ACTIVE),
        PRESENCE_EXPIRATION_DATE(ActorEntry.PRESENCE_EXPIRATION_DATE);

        public static final String[] DEFAULT_PROJECTION = getProjection(vw_PeopleInSmallRooms.values());

        public DbColumn sourceCol;
        public static final String VIEW_NAME = vw_PeopleInSmallRooms.class.getSimpleName();
        public static final int URI_MATCHCODE = getNextMatchId();
        public static final int URI_IDMATCHCODE = URI_MATCHCODE + ID_URI_MATCH_ID_BASE;

        public static final Uri CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY + "/" + VIEW_NAME);

        vw_PeopleInSmallRooms(DbColumn _sourceCol) {
            sourceCol = _sourceCol;
        }

        @Override
        public String getViewSql() {
            return "CREATE VIEW " + VIEW_NAME + " AS SELECT " + getTableColumns(values())
                    + " FROM " + ActorEntry.TABLE_NAME + "," + ConversationEntry.TABLE_NAME + "," + ParticipantEntry.TABLE_NAME
                    + " WHERE " + ConversationEntry.PARTICIPANT_COUNT + " <= 5"
                    + " AND " + ParticipantEntry.MEMBERSHIP_STATE + " = " + ParticipantEntry.MembershipState.ACTIVE.ordinal()
                    + " AND " + fqname(ParticipantEntry.CONVERSATION_ID) + " = " + fqname(ConversationEntry.CONVERSATION_ID)
                    + " AND " + fqname(ParticipantEntry.ACTOR_UUID) + " = " + fqname(ActorEntry.ACTOR_UUID)
                    + " AND " + ActorEntry.DISPLAY_NAME + " IS NOT NULL"
                    + " GROUP BY " + ActorEntry.TABLE_NAME + "." + ActorEntry.ACTOR_UUID;
        }

        @Override
        public int uriMatchCode() {
            return URI_MATCHCODE;
        }

        @Override
        public int idUriMatchCode() {
            return URI_IDMATCHCODE;
        }

        @Override
        public String tablename() {
            return VIEW_NAME;
        }

        @Override
        public DbColumn sourceColumn() {
            return sourceCol;
        }

        @Override
        public String datatype() {
            return sourceCol.datatype();
        }

        @Override
        public int flags() {
            return sourceCol.flags();
        }

        @Override
        public String contentType() {
            return CONTENT_TYPE_PREFIX + VIEW_NAME.toLowerCase(Locale.US);
        }

        @Override
        public String contentItemType() {
            return CONTENT_ITEM_TYPE_PREFIX + VIEW_NAME.toLowerCase(Locale.US);
        }

        @Override
        public Uri contentUri() {
            return CONTENT_URI;
        }
    }

    /**
     * vw_PendingSyncOperations
     */
    public enum vw_PendingSyncOperations implements ViewColumn {
        _id(ActivityEntry._id),
        SYNC_OPERATION_ID(SyncOperationEntry.SYNC_OPERATION_ID),
        OPERATION_TYPE(SyncOperationEntry.OPERATION_TYPE),
        SYNC_STATE(SyncOperationEntry.SYNC_STATE),
        START_TIME(SyncOperationEntry.START_TIME),
        DATA(SyncOperationEntry.DATA);

        public static final String[] DEFAULT_PROJECTION = getProjection(vw_PendingSyncOperations.values());

        public DbColumn sourceCol;
        public static final String VIEW_NAME = vw_PendingSyncOperations.class.getSimpleName();
        public static final int URI_MATCHCODE = getNextMatchId();
        public static final int URI_IDMATCHCODE = URI_MATCHCODE + ID_URI_MATCH_ID_BASE;

        public static final Uri CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY + "/" + VIEW_NAME);

        vw_PendingSyncOperations(DbColumn _sourceCol) {
            if (!name().equals(_sourceCol.name()))  // an annoying limitation
                throw new IllegalArgumentException("ASSERT : View column " + name()
                        + " does not match corresponding column " + _sourceCol.name() + " in table");
            sourceCol = _sourceCol;
        }

        @Override
        public String getViewSql() {
            return "CREATE VIEW " + VIEW_NAME + " AS SELECT " + getTableColumns(values())
                    + " FROM " + SyncOperationEntry.TABLE_NAME + " LEFT JOIN " + ActivityEntry.TABLE_NAME
                    + " ON " + SyncOperationEntry.TABLE_NAME + "._ID = " + ActivityEntry.TABLE_NAME + "._ID"
                    + " WHERE " + SyncOperationEntry.OPERATION_TYPE + " IS NOT NULL";
        }

        @Override
        public int uriMatchCode() {
            return URI_MATCHCODE;
        }

        @Override
        public int idUriMatchCode() {
            return URI_IDMATCHCODE;
        }

        @Override
        public String tablename() {
            return VIEW_NAME;
        }

        @Override
        public DbColumn sourceColumn() {
            return sourceCol;
        }

        @Override
        public String datatype() {
            return sourceCol.datatype();
        }

        @Override
        public int flags() {
            return sourceCol.flags();
        }

        @Override
        public String contentType() {
            return CONTENT_TYPE_PREFIX + VIEW_NAME.toLowerCase(Locale.US);
        }

        @Override
        public String contentItemType() {
            return CONTENT_ITEM_TYPE_PREFIX + VIEW_NAME.toLowerCase(Locale.US);
        }

        @Override
        public Uri contentUri() {
            return CONTENT_URI;
        }
    }

    public enum vw_ConversationMeetingEntry implements ViewColumn {
        _id(ConversationEntry._id),
        CONVERSATION_ID(ConversationMeetingEntry.CONVERSATION_ID),
        MEETING_ID(ConversationMeetingEntry.MEETING_ID),
        TITLE(ConversationEntry.TITLE);

        public static final String[] DEFAULT_PROJECTION = getProjection(vw_ConversationMeetingEntry.values());

        public DbColumn sourceCol;
        public static final String VIEW_NAME = vw_ConversationMeetingEntry.class.getSimpleName();
        public static final int URI_MATCHCODE = getNextMatchId();
        public static final int URI_IDMATCHCODE = URI_MATCHCODE + ID_URI_MATCH_ID_BASE;

        public static final Uri CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY + "/" + VIEW_NAME);

        vw_ConversationMeetingEntry(DbColumn _sourceCol) {
            sourceCol = _sourceCol;
        }

        @Override
        public String getViewSql() {
            return "CREATE VIEW " + VIEW_NAME + " AS SELECT " + getTableColumns(values())
                    + " FROM " + ConversationMeetingEntry.TABLE_NAME
                    + " LEFT JOIN " + ConversationEntry.TABLE_NAME
                    + " ON " + fqname(ConversationEntry.CONVERSATION_ID) + " = " + fqname(ConversationMeetingEntry.CONVERSATION_ID)
                    + " OR " + fqname(ConversationEntry.SYNC_OPERATION_ID) + " = " + fqname(ConversationMeetingEntry.CONVERSATION_ID);
        }

        @Override
        public int uriMatchCode() {
            return URI_MATCHCODE;
        }

        @Override
        public int idUriMatchCode() {
            return URI_IDMATCHCODE;
        }

        @Override
        public String tablename() {
            return VIEW_NAME;
        }

        @Override
        public DbColumn sourceColumn() {
            return sourceCol;
        }

        @Override
        public String datatype() {
            return sourceCol.datatype();
        }

        @Override
        public int flags() {
            return sourceCol.flags();
        }

        @Override
        public String contentType() {
            return CONTENT_TYPE_PREFIX + VIEW_NAME.toLowerCase(Locale.US);
        }

        @Override
        public String contentItemType() {
            return CONTENT_ITEM_TYPE_PREFIX + VIEW_NAME.toLowerCase(Locale.US);
        }

        @Override
        public Uri contentUri() {
            return CONTENT_URI;
        }
    }


    public enum vw_CallHistory implements ViewColumn {
        _id(CallHistory._id),
        CALLHISTORY_URL(CallHistory.URL),
        START_TIME(CallHistory.START_TIME),
        END_TIME(CallHistory.END_TIME),
        DURATION_SECS(CallHistory.DURATION_SECS),
        JOINED_DURATION_SECS(CallHistory.JOINED_DURATION_SECS),
        DIRECTION(CallHistory.DIRECTION),
        DISPOSTION(CallHistory.DISPOSTION),
        PARTICIPANT_COUNT(CallHistory.PARTICIPANT_COUNT),
        CALLBACK_ADDRESS(CallHistory.CALLBACK_ADDRESS),
        OTHER_UUID(CallHistory.OTHER_UUID),
        OTHER_EMAIL(CallHistory.OTHER_EMAIL),
        OTHER_NAME(CallHistory.OTHER_NAME),
        OTHER_IS_EXTERNAL(CallHistory.OTHER_IS_EXTERNAL),
        OTHER_PRIMARY_DISPLAY_STRING(CallHistory.OTHER_PRIMARY_DISPLAY_STRING),
        OTHER_SECONDARY_DISPLAY_STRING(CallHistory.OTHER_SECONDARY_DISPLAY_STRING),
        OTHER_PHONE_NUMBER(CallHistory.OTHER_PHONE_NUMBER),
        OTHER_SIP_URL(CallHistory.OTHER_SIP_URL),
        OTHER_TEL_URL(CallHistory.OTHER_TEL_URL),
        OTHER_OWNER_ID(CallHistory.OTHER_OWNER_ID),
        CONVERSATION_URL(CallHistory.CONVERSATION_URL),
        CONVERSATION_DISPLAY_NAME(ConversationEntry.CONVERSATION_DISPLAY_NAME),
        TITLE(ConversationEntry.TITLE),
        LOCUS_URL(ConversationEntry.LOCUS_URL),
        TOP_PARTICIPANTS(ConversationEntry.TOP_PARTICIPANTS),
        ONE_ON_ONE_PARTICIPANT(ConversationEntry.ONE_ON_ONE_PARTICIPANT),
        PMR(ConversationEntry.PMR),
        TEAM_ID(ConversationEntry.TEAM_ID),
        TEAM_COLOR(TeamEntry.COLOR);

        public static final String[] DEFAULT_PROJECTION = getProjection(vw_CallHistory.values());

        public DbColumn sourceCol;
        public static final String VIEW_NAME = vw_CallHistory.class.getSimpleName();
        public static final int URI_MATCHCODE = getNextMatchId();
        public static final int URI_IDMATCHCODE = URI_MATCHCODE + ID_URI_MATCH_ID_BASE;

        public static final Uri CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY + "/" + VIEW_NAME);

        vw_CallHistory(DbColumn _sourceCol) {
            sourceCol = _sourceCol;
        }

        @Override
        public String getViewSql() {
            return "CREATE VIEW " + VIEW_NAME + " AS SELECT " + getTableColumns(values())
                    + " FROM " + CallHistory.TABLE_NAME
                    + " LEFT JOIN " + ConversationEntry.TABLE_NAME
                    + " ON " + fqname(ConversationEntry.URL) + " = " + CallHistory.CONVERSATION_URL
                    + " LEFT JOIN " + TeamEntry.TABLE_NAME
                    + " ON " + fqname(TeamEntry.TEAM_ID) + " = " + fqname(ConversationEntry.TEAM_ID);

        }

        @Override
        public int uriMatchCode() {
            return URI_MATCHCODE;
        }

        @Override
        public int idUriMatchCode() {
            return URI_IDMATCHCODE;
        }

        @Override
        public String tablename() {
            return VIEW_NAME;
        }

        @Override
        public DbColumn sourceColumn() {
            return sourceCol;
        }

        @Override
        public String datatype() {
            return sourceCol.datatype();
        }

        @Override
        public int flags() {
            return sourceCol.flags();
        }

        @Override
        public String contentType() {
            return CONTENT_TYPE_PREFIX + VIEW_NAME.toLowerCase(Locale.US);
        }

        @Override
        public String contentItemType() {
            return CONTENT_ITEM_TYPE_PREFIX + VIEW_NAME.toLowerCase(Locale.US);
        }

        @Override
        public Uri contentUri() {
            return CONTENT_URI;
        }
    }

    public enum vw_Activities implements ViewColumn {
        _id(ActivityEntry._id),
        ACTIVITY_ID(ActivityEntry.ACTIVITY_ID),
        ACTOR_ID(ActivityEntry.ACTOR_ID),
        CONVERSATION_ID(ActivityEntry.CONVERSATION_ID),
        ACTIVITY_PUBLISHED_TIME(ActivityEntry.ACTIVITY_PUBLISHED_TIME),
        ACTIVITY_DATA(ActivityEntry.ACTIVITY_DATA),
        ACTIVITY_TYPE(ActivityEntry.ACTIVITY_TYPE),
        SOURCE(ActivityEntry.SOURCE),
        CONTENT_DATA_ID(ActivityEntry.CONTENT_DATA_ID),
        ENCRYPTION_KEY_URL(ActivityEntry.ENCRYPTION_KEY_URL),
        IS_ENCRYPTED(ActivityEntry.IS_ENCRYPTED),
        SYNC_OPERATION_ID(ActivityEntry.SYNC_OPERATION_ID),
        IS_MENTION(ActivityEntry.IS_MENTION),
        SYNC_STATE(ActivityEntry.SYNC_STATE),
        FLAG_STATE(FlagEntry.FLAG_STATE),
        PRESENCE_STATUS(ActorEntry.PRESENCE_STATUS),
        PRESENCE_LAST_ACTIVE(ActorEntry.PRESENCE_LAST_ACTIVE),
        PRESENCE_EXPIRATION_DATE(ActorEntry.PRESENCE_EXPIRATION_DATE),
        ORG_ID(OrganizationEntry.ORG_ID),
        RETENTION_DAYS(OrganizationEntry.RETENTION_DAYS),
        ONE_ON_ONE_PARTICIPANT(ConversationEntry.ONE_ON_ONE_PARTICIPANT),
        SPARK_WIDGET_MEETING_ID(ActivityEntry.SPARK_WIDGET_MEETING_ID),;

        public static final String[] DEFAULT_PROJECTION = getProjection(vw_Activities.values());

        public DbColumn sourceCol;
        public static final String VIEW_NAME = vw_Activities.class.getSimpleName();
        public static final int URI_MATCHCODE = getNextMatchId();
        public static final int URI_IDMATCHCODE = URI_MATCHCODE + ID_URI_MATCH_ID_BASE;

        public static final Uri CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY + "/" + VIEW_NAME);

        vw_Activities(DbColumn _sourceCol) {
            sourceCol = _sourceCol;
        }

        @Override
        public String getViewSql() {
            return "CREATE VIEW " + VIEW_NAME + " AS SELECT " + getTableColumns(values())
                    + " FROM " + ActivityEntry.TABLE_NAME
                    + " LEFT JOIN " + FlagEntry.TABLE_NAME
                    + " ON " + fqname(ActivityEntry.ACTIVITY_ID) + " = " + fqname(FlagEntry.ACTIVITY_ID)
                    + " LEFT JOIN " + ActorEntry.TABLE_NAME
                    + " ON " + fqname(ActivityEntry.ACTOR_ID) + " = " + fqname(ActorEntry.ACTOR_UUID)
                    + " LEFT JOIN " + OrganizationEntry.TABLE_NAME
                    + " ON " + fqname(ActivityEntry.ACTOR_ID) + " = " + fqname(ActorEntry.ACTOR_UUID)
                    + " AND " + fqname(ActorEntry.ORG_ID) + " = " + fqname(OrganizationEntry.ORG_ID)
                    + " LEFT JOIN " + ConversationEntry.TABLE_NAME
                    + " ON " + fqname(ActivityEntry.CONVERSATION_ID) + " = " + fqname(ConversationEntry.CONVERSATION_ID);
        }

        @Override
        public int uriMatchCode() {
            return URI_MATCHCODE;
        }

        @Override
        public int idUriMatchCode() {
            return URI_IDMATCHCODE;
        }

        @Override
        public String tablename() {
            return VIEW_NAME;
        }

        @Override
        public DbColumn sourceColumn() {
            return sourceCol;
        }

        @Override
        public String datatype() {
            return sourceCol.datatype();
        }

        @Override
        public int flags() {
            return sourceCol.flags();
        }

        @Override
        public String contentType() {
            return CONTENT_TYPE_PREFIX + VIEW_NAME.toLowerCase(Locale.US);
        }

        @Override
        public String contentItemType() {
            return CONTENT_ITEM_TYPE_PREFIX + VIEW_NAME.toLowerCase(Locale.US);
        }

        @Override
        public Uri contentUri() {
            return CONTENT_URI;
        }
    }

    public enum vw_Mentions implements ViewColumn {
        _id(ActivityEntry._id),
        ACTIVITY_ID(ActivityEntry.ACTIVITY_ID),
        ACTOR_ID(ActivityEntry.ACTOR_ID),
        ACTIVITY_PUBLISHED_TIME(ActivityEntry.ACTIVITY_PUBLISHED_TIME),
        ACTIVITY_DATA(ActivityEntry.ACTIVITY_DATA),
        ACTIVITY_TYPE(ActivityEntry.ACTIVITY_TYPE),
        CONVERSATION_DISPLAY_NAME(ConversationEntry.CONVERSATION_DISPLAY_NAME),
        CONVERSATION_ID(ConversationEntry.CONVERSATION_ID),
        PARTICIPANT_COUNT(ConversationEntry.PARTICIPANT_COUNT),
        SELF_ACK_TIMESTAMP(ConversationEntry.TIMESTAMP_LAST_SEEN_ACTIVITY_REMOTE),
        TOP_PARTICIPANTS(ConversationEntry.TOP_PARTICIPANTS),
        ONE_ON_ONE_PARTICIPANT(ConversationEntry.ONE_ON_ONE_PARTICIPANT),
        DISPLAY_NAME(ActorEntry.DISPLAY_NAME);

        public static final String[] DEFAULT_PROJECTION = getProjection(vw_Mentions.values());

        public DbColumn sourceCol;
        public static final String VIEW_NAME = vw_Mentions.class.getSimpleName();
        public static final int URI_MATCHCODE = getNextMatchId();
        public static final int URI_IDMATCHCODE = URI_MATCHCODE + ID_URI_MATCH_ID_BASE;

        public static final Uri CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY + "/" + VIEW_NAME);

        vw_Mentions(DbColumn _sourceCol) {
            sourceCol = _sourceCol;
        }

        @Override
        public String getViewSql() {
            return "CREATE VIEW " + VIEW_NAME + " AS SELECT " + getTableColumns(values())
                    + " FROM " + ActivityEntry.TABLE_NAME
                    + " JOIN " + ConversationEntry.TABLE_NAME
                    + " ON " + fqname(ActivityEntry.CONVERSATION_ID) + " = " + fqname(ConversationEntry.CONVERSATION_ID)
                    + " JOIN " + ActorEntry.TABLE_NAME
                    + " ON " + fqname(ActivityEntry.ACTOR_ID) + " = " + fqname(ActorEntry.ACTOR_UUID)
                    + " WHERE " + ActivityEntry.IS_MENTION + " = 1 AND " + ActivityEntry.IS_ENCRYPTED + " = 0 AND " + ConversationEntry.SELF_JOINED + " = 1 AND " + ConversationEntry.ARCHIVED + " = 0 ";
        }

        @Override
        public int uriMatchCode() {
            return URI_MATCHCODE;
        }

        @Override
        public int idUriMatchCode() {
            return URI_IDMATCHCODE;
        }

        @Override
        public String tablename() {
            return VIEW_NAME;
        }

        @Override
        public DbColumn sourceColumn() {
            return sourceCol;
        }

        @Override
        public String datatype() {
            return sourceCol.datatype();
        }

        @Override
        public int flags() {
            return sourceCol.flags();
        }

        @Override
        public String contentType() {
            return CONTENT_TYPE_PREFIX + VIEW_NAME.toLowerCase(Locale.US);
        }

        @Override
        public String contentItemType() {
            return CONTENT_ITEM_TYPE_PREFIX + VIEW_NAME.toLowerCase(Locale.US);
        }

        @Override
        public Uri contentUri() {
            return CONTENT_URI;
        }
    }

    public enum vw_Flags implements ViewColumn {
        _id(ActivityEntry._id),
        ACTIVITY_ID(ActivityEntry.ACTIVITY_ID),
        ACTOR_ID(ActivityEntry.ACTOR_ID),
        ACTIVITY_PUBLISHED_TIME(ActivityEntry.ACTIVITY_PUBLISHED_TIME),
        ACTIVITY_DATA(ActivityEntry.ACTIVITY_DATA),
        ACTIVITY_TYPE(ActivityEntry.ACTIVITY_TYPE),
        CONVERSATION_DISPLAY_NAME(ConversationEntry.CONVERSATION_DISPLAY_NAME),
        CONVERSATION_ID(ConversationEntry.CONVERSATION_ID),
        PARTICIPANT_COUNT(ConversationEntry.PARTICIPANT_COUNT),
        SELF_ACK_TIMESTAMP(ConversationEntry.TIMESTAMP_LAST_SEEN_ACTIVITY_REMOTE),
        ONE_ON_ONE_PARTICIPANT(ConversationEntry.ONE_ON_ONE_PARTICIPANT),
        TOP_PARTICIPANTS(ConversationEntry.TOP_PARTICIPANTS),
        SELF_JOINED(ConversationEntry.SELF_JOINED),
        FLAG_UPDATE_TIME(FlagEntry.FLAG_UPDATE_TIME),
        DISPLAY_NAME(ActorEntry.DISPLAY_NAME);

        public static final String[] DEFAULT_PROJECTION = getProjection(vw_Flags.values());

        public DbColumn sourceCol;
        public static final String VIEW_NAME = vw_Flags.class.getSimpleName();
        public static final int URI_MATCHCODE = getNextMatchId();
        public static final int URI_IDMATCHCODE = URI_MATCHCODE + ID_URI_MATCH_ID_BASE;

        public static final Uri CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY + "/" + VIEW_NAME);

        vw_Flags(DbColumn _sourceCol) {
            sourceCol = _sourceCol;
        }

        @Override
        public String getViewSql() {
            return "CREATE VIEW " + VIEW_NAME + " AS SELECT " + getTableColumns(values())
                    + " FROM " + FlagEntry.TABLE_NAME
                    + " JOIN " + ActivityEntry.TABLE_NAME
                    + " ON " + fqname(FlagEntry.ACTIVITY_ID) + " = " + fqname(ActivityEntry.ACTIVITY_ID)
                    + " JOIN " + ActorEntry.TABLE_NAME
                    + " ON " + fqname(ActivityEntry.ACTOR_ID) + " = " + fqname((ActorEntry.ACTOR_UUID))
                    + " JOIN " + ConversationEntry.TABLE_NAME
                    + " ON " + fqname(ActivityEntry.CONVERSATION_ID) + " = " + fqname(ConversationEntry.CONVERSATION_ID)
                    + " WHERE " + ActivityEntry.IS_ENCRYPTED + " = 0 "
                    + " ORDER BY " + FlagEntry.FLAG_UPDATE_TIME + " DESC";
        }

        @Override
        public int uriMatchCode() {
            return URI_MATCHCODE;
        }

        @Override
        public int idUriMatchCode() {
            return URI_IDMATCHCODE;
        }

        @Override
        public String tablename() {
            return VIEW_NAME;
        }

        @Override
        public DbColumn sourceColumn() {
            return sourceCol;
        }

        @Override
        public String datatype() {
            return sourceCol.datatype();
        }

        @Override
        public int flags() {
            return sourceCol.flags();
        }

        @Override
        public String contentType() {
            return CONTENT_TYPE_PREFIX + VIEW_NAME.toLowerCase(Locale.US);
        }

        @Override
        public String contentItemType() {
            return CONTENT_ITEM_TYPE_PREFIX + VIEW_NAME.toLowerCase(Locale.US);
        }

        @Override
        public Uri contentUri() {
            return CONTENT_URI;
        }
    }

    public enum vw_TitleEncryptionInfo implements ViewColumn {
        _id(ConversationEntry._id),
        CONVERSATION_ID(ConversationEntry.CONVERSATION_ID),
        LOCUS_URL(ConversationEntry.LOCUS_URL),
        TITLE(ConversationEntry.TITLE),
        IS_TITLE_ENCRYPTED(ConversationEntry.IS_TITLE_ENCRYPTED),
        TITLE_ENCRYPTION_KEY_URL(ConversationEntry.TITLE_ENCRYPTION_KEY_URL),
        TITLE_ENCRYPTION_KEY(EncryptionKeyEntry.ENCRYPTION_KEY),
        TITLE_ENCRYPTION_KEY_ID(EncryptionKeyEntry.ENCRYPTION_KEY_ID);

        public static final String[] DEFAULT_PROJECTION = getProjection(vw_TitleEncryptionInfo.values());

        public DbColumn sourceCol;
        public static final String VIEW_NAME = vw_TitleEncryptionInfo.class.getSimpleName();
        public static final int URI_MATCHCODE = getNextMatchId();
        public static final int URI_IDMATCHCODE = URI_MATCHCODE + ID_URI_MATCH_ID_BASE;

        public static final Uri CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY + "/" + VIEW_NAME);

        vw_TitleEncryptionInfo(DbColumn _sourceCol) {
            sourceCol = _sourceCol;
        }

        @Override
        public String getViewSql() {
            return "CREATE VIEW " + VIEW_NAME + " AS SELECT " + getTableColumns(values())
                    + " FROM " + ConversationEntry.TABLE_NAME
                    + " LEFT JOIN " + EncryptionKeyEntry.TABLE_NAME
                    + " ON " + ConversationEntry.TABLE_NAME + "." + ConversationEntry.TITLE_ENCRYPTION_KEY_URL + " = "
                    + EncryptionKeyEntry.TABLE_NAME + "." + EncryptionKeyEntry.ENCRYPTION_KEY_URI;
        }

        @Override
        public int uriMatchCode() {
            return URI_MATCHCODE;
        }

        @Override
        public int idUriMatchCode() {
            return URI_IDMATCHCODE;
        }

        @Override
        public String tablename() {
            return VIEW_NAME;
        }

        @Override
        public DbColumn sourceColumn() {
            return sourceCol;
        }

        @Override
        public String datatype() {
            return sourceCol.datatype();
        }

        @Override
        public int flags() {
            return sourceCol.flags();
        }

        @Override
        public String contentType() {
            return CONTENT_TYPE_PREFIX + VIEW_NAME.toLowerCase(Locale.US);
        }

        @Override
        public String contentItemType() {
            return CONTENT_ITEM_TYPE_PREFIX + VIEW_NAME.toLowerCase(Locale.US);
        }

        @Override
        public Uri contentUri() {
            return CONTENT_URI;
        }
    }

    public enum vw_TeamsList implements ViewColumn {
        _id(ConversationEntry._id),
        DISPLAY_NAME(ConversationEntry.CONVERSATION_DISPLAY_NAME),
        SUMMARY(ConversationEntry.SUMMARY),
        TEAM_COLOR(TeamEntry.COLOR),
        PRIMARY_CONVERSATION_ID(TeamEntry.PRIMARY_CONVERSATION_ID),
        TEAM_ID(ConversationEntry.TEAM_ID),
        BADGE_COUNT(null);

        public DbColumn sourceCol;
        public static final String VIEW_NAME = vw_TeamsList.class.getSimpleName();
        public static final int URI_MATCHCODE = getNextMatchId();
        public static final int URI_IDMATCHCODE = URI_MATCHCODE + ID_URI_MATCH_ID_BASE;
        public static final Uri CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY + "/" + VIEW_NAME);
        public static final String[] DEFAULT_PROJECTION = getProjection(vw_TeamsList.values());

        vw_TeamsList(@Nullable  DbColumn _sourceCol) {
            sourceCol = _sourceCol;
        }

        @Override
        public String datatype() {
            // The only null sourceCol is the badge count column which should be an integer as it is the result of a COUNT(*)
            return sourceCol != null ? sourceCol.datatype() : INTEGER;
        }

        @Override
        public int flags() {
            // The only null sourceCol is the badge count column which should be non null with a 0 default
            return sourceCol != null ? sourceCol.flags() : NOT_NULL | DEFAULT_0;
        }

        @Override
        public String tablename() {
            return VIEW_NAME;
        }

        @Override
        public int uriMatchCode() {
            return URI_MATCHCODE;
        }

        @Override
        public int idUriMatchCode() {
            return URI_IDMATCHCODE;
        }

        @Override
        public String contentType() {
            return CONTENT_TYPE_PREFIX + VIEW_NAME.toLowerCase(Locale.US);
        }

        @Override
        public String contentItemType() {
            return CONTENT_ITEM_TYPE_PREFIX + VIEW_NAME.toLowerCase(Locale.US);
        }

        @Override
        public Uri contentUri() {
            return CONTENT_URI;
        }

        @Override
        public String getViewSql() {
            return "CREATE VIEW vw_TeamsList AS "
                    + "SELECT "
                        + "C._id as _id, "
                        + "C.CONVERSATION_DISPLAY_NAME as DISPLAY_NAME, "
                        + "C.SUMMARY as SUMMARY, "
                        + "T.COLOR as TEAM_COLOR, "
                        + "T.PRIMARY_CONVERSATION_ID as PRIMARY_CONVERSATION_ID, "
                        + "T.TEAM_ID as TEAM_ID, "
                        + "(SELECT COUNT(*) FROM ConversationEntry WHERE ConversationEntry.TEAM_ID = C.TEAM_ID AND ConversationEntry.TIMESTAMP_LAST_READABLE_ACTIVITY_REMOTE > ConversationEntry.TIMESTAMP_LAST_SEEN_ACTIVITY_REMOTE) as BADGE_COUNT "
                    + "FROM ConversationEntry C, TeamEntry T "
                    + "WHERE C.CONVERSATION_ID = T.PRIMARY_CONVERSATION_ID AND C.HIDDEN = 0 AND C.ARCHIVED = 0";
        }


        @Override
        public @Nullable DbColumn sourceColumn() {
            return sourceCol;
        }
    }

    public enum vw_ScoredActorSearch implements ViewColumn {
        _id(ActorEntry._id),
        ACTOR_UUID(ActorEntry.ACTOR_UUID),
        DISPLAY_NAME(ActorEntry.DISPLAY_NAME),
        EMAIL(ActorEntry.EMAIL),
        ENTITLEMENT_SQUARED(ActorEntry.ENTITLEMENT_SQUARED),
        PRESENCE_STATUS(ActorEntry.PRESENCE_STATUS),
        PRESENCE_LAST_ACTIVE(ActorEntry.PRESENCE_LAST_ACTIVE),
        PRESENCE_EXPIRATION_DATE(ActorEntry.PRESENCE_EXPIRATION_DATE),
        CI_NOTFOUND(ActorEntry.CI_NOTFOUND),
        TOTAL_CONV_COUNT(null),
        SMALL_CONV_COUNT(null),
        SCORE(null);

        public DbColumn sourceCol;
        public static final String VIEW_NAME = vw_ScoredActorSearch.class.getSimpleName();
        public static final int URI_MATCHCODE = getNextMatchId();
        public static final int URI_IDMATCHCODE = URI_MATCHCODE + ID_URI_MATCH_ID_BASE;
        public static final Uri CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY + "/" + VIEW_NAME);
        public static final String[] DEFAULT_PROJECTION = getProjection(vw_ScoredActorSearch.values());

        vw_ScoredActorSearch(@Nullable  DbColumn _sourceCol) {
            sourceCol = _sourceCol;
        }

        @Override
        public String datatype() {
            // The only null sourceCols are integers
            return sourceCol != null ? sourceCol.datatype() : INTEGER;
        }

        @Override
        public int flags() {
            // The only null sourceCols are integers
            return sourceCol != null ? sourceCol.flags() : NOT_NULL | DEFAULT_0;
        }

        @Override
        public String tablename() {
            return VIEW_NAME;
        }

        @Override
        public int uriMatchCode() {
            return URI_MATCHCODE;
        }

        @Override
        public int idUriMatchCode() {
            return URI_IDMATCHCODE;
        }

        @Override
        public String contentType() {
            return CONTENT_TYPE_PREFIX + VIEW_NAME.toLowerCase(Locale.US);
        }

        @Override
        public String contentItemType() {
            return CONTENT_ITEM_TYPE_PREFIX + VIEW_NAME.toLowerCase(Locale.US);
        }

        @Override
        public Uri contentUri() {
            return CONTENT_URI;
        }

        @Override
        public String getViewSql() {
            return "CREATE VIEW vw_ScoredActorSearch AS " +
                "SELECT ae._ID AS _ID, " +
                "sum(case when SMALL_CONVERSATION == 1 then 1 else 0 end) * 2 + count(*) as SCORE, " +
                "sum(case when SMALL_CONVERSATION == 1 then 1 else 0 end) AS SMALL_CONV_COUNT, " +
                "count(*) AS TOTAL_CONV_COUNT, " +
                "ae.actor_uuid AS ACTOR_UUID, PRESENCE_STATUS, PRESENCE_LAST_ACTIVE, PRESENCE_EXPIRATION_DATE, DISPLAY_NAME, EMAIL, ENTITLEMENT_SQUARED, CI_NOTFOUND " +

                "FROM " +
                "(select conversation_id, case when PARTICIPANT_COUNT <= 20 then 1 else 0 end SMALL_CONVERSATION from ConversationEntry) sq " +
                "join ParticipantEntry pe on sq.conversation_id = pe.conversation_id join ActorEntry ae on ae.actor_uuid = pe.actor_uuid group by pe.actor_uuid";
        }

        @Override
        public @Nullable DbColumn sourceColumn() {
            return sourceCol;
        }
    }

    public enum vw_UniqueCallOutAddresses implements ViewColumn {
        _id(null),
        TOTAL_COUNT(null),
        CALLBACK_ADDRESS(CallHistory.CALLBACK_ADDRESS),
        OTHER_UUID(CallHistory.OTHER_UUID),
        OTHER_EMAIL(CallHistory.OTHER_EMAIL),
        OTHER_NAME(CallHistory.OTHER_NAME),
        OTHER_IS_EXTERNAL(CallHistory.OTHER_IS_EXTERNAL),
        OTHER_PRIMARY_DISPLAY_STRING(CallHistory.OTHER_PRIMARY_DISPLAY_STRING),
        OTHER_SECONDARY_DISPLAY_STRING(CallHistory.OTHER_SECONDARY_DISPLAY_STRING),
        OTHER_PHONE_NUMBER(CallHistory.OTHER_PHONE_NUMBER),
        OTHER_SIP_URL(CallHistory.OTHER_SIP_URL),
        OTHER_TEL_URL(CallHistory.OTHER_TEL_URL),
        OTHER_OWNER_ID(CallHistory.OTHER_OWNER_ID),
        CONVERSATION_URL(CallHistory.CONVERSATION_URL),
        PARTICIPANT_COUNT(CallHistory.PARTICIPANT_COUNT),
        CONVERSATION_DISPLAY_NAME(ConversationEntry.CONVERSATION_DISPLAY_NAME),
        TITLE(ConversationEntry.TITLE),
        ONE_ON_ONE_PARTICIPANT(ConversationEntry.ONE_ON_ONE_PARTICIPANT),
        LOCUS_URL(ConversationEntry.LOCUS_URL),
        TOP_PARTICIPANTS(ConversationEntry.TOP_PARTICIPANTS);

        public DbColumn sourceCol;
        public static final String VIEW_NAME = vw_UniqueCallOutAddresses.class.getSimpleName();
        public static final int URI_MATCHCODE = getNextMatchId();
        public static final int URI_IDMATCHCODE = URI_MATCHCODE + ID_URI_MATCH_ID_BASE;
        public static final Uri CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY + "/" + VIEW_NAME);
        public static final String[] DEFAULT_PROJECTION = getProjection(vw_UniqueCallOutAddresses.values());

        vw_UniqueCallOutAddresses(@Nullable  DbColumn _sourceCol) {
            sourceCol = _sourceCol;
        }

        @Override
        public String datatype() {
            // The only null sourceCols are integers
            return sourceCol != null ? sourceCol.datatype() : INTEGER;
        }

        @Override
        public int flags() {
            // The only null sourceCols are integers
            return sourceCol != null ? sourceCol.flags() : NOT_NULL | DEFAULT_0;
        }

        @Override
        public String tablename() {
            return VIEW_NAME;
        }

        @Override
        public int uriMatchCode() {
            return URI_MATCHCODE;
        }

        @Override
        public int idUriMatchCode() {
            return URI_IDMATCHCODE;
        }

        @Override
        public String contentType() {
            return CONTENT_TYPE_PREFIX + VIEW_NAME.toLowerCase(Locale.US);
        }

        @Override
        public String contentItemType() {
            return CONTENT_ITEM_TYPE_PREFIX + VIEW_NAME.toLowerCase(Locale.US);
        }

        @Override
        public Uri contentUri() {
            return CONTENT_URI;
        }

        @Override
        public String getViewSql() {

            return "CREATE VIEW vw_UniqueCallOutAddresses " +
                "AS SELECT " +
                "CallHistory._id as _id, " +
                "count(CALLBACK_ADDRESS) AS TOTAL_COUNT, " +
                "CallHistory.CALLBACK_ADDRESS AS CALLBACK_ADDRESS, " +
                "CallHistory.OTHER_UUID AS OTHER_UUID, " +
                "CallHistory.OTHER_EMAIL AS OTHER_EMAIL, " +
                "CallHistory.OTHER_NAME AS OTHER_NAME, " +
                "CallHistory.OTHER_IS_EXTERNAL AS OTHER_IS_EXTERNAL, " +
                "CallHistory.OTHER_PRIMARY_DISPLAY_STRING AS OTHER_PRIMARY_DISPLAY_STRING, " +
                "CallHistory.OTHER_SECONDARY_DISPLAY_STRING AS OTHER_SECONDARY_DISPLAY_STRING, " +
                "CallHistory.OTHER_PHONE_NUMBER AS OTHER_PHONE_NUMBER, " +
                "CallHistory.OTHER_SIP_URL AS OTHER_SIP_URL, " +
                "CallHistory.OTHER_TEL_URL AS OTHER_TEL_URL, " +
                "CallHistory.OTHER_OWNER_ID AS OTHER_OWNER_ID, " +
                "CallHistory.CONVERSATION_URL AS CONVERSATION_URL, " +
                "CallHistory.PARTICIPANT_COUNT AS PARTICIPANT_COUNT, " +
                "ConversationEntry.TITLE AS TITLE, " +
                "ConversationEntry.LOCUS_URL AS LOCUS_URL, " +
                "ConversationEntry.TOP_PARTICIPANTS AS TOP_PARTICIPANTS, " +
                "ConversationEntry.CONVERSATION_DISPLAY_NAME AS CONVERSATION_DISPLAY_NAME, " +
                "ConversationEntry.ONE_ON_ONE_PARTICIPANT AS ONE_ON_ONE_PARTICIPANT " +
                "FROM CallHistory LEFT JOIN ConversationEntry ON ConversationEntry.URL = CONVERSATION_URL " +
                "GROUP BY CALLBACK_ADDRESS HAVING CALLBACK_ADDRESS LIKE '%@%' ";
        }

        @Override
        public @Nullable DbColumn sourceColumn() {
            return sourceCol;
        }
    }

    /*
    * VIRTUAL TABLE for FTS
    */
    public interface VirtualTableColumn extends DbColumn {
        String getVirtualTableSql();
    }

    public enum ConversationSearchEntry implements VirtualTableColumn {
        _id(INTEGER, PRIMARY_KEY | AUTOINCREMENT),
        suggest_text_1(TEXT, 0),
        suggest_intent_data(TEXT, UNIQUE | NOT_NULL),       //Contains the conversation id
        CONVERSATION_NAME(TEXT, 0),
        PARTICIPANT_NAMES(TEXT, 0),
        PARTICIPANT_EMAILS(TEXT, 0),
        suggest_intent_extra_data(TEXT, 0),
        LAST_ACTIVE_TIME(INTEGER, 0),
        LOCUS_URL(TEXT, 0),
        ONE_ON_ONE_PARTICIPANT_UUID(TEXT, 0);

        // Suggestions use alpha ASC ordering so name these types appropriately
        public static final String TYPE_ONE_ON_ONE = "A_ONE_ON_ONE";
        public static final String TYPE_GROUP = "B_GROUP";

        public static final String SNIP_NAME = "SNIP_NAME";
        public static final String OFFSETS = "OFFSETS";
        public static final String NAME_DELIMITER = ";";

        public static final String[] DEFAULT_PROJECTION = getProjection(values());


        public static final String TABLE_NAME = ConversationSearchEntry.class.getSimpleName();
        public static final int URI_MATCHCODE = getNextMatchId();
        public static final int URI_IDMATCHCODE = URI_MATCHCODE + ID_URI_MATCH_ID_BASE;

        public static final String[] PROJECTION_WITH_SNIPPET =
                getProjection(values(),
                        "SNIPPET(" + TABLE_NAME + ", \"<b>\", \"</b>\", \"<b>...</b>\", " + CONVERSATION_NAME.ordinal() + ") AS " + SNIP_NAME,
                        "OFFSETS(" + TABLE_NAME + ") AS " + OFFSETS);

        public static final Uri CONTENT_URI = Uri
                .parse("content://" + CONTENT_AUTHORITY + "/" + TABLE_NAME);

        public String type = "";
        int flags = 0;

        ConversationSearchEntry(String type, int flags) {
            this.type = type;
            this.flags = flags;
        }

        @Override
        public String getVirtualTableSql() {
            return "CREATE VIRTUAL TABLE " + TABLE_NAME + " USING fts4(content=" + ConversationSearchDataEntry.TABLE_NAME + ", " + getTableColumns(values()) + ");";
        }

        @Override
        public String tablename() {
            return TABLE_NAME;
        }

        @Override
        public int uriMatchCode() {
            return URI_MATCHCODE;
        }

        @Override
        public int idUriMatchCode() {
            return URI_IDMATCHCODE;
        }

        @Override
        public String datatype() {
            return type;
        }

        @Override
        public int flags() {
            return flags;
        }

        @Override
        public String contentType() {
            return CONTENT_TYPE_PREFIX + TABLE_NAME.toLowerCase(Locale.US);
        }

        @Override
        public String contentItemType() {
            return CONTENT_ITEM_TYPE_PREFIX + TABLE_NAME.toLowerCase(Locale.US);
        }

        @Override
        public Uri contentUri() {
            return CONTENT_URI;
        }
    }

    public enum ContentSearchEntry implements VirtualTableColumn {
        _id(INTEGER, PRIMARY_KEY | AUTOINCREMENT),
        suggest_text_1(TEXT, 0),
        suggest_intent_data(TEXT, 0),
        ACTIVITY_ID(TEXT, UNIQUE | NOT_NULL),
        CONVERSATION_NAME(TEXT, 0),
        CONTENT_TITLE(TEXT, 0),
        CONTENT_SHARED_BY(TEXT, 0),
        CONTENT_TYPE(TEXT, 0),
        CONTENT_PUBLISHED_TIME(INTEGER, 0),
        suggest_intent_extra_data(TEXT, 0),
        CONTENT_POSTED_BY_UUID(TEXT, 0);

        // Suggestions use alpha ASC ordering so name these types appropriately

        public static final String[] DEFAULT_PROJECTION = getProjection(values());


        public static final String TABLE_NAME = ContentSearchEntry.class.getSimpleName();
        public static final int URI_MATCHCODE = getNextMatchId();
        public static final int URI_IDMATCHCODE = URI_MATCHCODE + ID_URI_MATCH_ID_BASE;

        public static final Uri CONTENT_URI = Uri
                .parse("content://" + CONTENT_AUTHORITY + "/" + TABLE_NAME);

        public String type = "";
        int flags = 0;

        ContentSearchEntry(String type, int flags) {
            this.type = type;
            this.flags = flags;
        }

        @Override
        public String getVirtualTableSql() {
            return "CREATE VIRTUAL TABLE " + TABLE_NAME + " USING fts4(content=" + ContentSearchDataEntry.TABLE_NAME + ", " + getTableColumns(values()) + ");";
        }

        @Override
        public String tablename() {
            return TABLE_NAME;
        }

        @Override
        public int uriMatchCode() {
            return URI_MATCHCODE;
        }

        @Override
        public int idUriMatchCode() {
            return URI_IDMATCHCODE;
        }

        @Override
        public String datatype() {
            return type;
        }

        @Override
        public int flags() {
            return flags;
        }

        @Override
        public String contentType() {
            return CONTENT_TYPE_PREFIX + TABLE_NAME.toLowerCase(Locale.US);
        }

        @Override
        public String contentItemType() {
            return CONTENT_ITEM_TYPE_PREFIX + TABLE_NAME.toLowerCase(Locale.US);
        }

        @Override
        public Uri contentUri() {
            return CONTENT_URI;
        }
    }

    public enum MessageSearchEntry implements VirtualTableColumn {
        _id(INTEGER, PRIMARY_KEY | AUTOINCREMENT),
        suggest_text_1(TEXT, 0),
        suggest_intent_data(TEXT, 0),
        ACTIVITY_ID(TEXT, 0),
        CONVERSATION_NAME(TEXT, 0),
        MESSAGE_TEXT(TEXT, 0),
        MESSAGE_POSTED_BY(TEXT, 0),
        MESSAGE_PUBLISHED_TIME(INTEGER, 0),
        suggest_intent_extra_data(TEXT, 0),
        MESSAGE_POSTED_BY_UUID(TEXT, 0),
        FILES_SHARED_COUNT(INTEGER, 0),
        ONE_ON_ONE_PARTICIPANT(TEXT, 0);

        // Suggestions use alpha ASC ordering so name these types appropriately

        public static final String[] DEFAULT_PROJECTION = getProjection(values());


        public static final String TABLE_NAME = MessageSearchEntry.class.getSimpleName();
        public static final int URI_MATCHCODE = getNextMatchId();
        public static final int URI_IDMATCHCODE = URI_MATCHCODE + ID_URI_MATCH_ID_BASE;

        public static final Uri CONTENT_URI = Uri
                .parse("content://" + CONTENT_AUTHORITY + "/" + TABLE_NAME);

        public String type = "";
        int flags = 0;

        MessageSearchEntry(String type, int flags) {
            this.type = type;
            this.flags = flags;
        }

        @Override
        public String getVirtualTableSql() {
            return "CREATE VIRTUAL TABLE " + TABLE_NAME + " USING fts4(content=" + MessageSearchDataEntry.TABLE_NAME + ", " + getTableColumns(values()) + ");";
        }

        @Override
        public String tablename() {
            return TABLE_NAME;
        }

        @Override
        public int uriMatchCode() {
            return URI_MATCHCODE;
        }

        @Override
        public int idUriMatchCode() {
            return URI_IDMATCHCODE;
        }

        @Override
        public String datatype() {
            return type;
        }

        @Override
        public int flags() {
            return flags;
        }

        @Override
        public String contentType() {
            return CONTENT_TYPE_PREFIX + TABLE_NAME.toLowerCase(Locale.US);
        }

        @Override
        public String contentItemType() {
            return CONTENT_ITEM_TYPE_PREFIX + TABLE_NAME.toLowerCase(Locale.US);
        }

        @Override
        public Uri contentUri() {
            return CONTENT_URI;
        }
    }


    // /////////////////////////////////////
    //
    // Utility functions
    //
    //

    public static final DbColumn[][] allTables = {
            ActorEntry.values(),
            SyncOperationEntry.values(),
            ActivityEntry.values(),
            FlagEntry.values(),
            ConversationEntry.values(),
            ParticipantEntry.values(),
            ContentDataCacheEntry.values(),
            ConversationMeetingEntry.values(),
            CallHistory.values(),
            EncryptionKeyEntry.values(),
            ConversationSearchDataEntry.values(),
            MessageSearchDataEntry.values(),
            ContentSearchDataEntry.values(),
            TeamEntry.values(),
            LocusMeetingInfoEntry.values(),
            OrganizationEntry.values(),
            CalendarMeetingInfoEntry.values()
    };

    public static final ViewColumn[][] allViews = {
            vw_ContentCacheSize.values(),
            vw_Conversation.values(),
            vw_PendingSyncOperations.values(),
            vw_Participant.values(),
            vw_ConversationMeetingEntry.values(),
            vw_CallHistory.values(),
            vw_TitleEncryptionInfo.values(),
            vw_PeopleInSmallRooms.values(),
            vw_Mentions.values(),
            vw_Flags.values(),
            vw_TeamsList.values(),
            vw_Activities.values(),
            vw_ScoredActorSearch.values(),
            vw_UniqueCallOutAddresses.values()
    };

    public static final VirtualTableColumn[][] allVirtualTables = {
            ConversationSearchEntry.values(),
            ContentSearchEntry.values(),
            MessageSearchEntry.values()
    };

    public static final String[] triggers = new String[]{
            "CREATE TRIGGER IF NOT EXISTS tr_deleteConversationActivities "
                    + "AFTER DELETE ON " + ConversationEntry.TABLE_NAME + " "
                    + "BEGIN "
                    + "DELETE FROM " + ActivityEntry.TABLE_NAME + " "
                    + "WHERE " + ActivityEntry.CONVERSATION_ID + " = OLD." + ConversationEntry.CONVERSATION_ID + "; "
                    + "END",

            "CREATE TRIGGER IF NOT EXISTS tr_deleteConversationParticipants "
                    + "AFTER DELETE ON " + ConversationEntry.TABLE_NAME + " "
                    + "BEGIN "
                    + "DELETE FROM " + ParticipantEntry.TABLE_NAME + " "
                    + "WHERE " + ParticipantEntry.CONVERSATION_ID + " = OLD." + ConversationEntry.CONVERSATION_ID + "; "
                    + "END",

            "CREATE TRIGGER IF NOT EXISTS tr_deleteBeforeUpdateMessageFTSEntry BEFORE UPDATE ON " + MessageSearchDataEntry.TABLE_NAME + " BEGIN "
                    + "  DELETE FROM " + MessageSearchEntry.TABLE_NAME
                    + " WHERE docid=old.rowid; "
                    + "END",

            "CREATE TRIGGER tr_deleteMessageFTSEntry BEFORE DELETE ON " + MessageSearchDataEntry.TABLE_NAME + " BEGIN "
                    + "  DELETE FROM " + MessageSearchEntry.TABLE_NAME + " WHERE docid=old.rowid; "
                    + "END",

            "CREATE TRIGGER tr_updateMessageFTSEntry AFTER UPDATE ON " + MessageSearchDataEntry.TABLE_NAME + " BEGIN "
                    + "  INSERT INTO " + MessageSearchEntry.TABLE_NAME + "(docid, MESSAGE_TEXT, MESSAGE_POSTED_BY)"
                    + "  VALUES(new.rowid, new.MESSAGE_TEXT, new.MESSAGE_POSTED_BY); "
                    + "END",

            "CREATE TRIGGER tr_insertMessageFTSEntry AFTER INSERT ON " + MessageSearchDataEntry.TABLE_NAME + " BEGIN "
                    + "  INSERT INTO " + MessageSearchEntry.TABLE_NAME + "(docid, MESSAGE_TEXT, MESSAGE_POSTED_BY)"
                    + " VALUES(new.rowid, new.MESSAGE_TEXT, new.MESSAGE_POSTED_BY); "
                    + "END",

            "CREATE TRIGGER IF NOT EXISTS tr_deleteBeforeUpdateContentFTSEntry BEFORE UPDATE ON " + ContentSearchDataEntry.TABLE_NAME + " BEGIN "
                    + "  DELETE FROM " + ContentSearchEntry.TABLE_NAME
                    + " WHERE docid=old.rowid; "
                    + "END",

            "CREATE TRIGGER tr_deleteContentFTSEntry  BEFORE DELETE ON " + ContentSearchDataEntry.TABLE_NAME + " BEGIN "
                    + "  DELETE FROM " + ContentSearchEntry.TABLE_NAME + " WHERE docid=old.rowid; "
                    + "END",

            "CREATE TRIGGER tr_updateContentFTSEntry AFTER UPDATE ON " + ContentSearchDataEntry.TABLE_NAME + " BEGIN "
                    + "  INSERT INTO " + ContentSearchEntry.TABLE_NAME + "(docid, CONTENT_TITLE, CONTENT_SHARED_BY, CONTENT_TYPE)"
                    + "  VALUES(new.rowid, new.CONTENT_TITLE, new.CONTENT_SHARED_BY, new.CONTENT_TYPE); "
                    + "END",

            "CREATE TRIGGER tr_insertContentFTSEntry AFTER INSERT ON " + ContentSearchDataEntry.TABLE_NAME + " BEGIN "
                    + "  INSERT INTO " + ContentSearchEntry.TABLE_NAME + "(docid, CONTENT_TITLE, CONTENT_SHARED_BY, CONTENT_TYPE)"
                    + " VALUES(new.rowid, new.CONTENT_TITLE, new.CONTENT_SHARED_BY, new.CONTENT_TYPE); "
                    + "END",

            "CREATE TRIGGER IF NOT EXISTS tr_deleteBeforeUpdateConversationFTSEntry BEFORE UPDATE ON " + ConversationSearchDataEntry.TABLE_NAME + " BEGIN "
                    + "  DELETE FROM " + ConversationSearchEntry.TABLE_NAME
                    + " WHERE docid=old.rowid; "
                    + "END",

            "CREATE TRIGGER tr_deleteConversationFTSEntry BEFORE DELETE ON " + ConversationSearchDataEntry.TABLE_NAME + " BEGIN "
                    + "  DELETE FROM " + ConversationSearchEntry.TABLE_NAME + " WHERE docid=old.rowid; "
                    + "END",

            "CREATE TRIGGER tr_updateConversationFTSEntry AFTER UPDATE ON " + ConversationSearchDataEntry.TABLE_NAME + " BEGIN "
                    + "  INSERT INTO " + ConversationSearchEntry.TABLE_NAME + "(docid, CONVERSATION_NAME, PARTICIPANT_NAMES, PARTICIPANT_EMAILS)"
                    + "  VALUES(new.rowid, new.CONVERSATION_NAME, new.PARTICIPANT_NAMES, new.PARTICIPANT_EMAILS); "
                    + "END",

            "CREATE TRIGGER tr_insertConversationFTSEntry AFTER INSERT ON " + ConversationSearchDataEntry.TABLE_NAME + " BEGIN "
                    + "  INSERT INTO " + ConversationSearchEntry.TABLE_NAME + "(docid, CONVERSATION_NAME, PARTICIPANT_NAMES, PARTICIPANT_EMAILS)"
                    + " VALUES(new.rowid, new.CONVERSATION_NAME, new.PARTICIPANT_NAMES, new.PARTICIPANT_EMAILS); "
                    + "END",

            "CREATE TRIGGER IF NOT EXISTS tr_pruningActivityBeyondRetentionDate AFTER DELETE ON " + ActivityEntry.TABLE_NAME + " BEGIN "
                    + "DELETE FROM " + FlagEntry.TABLE_NAME + " "
                    + "WHERE " + FlagEntry.ACTIVITY_ID + " = OLD." + ActivityEntry.ACTIVITY_ID + "; "
                    + "DELETE FROM " + MessageSearchDataEntry.TABLE_NAME + " "
                    + "WHERE " + MessageSearchDataEntry.ACTIVITY_ID + " = OLD." + ActivityEntry.ACTIVITY_ID + "; "
                    + "DELETE FROM " + ContentSearchDataEntry.TABLE_NAME + " "
                    + "WHERE " + ContentSearchDataEntry.ACTIVITY_ID + " = OLD." + ActivityEntry.ACTIVITY_ID + "; "
                    + "END"
    };

    // get comma delimited table.column list from array of view columns
    static String getTableColumns(ViewColumn[] viewCols) {
        ArrayList<String> columns = new ArrayList<String>(viewCols.length);

        for (ViewColumn viewCol : viewCols) {
            if (viewCol != null && viewCol.sourceColumn() != null)
                columns.add(viewCol.sourceColumn().tablename() + "." + viewCol.sourceColumn().name() + " as " + viewCol.name());
        }

        return TextUtils.join(",", columns);
    }

    // get comma delimited list from array of table columns
    static String getTableColumns(DbColumn[] cols) {
        ArrayList<String> columns = new ArrayList<String>(cols.length);

        for (DbColumn col : cols) {
            if (col != null)
                columns.add(col.name());
        }

        return TextUtils.join(",", columns);
    }

    // create a map of id match codes to tables.
    private static SparseArray<DbColumn[]> mMatchCodeToTable = new SparseArray<DbColumn[]>();

    /**
     * Return the ID column from the table by URI match
     *
     * @param match The URI_MATCH code
     * @return Either the numeric _ID column or the GUID FOO_ID column for the table, depending on
     * whether the ID is numeric.
     */
    public static DbColumn getIDColumnForMatch(int match, String id) {
        if (TextUtils.isEmpty(id) || TextUtils.isDigitsOnly(id))
            return mMatchCodeToTable.get(match)[0];

        return mMatchCodeToTable.get(match)[1];
    }

    /**
     * Return the GUID column if the second column is of that type. Otherwise return column[0]:
     * _ID.
     * <p/>
     * By convention, column[0] is _ID and column[1] is an optional guid key for the table.
     *
     * @param match
     * @return
     */
    public static DbColumn getGUIDColumnForMatch(int match) {
        DbColumn ret = mMatchCodeToTable.get(match)[1];
        if ((ret.flags() | UNIQUE) > 0)
            return ret;

        return mMatchCodeToTable.get(match)[0];
    }

    public static ArrayList<String> getUniqueColumnsForMatch(int match) {
        ArrayList<String> ret = new ArrayList<String>();
        for (DbColumn col : mMatchCodeToTable.get(match)) {
            if ((col.flags() & UNIQUE) > 0 || (col.flags() & COMPLEX_KEY) > 0)
                ret.add(col.name());
        }
        return ret;
    }

    /**
     * If the table has a timestamp column return it
     *
     * @param match
     * @return the column or null
     */
    public static DbColumn getTimeModifiedColumn(int match) {
        if (match == ParticipantEntry.URI_IDMATCHCODE || match == ParticipantEntry.URI_MATCHCODE)
            return ParticipantEntry.TIME_MODIFIED;
        return null;
    }

    // Build a map to easily look up a list of dependent views by table. Used for content provider notifications.
    private static HashMap<String, TreeSet<Uri>> mTableUriToViewUris = new HashMap<String, TreeSet<Uri>>();

    static {
        for (DbColumn[] table : allTables) {
            mMatchCodeToTable.put(table[0].uriMatchCode(), table);
            mMatchCodeToTable.put(table[0].idUriMatchCode(), table);

            mTableUriToViewUris.put(table[0].tablename(), new TreeSet<Uri>());
        }

        for (VirtualTableColumn[] table : allVirtualTables) {
            mMatchCodeToTable.put(table[0].uriMatchCode(), table);
            mMatchCodeToTable.put(table[0].idUriMatchCode(), table);

            mTableUriToViewUris.put(table[0].tablename(), new TreeSet<Uri>());
        }

        for (ViewColumn[] view : allViews) {
            mMatchCodeToTable.put(view[0].uriMatchCode(), view);
            mMatchCodeToTable.put(view[0].idUriMatchCode(), view);

            for (ViewColumn vc : view) {
                if (vc.sourceColumn() != null)
                    mTableUriToViewUris.get(vc.sourceColumn().tablename()).add(view[0].contentUri());
            }
        }
        // vw_Flags doesn't expose any column from FlagEntry, explicitly add it
        mTableUriToViewUris.get(FlagEntry.TABLE_NAME).add(vw_Flags.CONTENT_URI);

        //Special Case for content://conversationauthority/conversationentry/*/activities[/*]
        mMatchCodeToTable.put(ConversationEntry.URI_MATCH_CONVERSATION_ACTIVITIES, vw_Activities.values());
    }

    public static Uri[] getViewsForTable(String table) {
        return mTableUriToViewUris.get(table).toArray(new Uri[]{});
    }

    public static String fqname(DbColumn col) {
        return col.tablename() + "." + col.name();
    }
}
