package com.cisco.spark.android.meetings;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.LruCache;
import android.text.TextUtils;

import com.cisco.spark.android.authenticator.ApiTokenProvider;
import com.cisco.spark.android.authenticator.LogoutEvent;
import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.locus.model.CallInNumbersInfo;
import com.cisco.spark.android.locus.model.LocusMeetingInfo;
import com.cisco.spark.android.locus.model.PstnNumber;
import com.cisco.spark.android.sync.ConversationContract;
import com.cisco.spark.android.util.Action;
import com.cisco.spark.android.util.NameUtils;
import com.cisco.spark.android.util.ThrottledAsyncTask;
import com.cisco.spark.android.util.UriUtils;
import com.github.benoitdion.ln.Ln;
import com.google.gson.Gson;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Response;

/**
 * {@link LocusMeetingInfoProvider} is used to load and store {@link LocusMeetingInfo} from and to the database.
 *
 * Records are loaded asynchronously and on demand using
 * {@link LocusMeetingInfoProvider#callWithLocusMeetingInfo(String, com.cisco.spark.android.meetings.GetMeetingInfoType, com.cisco.spark.android.util.Action)}
 *
 * When a record is requested, it is first searched for in the cache. If no record is found, then it is requested from the database and eventually, Locus. If a
 * record is returned from Locus, then it is added to the database. If a record is found in the database first, it will be fetched from
 * Locus again if it was written to the database more than 1-day in the past. At the moment there is no mechanism for Locus to push an updated
 * LocusMeetingInfo record down to a client. If a "stale" LocusMeetingInfo record is found in the database but the request to Locus does not
 * return a result, then the database entry will be deleted.
 *
 * Three layers of access exist:
 *     1) getCached() - Return a cached object matching provided LocusId or null
 *     2) get() - Return a cached object or query the DB synchronously if necessary, returning the requested value or null
 *         a) Returned by the cache
 *         b) Returned by the DB
 *     3) callWithLocusMeetingInfo() - Execute the provided callback on a LocusMeetingInfo object that is either
 *         a) Returned by the cache
 *         b) Returned by the DB - THIS WILL BE ASYNC
 *         c) Returned by the /meetingInfo endpoint
 *
 *         Then return a boolean value representing whether the get was executed synchronously or asynchronously
 */
@Singleton
public class LocusMeetingInfoProvider {

    // A database entry more than 1-day prior will be updated with the current values from Locus
    private static final long MEETING_INFO_TTL = TimeUnit.DAYS.toMillis(1);
    public static final int DEFAULT_CACHE_CAPACITY = 100;

    protected Context context;
    protected ApiClientProvider apiClientProvider;
    protected ApiTokenProvider apiTokenProvider;
    protected ThrottledLoadMeetingInfoTask loadMeetingInfoTasks;

    private final LruCache<String, LocusMeetingInfo> locusMeetingInfoCache = new LruCache<>(DEFAULT_CACHE_CAPACITY);

    private final Object taskSyncLock = new Object();
    @Inject
    public LocusMeetingInfoProvider(Context context, ApiClientProvider apiClientProvider) {
        this.context = context;
        this.apiClientProvider = apiClientProvider;
        this.loadMeetingInfoTasks = new ThrottledLoadMeetingInfoTask();
    }

    /**
     * Retrieve the LocusMeetingInfo specified by the supplied ID and search type.
     *
     * @param id A Locus ID, a SIP URI, a meeting number, or Spark user UUID for the meeting
     * @param type The GetMeetingInfoType that described the ID provided
     * @param callback Action to execute with the actor.
     * @return Indicator to caller whether this method was executed synchronously (returned from cache). This
     *      could be changed to fetch synchronously from the DB if desired
     *
     * "Deepest" layer of access - Will return either a cached record, record fetched asynchronously from DB, or record fetched asynchronously from remote
     */
    public boolean callWithLocusMeetingInfo(@NonNull final String id, @NonNull final GetMeetingInfoType type, @NonNull final Action<LocusMeetingInfo> callback) {
        if (LocusMeetingInfo.isEmptyString(id)) {
            Ln.d("Cannot get LocusMeetingInfo - id is null/empty");
            return true;
        }
        if (type == GetMeetingInfoType.LOCUS_ID) {
            LocusMeetingInfo cachedLocusMeetingInfo = locusMeetingInfoCache.get(id);
            if (cachedLocusMeetingInfo != null && !isRecordStale(cachedLocusMeetingInfo)) {
                Ln.d("callWithLocusMeetingInfo() - found cached LocusMeetingInfo for id: " + id);
                callback.call(cachedLocusMeetingInfo);
                return true;
            }
        }

        addRecordRequest(id, type, callback);
        return false;
    }

