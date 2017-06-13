package com.cisco.spark.android.meetings;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

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
import java.net.URI;
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
 * {@link LocusMeetingInfoProvider#get(String, com.cisco.spark.android.meetings.GetMeetingInfoType, com.cisco.spark.android.util.Action)}
 *
 * When a record is requested, it is first searched for in the database. If no record is found, then it is requested from Locus. If a
 * record is returned from Locus, then it is added to the database. If a record is found in the database first, it will be fetched from
 * Locus again if it was written to the database more than 1-day in the past. At the moment there is no mechanism for Locus to push an updated
 * LocusMeetingInfo record down to a client. If a "stale" LocusMeetingInfo record is found in the database but the request to Locus does not
 * return a result, then the database entry will be deleted.
 */
@Singleton
public class LocusMeetingInfoProvider {

    // A database entry more than 1-day prior will be updated with the current values from Locus
    private static final long MEETING_INFO_TTL = TimeUnit.DAYS.toMillis(1);

    protected Context context;
    protected ApiClientProvider apiClientProvider;
    protected ThrottledLoadMeetingInfoTask loadMeetingInfoTasks;

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
     */
    public void get(@NonNull final String id, @NonNull final GetMeetingInfoType type, @NonNull final Action<LocusMeetingInfo> callback) {
        loadMeetingInfoTasks.add(new SingleLocusMeetingInfoRecordRequest(id, type, callback));
    }

    private void getFromDB(final SingleLocusMeetingInfoRecordRequest request) {

        Cursor cursor = null;
        ConversationContract.LocusMeetingInfoEntry selection;
        try {
            switch (request.type) {
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
                    return;
            }

            cursor = context.getContentResolver().query(
                    ConversationContract.LocusMeetingInfoEntry.CONTENT_URI,
                    ConversationContract.LocusMeetingInfoEntry.DEFAULT_PROJECTION,
                    selection + "=?",
                    new String[]{request.id},
                    null);

            if (cursor != null && cursor.getCount() > 0) {
                try {
                    cursor.moveToFirst();

                    LocusMeetingInfo locusMeetingInfo = new LocusMeetingInfo.LocusMeetingInfoBuilder(URI.create(cursor.getString(ConversationContract.LocusMeetingInfoEntry.LOCUS_URL.ordinal())))
                        .setWebExMeetingLink(cursor.getString(ConversationContract.LocusMeetingInfoEntry.WEBEX_MEETING_URL.ordinal()))
                        .setSipMeetingUri(cursor.getString(ConversationContract.LocusMeetingInfoEntry.SIP_MEETING_URI.ordinal()))
                        .setCallInNumbersInfo(callInNumbersInfoFromJson(cursor.getString(ConversationContract.LocusMeetingInfoEntry.CALL_IN_NUMBER_INFO.ordinal())))
                        .setMeetingNumber(cursor.getString(ConversationContract.LocusMeetingInfoEntry.MEETING_NUMBER.ordinal()))
                        .setOwner(cursor.getString(ConversationContract.LocusMeetingInfoEntry.OWNER_ID.ordinal()))
                        .setNumericCode(cursor.getString(ConversationContract.LocusMeetingInfoEntry.NUMERIC_CODE.ordinal()))
                        .setUri(cursor.getString(ConversationContract.LocusMeetingInfoEntry.URI.ordinal()))
                        .setLocalDialInNumber(pstnNumberFromJson(cursor.getString(ConversationContract.LocusMeetingInfoEntry.LOCAL_DIAL_IN_NUMBER.ordinal())))
                        .setMeetingName(cursor.getString(ConversationContract.LocusMeetingInfoEntry.MEETING_NAME.ordinal()))
                        .setIsPMR(cursor.getInt(ConversationContract.LocusMeetingInfoEntry.IS_PMR.ordinal()) == 0 ? false : true)
                        .build();

                    final long lastWriteTime = cursor.getLong(ConversationContract.LocusMeetingInfoEntry.LAST_WRITE_TIME.ordinal());
                    request.isStale = System.currentTimeMillis() > (lastWriteTime + MEETING_INFO_TTL);
                    request.result = locusMeetingInfo;

                } catch (Exception e) {
                    Ln.e(e, "failed to retrieve LocusMeetingInfo from database with ID %s and type %s", request.id, request.type);
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return;
    }

    private void addToDatabase(LocusMeetingInfo locusMeetingInfo) {
        if (locusMeetingInfo == null) {
            return;
        }

        try {
            Ln.d("adding LocusMeetingInfo to database: %s", locusMeetingInfo.toString());
            ContentValues cv = getContentValues(locusMeetingInfo);
            context.getContentResolver().insert(ConversationContract.LocusMeetingInfoEntry.CONTENT_URI, cv);
        } catch (Exception e) {
            Ln.e(e, "failed to add LocusMeetingInfo to databae: %s", locusMeetingInfo.toString());
        }
    }

    private void updateDatabase(LocusMeetingInfo locusMeetingInfo) {
        if (locusMeetingInfo == null) {
            return;
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
            } else {
                selection = ConversationContract.LocusMeetingInfoEntry.LOCUS_ID;
                argument = getLocusID(locusMeetingInfo);
            }

            Ln.d("updating LocusMeetingInfo in database: %s: ", locusMeetingInfo.toString());
            context.getContentResolver().update(
                    ConversationContract.LocusMeetingInfoEntry.CONTENT_URI,
                    cv,
                    selection + "=?",
                    new String[]{argument}
            );
        } catch (Exception e) {
            Ln.e(e, "failed to update LocusMeetingInfo in database: %s", locusMeetingInfo.toString());
        }
    }

    private void deleteFromDatabase(LocusMeetingInfo locusMeetingInfo) {
        if (locusMeetingInfo == null) {
            return;
        }

        try {
            Ln.d("deleting LocusMeetingInfo from database: %s: ", locusMeetingInfo.toString());
            context.getContentResolver().delete(
                    ConversationContract.LocusMeetingInfoEntry.CONTENT_URI,
                    ConversationContract.LocusMeetingInfoEntry.LOCUS_ID + "=?",
                    new String[]{getLocusID(locusMeetingInfo)}
            );
        } catch (Exception e) {
            Ln.e(e, "failed to delete LocusMeetingInfo in database: %s", locusMeetingInfo.toString());
        }
    }

    private String getLocusID(LocusMeetingInfo locusMeetingInfo) {
        UUID uuid = UriUtils.extractUUID(Uri.parse(locusMeetingInfo.getLocusUrl().toString()));
        return uuid != null ? uuid.toString() : null;
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
        cv.put(ConversationContract.LocusMeetingInfoEntry.LOCUS_URL.name(), locusMeetingInfo.getLocusUrl().toString());
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

    private interface LocusMeetingInfoRecordRequest {
        void fetchRecord();
        void callbackWithResult();
    }

    private class SingleLocusMeetingInfoRecordRequest implements LocusMeetingInfoRecordRequest {

        final String id;
        final GetMeetingInfoType type;
        final Action<LocusMeetingInfo> callback;

        // These are set when request is processed
        LocusMeetingInfo result = null;
        boolean isStale = false;

        public SingleLocusMeetingInfoRecordRequest(@NonNull String id, @NonNull GetMeetingInfoType type, @NonNull Action<LocusMeetingInfo> callback) {
            this.id = NameUtils.stripDialableProtocol(id);
            this.type = type;
            this.callback = callback;
        }

        @Override
        public void fetchRecord() {
            if (TextUtils.isEmpty(id) || type == null || callback == null) {
                return;
            }

            getFromDB(this);
            if (result != null && !isStale) {
                return;
            } else {
                LocusMeetingInfo locusMeetingInfo = null;
                try {
                    Response<LocusMeetingInfo> response = apiClientProvider.getLocusClient().getMeetingInfo(id, type).execute();
                    if (response.isSuccessful()) {
                        locusMeetingInfo = response.body();
                        if (result == null) {
                            // DB entry did not exist but does exist in Locus. Add to db.
                            addToDatabase(locusMeetingInfo);
                        } else if (isStale) {
                            // db entry is stale. Update db.
                            updateDatabase(locusMeetingInfo);
                        }
                    } else {
                        if (response.code() == 404) {
                            // 404 returned - LocusMeetingInfo entry does not exist in Locus
                            deleteFromDatabase(result);
                        }
                    }
                } catch (IOException exception) {
                    Ln.w(exception, "IOException returned fetching LocusMeetingInfo with ID %s and type %s", id, type);
                } finally {
                    result = locusMeetingInfo;
                }
            }
        }

        @Override
        public void callbackWithResult() {
            if (callback != null) {
                callback.call(result);
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
            meetingInfoRecordRequests.add(request);
            scheduleExecute();
        }

        @Override
        protected void doInBackground() {
            while (!meetingInfoRecordRequests.isEmpty()) {
                LocusMeetingInfoRecordRequest request = meetingInfoRecordRequests.poll();
                try {
                    request.fetchRecord();
                } catch (Throwable t) {
                    Ln.w(t, "failed querying meetings table");
                }
                meetingInfoRecordRequestsCompleted.add(request);
            }
        }

        @Override
        protected void onSuccess() {
            Ln.d("processing " + meetingInfoRecordRequestsCompleted.size() + " LocusMeetingInfoRecordRequest results");
            while (!meetingInfoRecordRequestsCompleted.isEmpty()) {
                LocusMeetingInfoRecordRequest request = meetingInfoRecordRequestsCompleted.poll();
                try {
                    request.callbackWithResult();
                } catch (Throwable t) {
                    Ln.w(t, "failed callback with LocusMeetingInfo results");
                }
            }
        }

        @Override
        protected void onException(Exception e) throws RuntimeException {
            Ln.e(false, e);
        }
    }
}
