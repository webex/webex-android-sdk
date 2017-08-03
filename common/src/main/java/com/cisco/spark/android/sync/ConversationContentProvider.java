package com.cisco.spark.android.sync;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.cisco.spark.android.BuildConfig;
import com.cisco.spark.android.core.SquaredContentProvider;
import com.cisco.spark.android.sync.ConversationContract.ActivityEntry;
import com.cisco.spark.android.sync.ConversationContract.ActorEntry;
import com.cisco.spark.android.sync.ConversationContract.ContentSearchDataEntry;
import com.cisco.spark.android.sync.ConversationContract.ContentSearchEntry;
import com.cisco.spark.android.sync.ConversationContract.ConversationEntry;
import com.cisco.spark.android.sync.ConversationContract.ConversationSearchDataEntry;
import com.cisco.spark.android.sync.ConversationContract.ConversationSearchEntry;
import com.cisco.spark.android.sync.ConversationContract.DbColumn;
import com.cisco.spark.android.sync.ConversationContract.FlagEntry;
import com.cisco.spark.android.sync.ConversationContract.MessageSearchDataEntry;
import com.cisco.spark.android.sync.ConversationContract.MessageSearchEntry;
import com.cisco.spark.android.sync.ConversationContract.OrganizationEntry;
import com.cisco.spark.android.sync.ConversationContract.ParticipantEntry;
import com.cisco.spark.android.sync.ConversationContract.TeamEntry;
import com.cisco.spark.android.sync.ConversationContract.ViewColumn;
import com.cisco.spark.android.sync.ConversationContract.VirtualTableColumn;
import com.cisco.spark.android.util.CpuStat;
import com.cisco.spark.android.util.Strings;
import com.github.benoitdion.ln.Ln;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import static com.cisco.spark.android.sync.ConversationContract.CONTENT_AUTHORITY;
import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry;
import static com.cisco.spark.android.sync.ConversationContract.allTables;
import static com.cisco.spark.android.sync.ConversationContract.allViews;
import static com.cisco.spark.android.sync.ConversationContract.allVirtualTables;
import static com.cisco.spark.android.sync.ConversationContract.getGUIDColumnForMatch;
import static com.cisco.spark.android.sync.ConversationContract.getIDColumnForMatch;
import static com.cisco.spark.android.sync.ConversationContract.getTableNameFromMatchCode;
import static com.cisco.spark.android.sync.ConversationContract.getTimeModifiedColumn;
import static com.cisco.spark.android.sync.ConversationContract.getUniqueColumnsForMatch;
import static com.cisco.spark.android.sync.ConversationContract.getViewsForTable;
import static com.cisco.spark.android.sync.ConversationContract.isIdUri;
import static com.cisco.spark.android.sync.ConversationContract.isTable;
import static com.cisco.spark.android.sync.ConversationContract.isView;
import static com.cisco.spark.android.sync.ConversationContract.isVirtualTable;
import static com.cisco.spark.android.sync.ConversationContract.vw_Conversation;

@SuppressWarnings("unchecked")
public class ConversationContentProvider extends SquaredContentProvider {

    private static final int maxTransactionDurationBeforeLogWarn = 500;
    public static final String WITH_CONFLICT_RULE = "WITH_CONFLICT_RULE";
    private boolean isInTransaction;
    private final Map<Integer, Set<Uri>> urisToNotify = new ConcurrentHashMap<>();
    private final Object urisToNotifyLock = new Object();
    private final CpuStat cpuStat = new CpuStat();

    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    // Set the URI Matches
    static {
        // This fixes a dependency issue in the static initialization sequence
        ConversationContract conversationContract = new ConversationContract();

        for (DbColumn[] table : allTables) {
            uriMatcher.addURI(CONTENT_AUTHORITY, table[0].tablename(), table[0].uriMatchCode());
            uriMatcher.addURI(CONTENT_AUTHORITY, table[0].tablename() + "/*",
                    table[0].idUriMatchCode());
        }

        for (ViewColumn[] view : allViews) {
            uriMatcher.addURI(CONTENT_AUTHORITY, view[0].tablename(), view[0].uriMatchCode());
            uriMatcher.addURI(CONTENT_AUTHORITY, view[0].tablename() + "/*",
                    view[0].idUriMatchCode());
        }

        for (VirtualTableColumn[] table : allVirtualTables) {
            uriMatcher.addURI(CONTENT_AUTHORITY, table[0].tablename(), table[0].uriMatchCode());
            uriMatcher.addURI(CONTENT_AUTHORITY, table[0].tablename() + "/*",
                    table[0].idUriMatchCode());
        }

        uriMatcher.addURI(CONTENT_AUTHORITY, "ConversationActivity/*/activities",
                ConversationEntry.URI_MATCH_CONVERSATION_ACTIVITIES);
    }