    /**
     * Retrieve the LocusMeetingInfo specified by the supplied ID and search type.
     *
     * @param id A Locus ID, a SIP URI, a meeting number, or Spark user UUID for the meeting
     * @param type The GetMeetingInfoType that described the ID provided
     * @return LocusMeetingInfo or null to caller
     *
     * "Middle" layer of access - Will return either a cached record or record fetched synchronously from DB
     */
    public @Nullable LocusMeetingInfo get(@NonNull final String id, @NonNull final GetMeetingInfoType type) {
        if (LocusMeetingInfo.isEmptyString(id)) {
            Ln.d("Cannot get LocusMeetingInfo - id is null/empty");
            return null;
        }

        if (type == GetMeetingInfoType.LOCUS_ID) {
            LocusMeetingInfo cachedLocusMeetingInfo = locusMeetingInfoCache.get(id);
            if (cachedLocusMeetingInfo != null && !isRecordStale(cachedLocusMeetingInfo)) {
                Ln.d("get() - found cached LocusMeetingInfo for id: " + id);
                return cachedLocusMeetingInfo;
            }
        }

        LocusMeetingInfo locusMeetingInfo = getFromDB(id, type);
        return !isRecordStale(locusMeetingInfo) ? locusMeetingInfo : null;
    }

    /**
     * Retrieve the LocusMeetingInfo specified by the supplied Locus ID
     *
     * @param locusId A Locus ID, since this is how the cache is keyed
     * @return LocusMeetingInfo or null to caller
     *
     * "Highest" layer of access - Will return either a cached record or null, no type necessary, as all
     * cache entries are keyed by their ID
     */
    public @Nullable LocusMeetingInfo getCached(String locusId) {
        if (LocusMeetingInfo.isEmptyString(locusId)) {
            Ln.d("Cannot get cached LocusMeetingInfo - Locus id is null/empty");
            return null;
        }

        LocusMeetingInfo locusMeetingInfo = locusMeetingInfoCache.get(locusId);
        boolean stale = isRecordStale(locusMeetingInfo);
        Ln.d("getCached() returned value for id: " + locusId + " isStale: " + stale);
        return !stale ? locusMeetingInfo : null;
    }

    public void cache(@NonNull final String locusId) {
        LocusMeetingInfo cachedLocusMeetingInfo = locusMeetingInfoCache.get(locusId);
        if (cachedLocusMeetingInfo == null || isRecordStale(cachedLocusMeetingInfo)) {
            Ln.d("cache() - Fetching LocusMeetingInfo from DB or remote with null callback and inserting into cache");
            addRecordRequest(locusId, GetMeetingInfoType.LOCUS_ID, new Action<LocusMeetingInfo>() {
                @Override
                public void call(LocusMeetingInfo item) {
                    // noop
                }
            });
        }
    }

    private void addRecordRequest(String id, GetMeetingInfoType type, Action<LocusMeetingInfo> callback) {
        synchronized (taskSyncLock) {
            if (!loadMeetingInfoTasks.appendCallbackToExistingRequest(id, callback)) {
                loadMeetingInfoTasks.add(new LocusMeetingInfoRecordRequest(id, type, callback));
            }
        }
    }

    private @Nullable LocusMeetingInfo getFromDB(String id, GetMeetingInfoType type) {
        LocusMeetingInfo result = null;
        Cursor cursor = null;
        ConversationContract.LocusMeetingInfoEntry selection;
        try {
            switch (type) {
                case LOCUS_ID:
                    selection = ConversationContract.LocusMeetingInfoEntry.LOCUS_ID;
                    break;
                case SIP_URI:
                    selection = ConversationContract.LocusMeetingInfoEntry.SIP_MEETING_URI;
                    break;
                case MEETING_ID:
                    selection = ConversationContract.LocusMeetingInfoEntry.MEETING_NUMBER;
                    break;
                case PERSONAL_ROOM:
                    selection = ConversationContract.LocusMeetingInfoEntry.OWNER_ID;
                    break;
                default:
                    return null;
            }

            Ln.d(getLogString("Searching database for LocusMeetingInfo", id, type));
            cursor = context.getContentResolver().query(
                    ConversationContract.LocusMeetingInfoEntry.CONTENT_URI,
                    ConversationContract.LocusMeetingInfoEntry.DEFAULT_PROJECTION,
                    selection + "=?",
                    new String[]{id},
                    null);

            if (cursor != null && cursor.getCount() > 0) {
                try {
                    cursor.moveToFirst();

                    LocusMeetingInfo locusMeetingInfo = new LocusMeetingInfo.LocusMeetingInfoBuilder()
                        .setLocusURI(getLocusUriFromCursor(cursor))
                        .setWebExMeetingLink(cursor.getString(ConversationContract.LocusMeetingInfoEntry.WEBEX_MEETING_URL.ordinal()))
                        .setSipMeetingUri(cursor.getString(ConversationContract.LocusMeetingInfoEntry.SIP_MEETING_URI.ordinal()))
                        .setCallInNumbersInfo(callInNumbersInfoFromJson(cursor.getString(ConversationContract.LocusMeetingInfoEntry.CALL_IN_NUMBER_INFO.ordinal())))
                        .setMeetingNumber(cursor.getString(ConversationContract.LocusMeetingInfoEntry.MEETING_NUMBER.ordinal()))
                        .setOwner(cursor.getString(ConversationContract.LocusMeetingInfoEntry.OWNER_ID.ordinal()))
                        .setNumericCode(cursor.getString(ConversationContract.LocusMeetingInfoEntry.NUMERIC_CODE.ordinal()))
                        .setUri(cursor.getString(ConversationContract.LocusMeetingInfoEntry.URI.ordinal()))
                        .setLocalDialInNumber(pstnNumberFromJson(cursor.getString(ConversationContract.LocusMeetingInfoEntry.LOCAL_DIAL_IN_NUMBER.ordinal())))
                        .setMeetingName(cursor.getString(ConversationContract.LocusMeetingInfoEntry.MEETING_NAME.ordinal()))
                        .setIsPMR(cursor.getInt(ConversationContract.LocusMeetingInfoEntry.IS_PMR.ordinal()) != 0)
                        .setLastUpdated(cursor.getLong(ConversationContract.LocusMeetingInfoEntry.LAST_WRITE_TIME.ordinal()))
                        .build();

                    result = locusMeetingInfo;
                    putRecordInCache(locusMeetingInfo);


                    Ln.d(getLogString("Found LocusMeetingInfo in database", id, type));

                } catch (Exception e) {
                    Ln.e(e, "Failed to retrieve LocusMeetingInfo from database", id, type);
                }
            } else {
                Ln.d(getLogString("Did not find LocusMeetingInfo in database", id, type));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return result;
    }

    private boolean isRecordStale(LocusMeetingInfo locusMeetingInfo) {
        if (locusMeetingInfo == null) {
            return false;
        }
        return System.currentTimeMillis() > (locusMeetingInfo.getLastUpdated() + MEETING_INFO_TTL);
    }

    private void addToDatabase(LocusMeetingInfo locusMeetingInfo) {
        if (locusMeetingInfo == null) {
            return;
        }

        try {
            Ln.d("Adding LocusMeetingInfo to database: %s", getLocusMeetingInfoLogString(locusMeetingInfo));
            ContentValues cv = getContentValues(locusMeetingInfo);
            context.getContentResolver().insert(ConversationContract.LocusMeetingInfoEntry.CONTENT_URI, cv);
        } catch (Exception e) {
            Ln.e(e, "Failed to add LocusMeetingInfo to database: %s", getLocusMeetingInfoLogString(locusMeetingInfo));
        }
    }

    private int updateDatabase(LocusMeetingInfo locusMeetingInfo) {
        if (locusMeetingInfo == null) {
            return 0;
        }

        try {
            ContentValues cv = getContentValues(locusMeetingInfo);
            ConversationContract.LocusMeetingInfoEntry selection;
            String argument;
            if (locusMeetingInfo.isPmr() && locusMeetingInfo.getOwner() != null) {
                // A claimed PMR is handled differently as a single Spark user can have more than one PMR and each would
                // have a different Locus ID but the same owner. When a user's PMR is requested from Locus, Locus
                // will always return the user's currently preferred PMR and that is what we will store here, the preferred PMR.
                selection = ConversationContract.LocusMeetingInfoEntry.OWNER_ID;
                argument = locusMeetingInfo.getOwner();
            } else if (locusMeetingInfo.getSipMeetingUri() != null) {
                // CMR 3 does not have Locus URL. Default to using SIP address for non-claimed PMR
                selection = ConversationContract.LocusMeetingInfoEntry.SIP_MEETING_URI;
                argument = locusMeetingInfo.getSipMeetingUri();
            } else if (locusMeetingInfo.getWebExMeetingLink() != null) {
                // Last ditch attempt since SIP address was also null. Unsure how that can happen.
                selection = ConversationContract.LocusMeetingInfoEntry.WEBEX_MEETING_URL;
                argument = locusMeetingInfo.getWebExMeetingLink();
            } else {
                Ln.w("Not able to update LocusMeetingInfo from table as no unique identifier can be found: %s", getLocusMeetingInfoLogString(locusMeetingInfo));
                return 0;
            }

            Ln.d("updating LocusMeetingInfo in database: %s: ", getLocusMeetingInfoLogString(locusMeetingInfo));
            return context.getContentResolver().update(
                    ConversationContract.LocusMeetingInfoEntry.CONTENT_URI,
                    cv,
                    selection + "=?",
                    new String[]{argument}
            );
        } catch (Exception e) {
            Ln.e(e, "failed to update LocusMeetingInfo in database: %s", getLocusMeetingInfoLogString(locusMeetingInfo));
        }
        return 0;
    }

    private void deleteFromDatabase(LocusMeetingInfo locusMeetingInfo) {
        if (locusMeetingInfo == null) {
            return;
        }

        try {
            ConversationContract.LocusMeetingInfoEntry selection;
            String argument;
            if (locusMeetingInfo.getLocusUrl() != null) {
                selection = ConversationContract.LocusMeetingInfoEntry.LOCUS_URL;
                argument = locusMeetingInfo.getLocusUrl().toString();
            } else if (locusMeetingInfo.getSipMeetingUri() != null) {
                // CMR 3 does not have Locus URL. Default to using SIP address.
                selection = ConversationContract.LocusMeetingInfoEntry.SIP_MEETING_URI;
                argument = locusMeetingInfo.getSipMeetingUri();
            } else if (locusMeetingInfo.getWebExMeetingLink() != null) {
                // Last ditch attempt since SIP address was also null. Unsure how that can happen.
                selection = ConversationContract.LocusMeetingInfoEntry.WEBEX_MEETING_URL;
                argument = locusMeetingInfo.getWebExMeetingLink();
            } else {
                Ln.w("Not able to delete LocusMeetingInfo from table as no unique identifier can be found: %s", getLocusMeetingInfoLogString(locusMeetingInfo));
                return;
            }

            Ln.d("deleting LocusMeetingInfo from database: %s: ", getLocusMeetingInfoLogString(locusMeetingInfo));
            context.getContentResolver().delete(
                    ConversationContract.LocusMeetingInfoEntry.CONTENT_URI,
                    selection + "=?",
                    new String[]{argument});
        } catch (Exception e) {
            Ln.e(e, "failed to delete LocusMeetingInfo in database: %s", getLocusMeetingInfoLogString(locusMeetingInfo));
        }
    }

    private String getLocusID(LocusMeetingInfo locusMeetingInfo) {
        if (locusMeetingInfo == null || locusMeetingInfo.getLocusUrl() == null) {
            return null;
        }

        UUID uuid = UriUtils.extractUUID(Uri.parse(locusMeetingInfo.getLocusUrl().toString()));
        return uuid != null ? uuid.toString() : null;
    }

    private URI getLocusUriFromCursor(final Cursor cursor) {
        URI uri = null;
        if (cursor != null) {
            String uriAsString = cursor.getString(ConversationContract.LocusMeetingInfoEntry.LOCUS_URL.ordinal());
            if (uriAsString != null) {
                try {
                   uri = URI.create(uriAsString);
                } catch (IllegalArgumentException e) {
                    Ln.e("Failed to create URI from String %s", uriAsString);
                }
            }
        }

        return uri;
    }

    private String pstnNumberToJson(PstnNumber pstnNumber) {
        return (new Gson()).toJson(pstnNumber);
    }
    private PstnNumber pstnNumberFromJson(String pstnNumber) {
        return (new Gson()).fromJson(pstnNumber, PstnNumber.class);
    }

    private String callInNumbersInfoToJson(CallInNumbersInfo callInNumbersInfo) {
        return (new Gson()).toJson(callInNumbersInfo);
    }

    private CallInNumbersInfo callInNumbersInfoFromJson(String strCallInNumbersInfo) {
        return (new Gson()).fromJson(strCallInNumbersInfo, CallInNumbersInfo.class);
    }

    private ContentValues getContentValues(LocusMeetingInfo locusMeetingInfo) {
        ContentValues cv = new ContentValues();
        cv.put(ConversationContract.LocusMeetingInfoEntry.LOCUS_URL.name(), locusMeetingInfo.getLocusUrl() != null ? locusMeetingInfo.getLocusUrl().toString() : null);
        cv.put(ConversationContract.LocusMeetingInfoEntry.LOCUS_ID.name(), getLocusID(locusMeetingInfo));
        cv.put(ConversationContract.LocusMeetingInfoEntry.WEBEX_MEETING_URL.name(), locusMeetingInfo.getWebExMeetingLink());
        cv.put(ConversationContract.LocusMeetingInfoEntry.SIP_MEETING_URI.name(), locusMeetingInfo.getSipMeetingUri());
        cv.put(ConversationContract.LocusMeetingInfoEntry.CALL_IN_NUMBER_INFO.name(), callInNumbersInfoToJson(locusMeetingInfo.getCallInNumbersInfo()));
        cv.put(ConversationContract.LocusMeetingInfoEntry.MEETING_NUMBER.name(), locusMeetingInfo.getMeetingNumber());
        cv.put(ConversationContract.LocusMeetingInfoEntry.OWNER_ID.name(), locusMeetingInfo.getOwner());
        cv.put(ConversationContract.LocusMeetingInfoEntry.NUMERIC_CODE.name(), locusMeetingInfo.getNumericCode());
        cv.put(ConversationContract.LocusMeetingInfoEntry.URI.name(), locusMeetingInfo.getUri());
        cv.put(ConversationContract.LocusMeetingInfoEntry.LOCAL_DIAL_IN_NUMBER.name(), pstnNumberToJson(locusMeetingInfo.getLocalDialInNumber()));
        cv.put(ConversationContract.LocusMeetingInfoEntry.MEETING_NAME.name(), locusMeetingInfo.getMeetingName());
        cv.put(ConversationContract.LocusMeetingInfoEntry.IS_PMR.name(), locusMeetingInfo.isPmr());
        cv.put(ConversationContract.LocusMeetingInfoEntry.LAST_WRITE_TIME.name(), System.currentTimeMillis());

        return cv;
    }

    private class LocusMeetingInfoRecordRequest {

        final String id;
        final GetMeetingInfoType type;
        final List<Action<LocusMeetingInfo>> callbacks = new ArrayList<>();

        // These are set when request is processed
        LocusMeetingInfo result = null;
        boolean isStale = false;

        public LocusMeetingInfoRecordRequest(@NonNull String id, @NonNull GetMeetingInfoType type, @NonNull Action<LocusMeetingInfo> callback) {
            this.id = NameUtils.stripDialableProtocol(id);
            this.type = type;
            this.callbacks.add(callback);
        }

        public void fetchRecord() {
            if (TextUtils.isEmpty(id) || type == null || callbacks == null) {
                return;
            }


            LocusMeetingInfo locusMeetingInfo = getFromDB(id, type);

            if (locusMeetingInfo != null && !isRecordStale(locusMeetingInfo)) {
                result = locusMeetingInfo;
            } else {
                try {
                    Ln.d(getLogString("Requesting LocusMeetingInfo from Locus ", id, type));
                    Response<LocusMeetingInfo> response = apiClientProvider.getLocusClient().getMeetingInfo(id, type).execute();
                    if (response.isSuccessful()) {
                        locusMeetingInfo = response.body();
                        Ln.d(getLogString("Locus returned LocusMeetingInfo : %s", id, type), getLocusMeetingInfoLogString(locusMeetingInfo));

                        locusMeetingInfo.setLastUpdated(System.currentTimeMillis());
                        putRecordInCache(locusMeetingInfo);
                        if (response.body().isPmr() && result == null) {
                            if (type != GetMeetingInfoType.PERSONAL_ROOM
                                    && locusMeetingInfo.getOwner() != null) {
                                // We fetched a PMR but did not use the GetMeetingInfoType.PERSONAL_MEETING_ROOM
                                // This prevents an entry from being created with a duplicate owner field
                                // Even if we use the correct type when adding to the database, an update will
                                // use the owner field on the LocusMeetingInfo first, and if set, replace
                                // the existing one in the DB
                                boolean updated = updateDatabase(locusMeetingInfo) > 0;
                                Ln.d("Attempted to update database for LocusMeetingInfo with id " + id + " - updated: " + updated);
                                if (!updated) {
                                    addToDatabase(locusMeetingInfo);
                                }
                            } else {
                                // DB entry did not exist but does exist in Locus. Add to db.
                                addToDatabase(locusMeetingInfo);
                            }

                        } else if (response.body().isPmr() && isStale) {
                            // db entry is stale. Update db.
                            updateDatabase(locusMeetingInfo);
                        }
                    } else if (response.code() == HttpURLConnection.HTTP_NOT_FOUND) {
                        // 404 returned - LocusMeetingInfo entry does not exist in Locus
                        deleteFromDatabase(result);
                    }
                } catch (IOException exception) {
                    Ln.w(exception, getLogString("IOException caught fetching LocusMeetingInfo from Locus", id, type));
                } finally {
                    result = locusMeetingInfo;
                }
            }

        }


        public void callbackWithResult() {
            if (callbacks != null) {
                for (Action<LocusMeetingInfo> callback : callbacks) {
                    callback.call(result);
                }
            }
        }
    }

    private class ThrottledLoadMeetingInfoTask extends ThrottledAsyncTask {

        private LinkedBlockingQueue<LocusMeetingInfoRecordRequest> meetingInfoRecordRequests = new LinkedBlockingQueue<>();
        private LinkedBlockingQueue<LocusMeetingInfoRecordRequest> meetingInfoRecordRequestsCompleted = new LinkedBlockingQueue<>();

        public ThrottledLoadMeetingInfoTask() {
            //run every 500ms
            super(500);
        }

        public void add(LocusMeetingInfoRecordRequest request) {
            Ln.d(getLogString("Adding LocusMeetingInfoRecordRequest to request queue", request.id, request.type));
            meetingInfoRecordRequests.add(request);
            scheduleExecute();
        }

        @Override
        protected void doInBackground() {
            while (!meetingInfoRecordRequests.isEmpty()) {
                LocusMeetingInfoRecordRequest request;
                synchronized (taskSyncLock) {
                    request = meetingInfoRecordRequests.poll();
                }
                try {
                    request.fetchRecord();
                } catch (Throwable t) {
                    Ln.w(t, getLogString("Failed querying LocusMeetingInfo table for SingleLocusMeetingInfoRecordRequest", request.id, request.type));
                }
                meetingInfoRecordRequestsCompleted.add(request);
            }
        }

        @Override
        protected void onSuccess() {
            Ln.d("Processing " + meetingInfoRecordRequestsCompleted.size() + " LocusMeetingInfoRecordRequest(s) from completed queue");
            while (!meetingInfoRecordRequestsCompleted.isEmpty()) {
                LocusMeetingInfoRecordRequest request = meetingInfoRecordRequestsCompleted.poll();
                Ln.d(getLogString("Successfully completed LocusMeetingInfoRecordRequest", request.id, request.type));
                try {
                    request.callbackWithResult();
                } catch (Throwable t) {
                    Ln.w(t, getLogString("Failed callback for SingleLocusMeetingInfoRecordRequest", request.id, request.type));
                }
            }
        }

        @Override
        protected void onException(Exception e) throws RuntimeException {
            Ln.e(false, e);
        }

        @Nullable
        private LocusMeetingInfoRecordRequest get(String locusId) {
            if (LocusMeetingInfo.isEmptyString(locusId)) {
                return null;
            }

            for (LocusMeetingInfoRecordRequest record : meetingInfoRecordRequests) {
                if (record.id.equals(locusId)) {
                    return record;
                }
            }
            return null;
        }

        public boolean appendCallbackToExistingRequest(String id, Action<LocusMeetingInfo> callback) {
            if (TextUtils.isEmpty(id)) {
                return false;
            }

            LocusMeetingInfoRecordRequest existingRequest = get(id);
            if (existingRequest == null) {
                return false;
            } else {
                existingRequest.callbacks.add(callback);
                Ln.d("Request already exists for id: " + id + " - adding it to existing request callback list");
                return true;
            }
        }
    }

    private String getLogString(String msg, String id, GetMeetingInfoType type) {
        StringBuilder builder = new StringBuilder(msg);
        builder.append(" with id ");
        builder.append(id);
        builder.append(" and type ");
        builder.append(type);

        return builder.toString();
    }

    // DO NOT USE FOR INFO LEVEL LOGGING. Contains PII
    private String getLocusMeetingInfoLogString(LocusMeetingInfo locusMeetingInfo) {
        return "LocusMeetingInfo{" +
                "locusUrl=" + locusMeetingInfo.getLocusUrl() +
                ", webExMeetingLink='" + locusMeetingInfo.getWebExMeetingLink() + '\'' +
                ", sipMeetingUri='" + locusMeetingInfo.getSipMeetingUri() + '\'' +
                ", meetingNumber='" + locusMeetingInfo.getMeetingNumber() + '\'' +
                ", owner='" + locusMeetingInfo.getOwner() + '\'' +
                ", numericCode='" + locusMeetingInfo.getNumericCode() + '\'' +
                ", uri='" + locusMeetingInfo.getUri() + '\'' +
                ", meetingName='" + locusMeetingInfo.getMeetingName() + '\'' +
                ", isPmr=" + locusMeetingInfo.isPmr() +
                ", meetingLink=" + locusMeetingInfo.getMeetingLink() +
                '}';

    }


    private class UpdateLocusMeetingInfoTask extends ThrottledAsyncTask {

        private String id;
        private GetMeetingInfoType type;
        private Action<LocusMeetingInfo> callback;


        public UpdateLocusMeetingInfoTask(@NonNull String id, @NonNull GetMeetingInfoType type, @NonNull Action<LocusMeetingInfo> callback) {
            this.id = id;
            this.type = type;
            this.callback = callback;
        }
        @Override
        protected void doInBackground() {
            // TODO: without the LocusUrl being returned by hecate, this is necessary to remove existing claimed PMRs
            // from DB storage, as there is no LocusUrl to update/replace.
            context.getContentResolver().delete(ConversationContract.LocusMeetingInfoEntry.CONTENT_URI, ConversationContract.LocusMeetingInfoEntry.OWNER_ID + "=?", new String[]{id});
        }

        @Override
        protected void onSuccess() {
            // For now, just reach out to `meetingInfo` to grab latest.
            synchronized (taskSyncLock) {
                loadMeetingInfoTasks.add(new LocusMeetingInfoRecordRequest(id, type, callback));
            }
        }
    }

    public void replaceEntryInDatabase(@NonNull String ownerId, @NonNull LocusMeetingInfo info, @NonNull GetMeetingInfoType type, @NonNull Action<LocusMeetingInfo> callback) {
        // TODO: `info` parameter is the Hecate response that has fewer fields than LocusMeetingInfo and here is where we would do stuff with it
        // The `id` parameter here should always be the UUID
        new UpdateLocusMeetingInfoTask(ownerId, type, callback).scheduleExecute();
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEvent(LogoutEvent logoutEvent) {
        if (locusMeetingInfoCache != null) {
            locusMeetingInfoCache.evictAll();
        }
    }

    private void putRecordInCache(LocusMeetingInfo locusMeetingInfo) {
        String locusId = getLocusID(locusMeetingInfo);
        if (!LocusMeetingInfo.isEmptyString(locusId)) {
            locusMeetingInfoCache.put(locusId, locusMeetingInfo);
            Ln.d("new LocusMeetingInfo cached with key: " + locusId);
        } else {
            // There are instances of PMRs whose LocusMeetingInfo does not always have a locusUrl in the DTO,
            // resulting in the LruCache throwing a NPE upon put()
            Ln.w("putRecordInCache() Cannot cache LocusMeetingInfo");
        }
    }
}