    @Inject
    DelegateSQLiteOpenHelper delegateSqLiteOpenHelper;

    public ConversationContentProvider() {
    }

    public ConversationContentProvider(DelegateSQLiteOpenHelper delegateSqLiteOpenHelper) {
        this.delegateSqLiteOpenHelper = delegateSqLiteOpenHelper;
    }


    @Override
    public String getType(Uri uri) {
        int match = uriMatcher.match(uri);
        DbColumn idcol = getIDColumnForMatch(match, null);

        if (idcol != null && isIdUri(match)) {
            return idcol.contentItemType();
        } else if (idcol != null) {
            return idcol.contentType();
        }

        throw new IllegalArgumentException("Unknown URI " + uri);
    }

    private void checkThread() {
        if (BuildConfig.DEBUG && Looper.getMainLooper() == Looper.myLooper()) {
            try {
                throw new IllegalThreadStateException("Calling DB from main thread");
            } catch (Exception e) {
                Ln.e(e);
                throw e;
            }
        }
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Ln.v("query " + uri + " selection " + selection + " " + Arrays.toString(selectionArgs));
        checkThread();

        SQLiteDatabaseInterface db = delegateSqLiteOpenHelper.getSqliteOpenHelper().getReadableDb();
        DatabaseSQLiteQueryBuilder qb = DelegateSQLiteQueryBuilder.getSQLiteQueryBuilder();

        int uriMatch = uriMatcher.match(uri);

        DbColumn idcol = getIDColumnForMatch(uriMatch, null);

        //Special cases for the Conversation Activities uri and ConversationEntry for temp id's
        if ((uriMatch == vw_Conversation.URI_IDMATCHCODE || uriMatch == ConversationEntry.URI_IDMATCHCODE)
                && !TextUtils.isDigitsOnly(uri.getPathSegments().get(1))) {

            String conversationId = uri.getPathSegments().get(1);
            if (TextUtils.isEmpty(selection)) {
                selection = ConversationEntry.CONVERSATION_ID + "=? OR " + ConversationEntry.SYNC_OPERATION_ID + "=?";
            } else {
                selection = "(" + selection + ") AND " + ConversationEntry.CONVERSATION_ID + "=? OR " + ConversationEntry.SYNC_OPERATION_ID + "=?";
            }

            //we added conversationId twice, once for the real id and another for the temp id
            selectionArgs = appendArray(selectionArgs, conversationId);
            selectionArgs = appendArray(selectionArgs, conversationId);

        } else if (uriMatch == ConversationEntry.URI_MATCH_CONVERSATION_ACTIVITIES) {

            String conversationId = uri.getPathSegments().get(1);

            if (TextUtils.isEmpty(selection)) {
                selection = ActivityEntry.CONVERSATION_ID + "=? OR " + ActivityEntry.SYNC_OPERATION_ID + "=?";
            } else {
                selection = "(" + selection + ") AND " + ActivityEntry.CONVERSATION_ID + "=?";
            }

            selectionArgs = appendArray(selectionArgs, conversationId);

        } else if (isIdUri(uriMatch)) {

            String id = uri.getLastPathSegment();
            idcol = getIDColumnForMatch(uriMatch, id);

            if (TextUtils.isEmpty(id)) {
                Ln.e("ERROR uri looks like an ID uri but has no ID. " + uri.toString());
            } else {
                idcol = getIDColumnForMatch(uriMatch, id);
                if (TextUtils.isEmpty(selection)) {
                    selection = idcol.name() + "=?";
                } else {
                    selection += " AND (" + idcol + "=?)";
                }
                selectionArgs = appendArray(selectionArgs, id);
            }
        }


        if (idcol == null) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        qb.setTables(idcol.tablename());

        long timeStart = System.currentTimeMillis();

        Cursor c = null;
        try {
            c = qb.query(db, projection, selection, selectionArgs, sortOrder);
        } catch (IllegalArgumentException | SQLiteException e) {
            StringBuilder message = new StringBuilder("Original exception was ")
                    .append(e.getClass().getSimpleName())
                    .append("\nOriginal error   = (").append(e.getMessage()).append(")")
                    .append("\nuri              = ").append(uri.toString())
                    .append("\nprojection       = ").append(Arrays.toString(projection))
                    .append("\nselection        = ").append(selection)
                    .append("\nselectionArgs    = ").append(Arrays.toString(selectionArgs))
                    .append("\nsortOrder        = ").append(sortOrder);

            Ln.e(new IllegalArgumentException(message.toString()), " FAILED due to exception: %s on uri: %s", e.getClass().getSimpleName(), uri);
        }

        if (c == null) {
            Ln.e("FAILED query: " + uri);
            return null;
        }

        try {
            c.setNotificationUri(getContext().getContentResolver(), uri);
        } catch (Exception e) {
            Ln.e(e, "FAILED");
        }

        long duration = System.currentTimeMillis() - timeStart;
        if (duration > maxTransactionDurationBeforeLogWarn) {
            Ln.i("$DBPERF Query operation took " + duration + "ms. " + uri);
            logCpuStat();
            Ln.v("$DBPERF " + uri + " proj: " + Arrays.toString(projection) + " sel: " + selection + " args: " + Arrays.toString(selectionArgs) + " orderby " + sortOrder);
        }

        return c;
    }

    public void logCpuStat() {
        Ln.i(cpuStat.toString());
    }

    private int getConflictRule(int match) {
        if (match == ConversationSearchDataEntry.URI_MATCHCODE)
            return SQLiteDatabase.CONFLICT_REPLACE;

        if (match == ConversationSearchEntry.URI_MATCHCODE)
            return SQLiteDatabase.CONFLICT_REPLACE;

        if (match == ContentSearchDataEntry.URI_MATCHCODE)
            return SQLiteDatabase.CONFLICT_REPLACE;

        if (match == ContentSearchEntry.URI_MATCHCODE)
            return SQLiteDatabase.CONFLICT_REPLACE;

        if (match == MessageSearchDataEntry.URI_MATCHCODE)
            return SQLiteDatabase.CONFLICT_REPLACE;

        if (match == MessageSearchEntry.URI_MATCHCODE)
            return SQLiteDatabase.CONFLICT_REPLACE;

        if (match == ConversationEntry.URI_MATCHCODE)
            return SQLiteDatabase.CONFLICT_IGNORE;

        if (match == ConversationEntry.URI_IDMATCHCODE)
            return SQLiteDatabase.CONFLICT_IGNORE;

        if (match == ActivityEntry.URI_MATCHCODE)
            return SQLiteDatabase.CONFLICT_IGNORE;

        if (match == ActorEntry.URI_MATCHCODE)
            return SQLiteDatabase.CONFLICT_IGNORE;

        if (match == ParticipantEntry.URI_MATCHCODE)
            return SQLiteDatabase.CONFLICT_IGNORE;

        if (match == OrganizationEntry.URI_MATCHCODE)
            return SQLiteDatabase.CONFLICT_IGNORE;

        if (match == SyncOperationEntry.URI_MATCHCODE)
            return SQLiteDatabase.CONFLICT_IGNORE;

        if (match == ConversationContract.EncryptionKeyEntry.URI_MATCHCODE)
            return SQLiteDatabase.CONFLICT_REPLACE;

        if (match == ConversationContract.CallHistory.URI_MATCHCODE)
            return SQLiteDatabase.CONFLICT_IGNORE;

        if (match == TeamEntry.URI_MATCHCODE)
            return SQLiteDatabase.CONFLICT_IGNORE;

        if (match == FlagEntry.URI_MATCHCODE)
            return SQLiteDatabase.CONFLICT_REPLACE;

        if (match == ConversationContract.ContentDataCacheEntry.URI_MATCHCODE)
            return SQLiteDatabase.CONFLICT_REPLACE;

        if (match == ConversationContract.CalendarMeetingInfoEntry.URI_MATCHCODE)
            return SQLiteDatabase.CONFLICT_IGNORE;

        return SQLiteDatabase.CONFLICT_ABORT;
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        SQLiteDatabaseInterface db = delegateSqLiteOpenHelper.getSqliteOpenHelper().getWritableDb();
        int uriMatch = uriMatcher.match(uri);
        Uri result = null;
        long rowID;

        checkThread();

        if (isView(uriMatch)) {
            throw new UnsupportedOperationException("Cannot insert into views directly. " + uri);
        }

        // If the table contains a timestamp column for lastmodifiedtime, add it to the values
        DbColumn timeModifiedColumn = getTimeModifiedColumn(uriMatch);
        if (timeModifiedColumn != null) {
            values.put(timeModifiedColumn.name(), System.currentTimeMillis());
        }

        String logStr = "contentValues";
        // no need to allocate a big string in release builds
        if (BuildConfig.DEBUG)
            logStr = "insert " + uri + " values=" + values.toString();

        int rule = getConflictRule(uriMatch);

        if (values.containsKey(WITH_CONFLICT_RULE)) {
            rule = values.getAsInteger(WITH_CONFLICT_RULE);
            values.remove(WITH_CONFLICT_RULE);
        }

        final DbColumn guidcol = getGUIDColumnForMatch(uriMatch);

        try {
            rowID = db.insertWithOnConflict(guidcol.tablename(), null, values, rule);
        } catch (Exception e) {
            Ln.e(e, "DB insert operation failed.");
            return null;
        }

        // only notify if we actually inserted something.
        boolean notify = false;

        if (rowID > 0) {
            result = ContentUris.withAppendedId(uri, rowID);
            notify = true;
            Ln.v(logStr + " :: inserted row with id " + rowID);
        }

        // Insert failed probably due to an ignored conflict,
        // build the result uri from passed-in guid if it exists
        if (result == null) {
            String guid = values.getAsString(guidcol.name());
            if (!TextUtils.isEmpty(guid)) {
                result = Uri.withAppendedPath(guidcol.contentUri(), guid);
            }
        }

        // Insert failed due to conflict and we don't have a guid column so try and fetch the _id
        if (result == null) {
            List<String> uniqueColumns = getUniqueColumnsForMatch(uriMatch);
            ArrayList<String> keys = new ArrayList<String>();
            ArrayList<String> selectionArgs = new ArrayList<String>();
            for (Map.Entry<String, Object> entry : values.valueSet()) {
                if (entry.getValue() == null || !uniqueColumns.contains(entry.getValue()))
                    continue;
                keys.add(entry.getKey());
                selectionArgs.add(values.getAsString(entry.getKey()));
            }

            String selection = TextUtils.join(" = ? AND ", keys) + " = ?";
            Cursor cursor = null;
            try {
                cursor = query(uri, new String[]{
                        "_id"
                }, selection, selectionArgs.toArray(new String[selectionArgs.size()]), null);

                if (cursor != null && cursor.moveToFirst()) {
                    result = ContentUris.withAppendedId(uri, cursor.getLong(0));
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        if (notify) {
            // Special Case
            String conversationId = null;
            String actorId = null;
            if (isActivityEntryUri(uriMatch) || isConversationEntryUri(uriMatch) || isParticipantEntryUri(uriMatch)) {
                conversationId = isParticipantEntryUri(uriMatch) ? values.getAsString(ParticipantEntry.CONVERSATION_ID.name()) : isActivityEntryUri(uriMatch) ? values.getAsString(ActivityEntry.CONVERSATION_ID.name()) : values.getAsString(ConversationEntry.CONVERSATION_ID.name());
                if (!TextUtils.isEmpty(conversationId)) {
                    if (isParticipantEntryUri(uriMatch)) {
                        notifyChange(uri, conversationId);
                    } else {
                        notifyChange(ConversationEntry.getConversationActivitiesUri(conversationId));
                    }
                } else {
                    notifyChange(uri);
                }
            } else if (isActorEntryUri(uriMatch)) {
                notifyChange(uri, values.getAsString(ActorEntry.ACTOR_UUID.name()));
            } else if (isFlagEntryUri(uriMatch)) {
                notifyChange(Uri.parse("content://" + CONTENT_AUTHORITY + "/ConversationActivity"));
                notifyChange(uri);
            } else {
                notifyChange(uri);
            }
            String appendedString = null;
            if (conversationId != null) {
                appendedString = ConversationEntry.CONVERSATION_ID.name() + "/" + conversationId;
            } else if (actorId != null) {
                appendedString = ActorEntry.ACTOR_UUID.name() + "/" + actorId;
            }
            notifyViews(getTableNameFromMatchCode(uriMatch), appendedString);
        }

        if (result == null) {
            Ln.e("insert() Error inserting into " + guidcol.tablename());
        }

        return result;
    }

    private void notifyChange(Uri uri) {
        notifyChange(uri, null);
    }

    int i = 0;
    private void notifyChange(Uri baseuri, @Nullable String appendedPath) {
        Uri uri = TextUtils.isEmpty(appendedPath)
                ? baseuri
                : Uri.withAppendedPath(baseuri, appendedPath);

        if (isInTransaction) {
            synchronized (urisToNotifyLock) {
                // To cut down on extremely chatty notifications, allow only one per uri-match per transaction.
                int match = uriMatcher.match(uri);
                Set<Uri> notifySet = urisToNotify.get(match);

                if (notifySet == null) {
                    notifySet = new HashSet<>();
                    urisToNotify.put(match, notifySet);
                }

                if (notifySet.size() > 3) {
                    // Spam prevention: Fall back to the base uri and let the set dedup
                    uri = baseuri;
                }

                notifySet.add(uri);
            }
        } else {
            getContext().getContentResolver().notifyChange(uri, null, false);
            Ln.v("Notifying " + uri);
        }
    }

    private void notifyViews(String table) {
        notifyViews(table, null);
    }

    private void notifyViews(String table, @Nullable String appendedPath) {
        if (TextUtils.isEmpty(table))
            return;

        Uri[] views = getViewsForTable(table);

        if (views == null)
            return;

        for (Uri view : views) {
            notifyChange(view, appendedPath);
        }
    }

    @Override
    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations)
            throws OperationApplicationException {
        SQLiteDatabaseInterface db = delegateSqLiteOpenHelper.getSqliteOpenHelper().getWritableDb();
        ArrayList<ContentProviderResult> results = new ArrayList<ContentProviderResult>();
        LinkedList<Uri> urisToNotifyCopy = new LinkedList<Uri>();

        long start = System.currentTimeMillis();
        synchronized (urisToNotifyLock) {
            try {
                db.beginTransactionNonExclusive();
                try {
                    isInTransaction = true;

                    final int numOperations = operations.size();
                    final ContentProviderResult[] backrefs = new ContentProviderResult[numOperations];

                    try {
                        for (int i = 0; i < numOperations; i++) {
                            backrefs[i] = operations.get(i).apply(this, backrefs, i);
                            results.add(backrefs[i]);
                        }
                        db.setTransactionSuccessful();
                    } catch (OperationApplicationException e) {
                        Ln.e(e, "DB apply batch operation failed.");
                        urisToNotify.clear();
                    }
                } finally {
                    try {
                        db.endTransaction();
                    } finally {
                        isInTransaction = false;
                    }
                    long duration = System.currentTimeMillis() - start;
                    if (duration > maxTransactionDurationBeforeLogWarn) {
                        Ln.i("$DBPERF Transaction took " + duration + " ms (" + results.size() + ")");
                        logCpuStat();
                    }

                    for (Set<Uri> uriSet : urisToNotify.values()) {
                        urisToNotifyCopy.addAll(uriSet);
                    }
                    urisToNotify.clear();
                }
            } catch (Exception e) {
                Ln.e(e, "DB apply batch operation failed.");
            }
        }

        try {
            start = System.currentTimeMillis();
            for (Uri uri : urisToNotifyCopy) {
                notifyChange(uri);
            }
            long duration = System.currentTimeMillis() - start;
            if (duration > 1000) {
                Ln.d("$DBPERF notifying content listeners took " + duration + " ms");
                logCpuStat();
            }
            if (results.size() > 1000) {
                start = System.currentTimeMillis();
                delegateSqLiteOpenHelper.getSqliteOpenHelper().getWritableDb().execSQL("analyze main");
                Ln.d("$DBPERF Updating DB statistics took " + (System.currentTimeMillis() - start) + " ms");
            }
            return results.toArray(new ContentProviderResult[results.size()]);
        } catch (Exception e) {
            Ln.e(e, "Content notifications failed.");
        }
        return null;
    }

    /**
     * Delete.
     */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        long timeStart = System.currentTimeMillis();

        String logStr = "delete " + uri + " selection " + selection + " " + Arrays.toString(selectionArgs);

        checkThread();

        SQLiteDatabaseInterface db = delegateSqLiteOpenHelper.getSqliteOpenHelper().getWritableDb();
        int count = 0;

        int match = uriMatcher.match(uri);
        if (!isTable(match) && !isVirtualTable(match)) {
            throw new UnsupportedOperationException("Cannot delete from views directly. " + uri);
        }

        String table = getTableNameFromMatchCode(match);

        // adjust selectionArgs
        if (isIdUri(match)) {
            String id = uri.getLastPathSegment();

            DbColumn idCol = getIDColumnForMatch(match, id);

            if (TextUtils.isEmpty(selection)) {
                selection = idCol + " =?";

                selectionArgs = new String[]{
                        uri.getLastPathSegment()
                };
            } else {
                selection += " AND " + idCol + " =?";
                selectionArgs = appendArray(selectionArgs, uri.getLastPathSegment());
            }
        }

        //NOTE Triggers are used to clean up dependent activities and participants when
        // a conversation is deleted. See ConversationContract.

        //$TODO jk This is a good place to delete filesystem objects referenced by deleted messages

        ArrayList<String> conversationIDs = new ArrayList<>();
        ArrayList<String> actorIDs = new ArrayList<>();
        Cursor cursor = null;
        try {
            if (ConversationEntry.TABLE_NAME.equals(table) || ActivityEntry.TABLE_NAME.equals(table)) {
                // build a list of conversationActivityUris to notify
                DatabaseSQLiteQueryBuilder qb = DelegateSQLiteQueryBuilder.getSQLiteQueryBuilder();
                qb.setTables(table);
                cursor = qb.query(db, new String[]{"DISTINCT " + ConversationEntry.CONVERSATION_ID.name()}, selection, selectionArgs, null);
                while (cursor.moveToNext()) {
                    conversationIDs.add(cursor.getString(0));
                }
            } else if (ActorEntry.TABLE_NAME.equals(table)) {
                // build a list of actor ID's which we will use to notify
                DatabaseSQLiteQueryBuilder qb = DelegateSQLiteQueryBuilder.getSQLiteQueryBuilder();
                qb.setTables(table);
                cursor = qb.query(db, new String[]{"DISTINCT " + ActorEntry.ACTOR_UUID.name()}, selection, selectionArgs, null);
                while (cursor.moveToNext()) {
                    actorIDs.add(cursor.getString(0));
                }
            } else if (FlagEntry.TABLE_NAME.equals(table)) {
                notifyChange(Uri.parse("content://" + CONTENT_AUTHORITY + "/ConversationActivity"));
            }
            count = db.delete(table, selection, selectionArgs);
        } catch (Exception e) {
            Ln.e(e, "DB delete operation failed.");
        } finally {
            if (cursor != null)
                cursor.close();
        }

        if (count > 0) {
            Ln.d(logStr + " removed " + count + " rows");

            if (!conversationIDs.isEmpty()) {
                for (String conversationID : conversationIDs) {
                    notifyChange(ConversationEntry.getConversationActivitiesUri(conversationID));
                    notifyViews(table, ConversationEntry.CONVERSATION_ID + "/" + conversationID);
                }
            }

            if (!actorIDs.isEmpty()) {
                for (String actorID : actorIDs) {
                    notifyViews(table, ActorEntry.ACTOR_UUID + "/" + actorID);
                }
            }

            if (conversationIDs.isEmpty() && actorIDs.isEmpty()) {
                notifyViews(table);
            }
        }

        long duration = System.currentTimeMillis() - timeStart;
        if (duration > maxTransactionDurationBeforeLogWarn) {
            Ln.d("$DBPERF delete operation took " + duration + "ms. " + uri);
            logCpuStat();
        }

        return count;
    }

    /**
     * Update.
     */
    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        String originalSelection = selection;

        checkThread();

        long timeStart = System.currentTimeMillis();

        SQLiteDatabaseInterface db = delegateSqLiteOpenHelper.getSqliteOpenHelper().getWritableDb();

        int count = 0;
        if (selection == null)
            selection = "";

        int match = uriMatcher.match(uri);

        if (!isTable(match) && !isVirtualTable(match)) {
            throw new UnsupportedOperationException("Cannot update views directly. " + uri);
        }

        String logStr = "$DBPERF update " + uri + " " + values + " selection:" + selection + " " + Arrays.toString(selectionArgs);

        String id = null;

        if (isIdUri(match))
            id = uri.getLastPathSegment();

        DbColumn idcol = getIDColumnForMatch(match, id);

        ArrayList<String> args;

        if (selectionArgs != null)
            args = new ArrayList<>(Arrays.asList(selectionArgs));
        else
            args = new ArrayList<>();

        if (isIdUri(match)) {
            if (!TextUtils.isEmpty(selection))
                selection += " AND ";
            selection += idcol.name() + " =?";
            args.add(uri.getLastPathSegment());
        }


        // Do not coalesce the FTS table
        if (!isVirtualTable(match)) {
            // We build list of args here that prevents us from updating data if it
            // is identical to existing data. This is a safety against update/notify/update
            // loops and keeps things generally less chatty.
            //
            // Given the list of values we're updating, append to the where clause something like:
            //  "where newColAvalue <> ColA OR newColBvalue <> ColB OR ... "
            //
            // The above is slightly simplified. Because of SQL's null handling e.g. "anything <> NULL" always
            // evaluates to false, if newColAvalue is not null and newColBvalue is null, the above has to be:
            //  "where COALESCE(ColA, 'sentinelvalue') <> ColA OR ColB IS NOT NULL OR... "
            ArrayList<String> orList = new ArrayList<>();

            Set<Map.Entry<String, Object>> valueSet = values.valueSet();
            for (Map.Entry<String, Object> entry : valueSet) {
                String key = entry.getKey();
                String val = values.getAsString(key);
                if (val == null) {
                    orList.add(key + " IS NOT NULL");
                } else {
                    // This looks wacky but it's to get around the special null handling in SQL statements.
                    orList.add("cast(COALESCE(" + key + ",'[~s3nT1nLvAl~}') as text) <> ?");
                    args.add(val);
                }
            }

            if (TextUtils.isEmpty(selection)) {
                selection = TextUtils.join(" OR ", orList);
            } else {
                selection = "( " + selection + " ) AND ( " + TextUtils.join(" OR ", orList) + " )";
            }
        }

        // If the table contains a timestamp column for lastmodifiedtime, add it to the values
        DbColumn timeModifiedColumn = getTimeModifiedColumn(match);
        if (timeModifiedColumn != null) {
            values.put(timeModifiedColumn.name(), System.currentTimeMillis());
        }

        String[] argsArray = args.toArray(new String[args.size()]);
        try {
            count = db.updateWithOnConflict(idcol.tablename(), values, selection,
                    argsArray, getConflictRule(match));
        } catch (Exception e) {
            Ln.e(e, "DB update operation failed.");
        }

        // Only notify if something changed
        if (count > 0) {
            // Special Case
            String conversationId = null;
            String actorId = null;
            if (isActivityEntryUri(match) || isConversationEntryUri(match) || isParticipantEntryUri(match)) {
                String tableName = isParticipantEntryUri(match) ? ParticipantEntry.TABLE_NAME : isActivityEntryUri(match) ? ActivityEntry.TABLE_NAME : ConversationEntry.TABLE_NAME;
                String columnName = isParticipantEntryUri(match) ? ParticipantEntry.CONVERSATION_ID.name() : isActivityEntryUri(match) ? ActivityEntry.CONVERSATION_ID.name() : ConversationEntry.CONVERSATION_ID.name();
                conversationId = getConversationId(match, values, originalSelection, selectionArgs, tableName, columnName);

                // If we don't have a conv ID (for a provisional activity/conv) check the sync operation ID
                // (Ignore participant entry because it doesn't have a sync operation ID column)
                if (conversationId == null && !isParticipantEntryUri(match)) {
                    conversationId = getConversationId(match, values, originalSelection, selectionArgs, tableName, isConversationEntryUri(match) ? ConversationEntry.SYNC_OPERATION_ID.name() : ActivityEntry.SYNC_OPERATION_ID.name());
                }
                if (!TextUtils.isEmpty(conversationId)) {
                    if (isParticipantEntryUri(match)) {
                        notifyChange(uri, conversationId);
                    } else {
                        notifyChange(ConversationEntry.getConversationActivitiesUri(conversationId));
                    }
                } else {
                    notifyChange(uri);
                }
            } else if (isActorEntryUri(match)) {
                actorId = getActorUUIDFromActorEntry(match, values, originalSelection, selectionArgs);
                if (!TextUtils.isEmpty(actorId)) {
                    notifyChange(uri, actorId);
                } else {
                    notifyChange(uri);
                }
            } else if (isFlagEntryUri(match)) {
                notifyChange(Uri.parse("content://" + CONTENT_AUTHORITY + "/ConversationActivity"));
                notifyChange(uri);
            } else {
                notifyChange(uri);
            }
            String appendedString = null;
            if (conversationId != null) {
                appendedString = ParticipantEntry.CONVERSATION_ID.name() + "/" + conversationId;
            } else if (actorId != null) {
                appendedString = ActorEntry.ACTOR_UUID.name() + "/" + actorId;
            }
            notifyViews(getTableNameFromMatchCode(match), appendedString);
        }

        if (count > 0) {
            Ln.d(logStr + " affected " + count + " rows");
        }

        long duration = System.currentTimeMillis() - timeStart;
        if (duration > maxTransactionDurationBeforeLogWarn) {
            Ln.i("$DBPERF update operation took " + duration + "ms. " + uri + " (" + count + ")");
            logCpuStat();
        }

        return count;
    }

    private boolean isActivityEntryUri(int uriMatch) {
        return uriMatch == ActivityEntry.URI_IDMATCHCODE || uriMatch == ActivityEntry.URI_MATCHCODE;
    }

    private boolean isConversationEntryUri(int uriMatch) {
        return uriMatch == ConversationEntry.URI_MATCHCODE || uriMatch == ConversationEntry.URI_IDMATCHCODE;
    }

    private boolean isFlagEntryUri(int uriMatch) {
        return uriMatch == FlagEntry.URI_MATCHCODE || uriMatch == FlagEntry.URI_IDMATCHCODE;
    }

    private boolean isParticipantEntryUri(int uriMatch) {
        return uriMatch == ParticipantEntry.URI_MATCHCODE || uriMatch == ParticipantEntry.URI_IDMATCHCODE;
    }

    private boolean isActorEntryUri(int uriMatch) {
        return uriMatch == ActorEntry.URI_MATCHCODE || uriMatch == ActorEntry.URI_IDMATCHCODE;
    }

    private String getActorUUIDFromActorEntry(int uriMatch, ContentValues values, String selection, String[] selectionArgs) {
        String ret = values.getAsString(ActorEntry.ACTOR_UUID.name());
        if (!TextUtils.isEmpty(ret))
            return ret;

        Cursor c = null;
        try {
            SQLiteDatabaseInterface db = delegateSqLiteOpenHelper.getSqliteOpenHelper().getReadableDb();
            c = db.query(ActorEntry.TABLE_NAME, new String[]{ActorEntry.ACTOR_UUID.name()}, selection, selectionArgs, null, null, null);
            if (c != null && c.moveToFirst()) {
                return c.getString(0);
            }
        } catch (Exception e) {
            Ln.w(e, "Failed getting actor uuid column from ActorEntry using query");
        } finally {
            if (c != null)
                c.close();
        }
        if (isConversationEntryUri(uriMatch) || isParticipantEntryUri(uriMatch)) {
            return extractIdFromSqlSelection(selection, selectionArgs, ActorEntry.ACTOR_UUID.name());
        }
        return null;
    }

    private String getConversationId(int uriMatch, ContentValues values, String selection, String[] selectionArgs, String tableName, String conversationIDColumnName) {
        String ret = values.getAsString(conversationIDColumnName);
        if (!TextUtils.isEmpty(ret))
            return ret;

        Cursor c = null;
        try {
            SQLiteDatabaseInterface db = delegateSqLiteOpenHelper.getSqliteOpenHelper().getReadableDb();
            c = db.query(tableName, new String[]{conversationIDColumnName}, selection, selectionArgs, null, null, null);
            if (c != null && c.moveToFirst()) {
                return c.getString(0);
            }
        } catch (Exception e) {
            Ln.w(e, "Failed getting conversation id column " + conversationIDColumnName + " from " + tableName + " using query");
        } finally {
            if (c != null)
                c.close();
        }
        if (isConversationEntryUri(uriMatch) || isParticipantEntryUri(uriMatch)) {
            return extractIdFromSqlSelection(selection, selectionArgs, conversationIDColumnName);
        }
        return null;
    }

    private static String[] appendArray(String[] array, String toAdd) {
        if (array == null || array.length == 0) {
            return new String[]{toAdd};
        } else {
            List<String> selectionArgList = new ArrayList(Arrays.asList(array));
            selectionArgList.add(toAdd);
            return selectionArgList.toArray(new String[selectionArgList.size()]);
        }
    }

    @Nullable
    public static String extractIdFromSqlSelection(String selection, String[] selectionArgs, String idColumnName) {
        //first remove all strings inside single quotes, in case one or more contain a question mark
        String selectionWithStringsRemoved = selection.replaceAll("(\'[^\']+\')", "");

        String[] splitSelection = selectionWithStringsRemoved.split(idColumnName);
        if (splitSelection.length == 1)
            return null;
        int indexOfIdArg = Strings.countMatches(splitSelection[0], "?");
        if (indexOfIdArg < selectionArgs.length)
            return selectionArgs[indexOfIdArg];
        return null;
    }

    //used for testing the matcher
    public static int matchUri(Uri uriToMatch) {
        int ret = uriMatcher.match(uriToMatch);
        Ln.d("URIMatch: " + ret + " for " + uriToMatch);
        return ret;
    }
}
