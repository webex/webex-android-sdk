package com.cisco.spark.android.callcontrol;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CallLog;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;

import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.ApplicationController;
import com.cisco.spark.android.core.Component;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.core.Settings;
import com.cisco.spark.android.locus.model.LocusParticipantInfo;
import com.cisco.spark.android.locus.model.UserRecentSession;
import com.cisco.spark.android.locus.model.UserRecentSessions;
import com.cisco.spark.android.mercury.events.UserRecentSessionsEvent;
import com.cisco.spark.android.sync.ActorRecordProvider;
import com.cisco.spark.android.sync.Batch;
import com.cisco.spark.android.sync.ConversationContentProviderQueries;
import com.cisco.spark.android.sync.ConversationContract;
import com.cisco.spark.android.sync.DBHelperUtils;
import com.cisco.spark.android.ui.conversation.ConversationResolver;
import com.cisco.spark.android.util.DateUtils;
import com.cisco.spark.android.util.IntentUtils;
import com.cisco.spark.android.util.UIUtils;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.github.benoitdion.ln.Ln;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.Date;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import de.greenrobot.event.EventBus;
import retrofit2.Response;

@Singleton
public class CallHistoryService implements Component {
    private static final int CALLS_LIMIT = 100;
    private static final int NUMBER_DAYS_HISTORY = 30;
    private static final String SORT_ORDER = "desc";

    private final Context context;
    private final DeviceRegistration deviceRegistration;
    private final ApiClientProvider apiClientProvider;
    private final Gson gson;
    private final EventBus bus;
    private final Provider<Batch> batchProvider;
    private final Injector injector;
    private final Settings settings;
    private final ActorRecordProvider actorRecordProvider;

    private final static Uri SAMSUNG_CALL_LOGS_CONTENT_URI = Uri.parse("content://logs/call");

    @Inject
    public CallHistoryService(Context context, DeviceRegistration deviceRegistration,
                              EventBus bus, ApiClientProvider apiClientProvider, Gson gson, Provider<Batch> batchProvider, Injector injector, Settings settings, ActorRecordProvider actorRecordProvider) {
        this.context = context;
        this.deviceRegistration = deviceRegistration;
        this.apiClientProvider = apiClientProvider;
        this.gson = gson;
        this.bus = bus;
        this.batchProvider = batchProvider;
        this.injector = injector;
        this.settings = settings;
        this.actorRecordProvider = actorRecordProvider;
    }


    public UserRecentSession getUserRecentSessionFromCursor(Cursor cursor) {
        UserRecentSession userRecentSession = new UserRecentSession();

        userRecentSession.setStartTime(new Date(cursor.getLong(ConversationContract.vw_CallHistory.START_TIME.ordinal())));
        userRecentSession.setEndTime(new Date(cursor.getLong(ConversationContract.vw_CallHistory.END_TIME.ordinal())));
        userRecentSession.setDurationSeconds(cursor.getLong(ConversationContract.vw_CallHistory.DURATION_SECS.ordinal()));
        userRecentSession.setJoinedDurationSeconds(cursor.getLong(ConversationContract.vw_CallHistory.JOINED_DURATION_SECS.ordinal()));
        userRecentSession.setDirection(cursor.getString(ConversationContract.vw_CallHistory.DIRECTION.ordinal()));
        userRecentSession.setDisposition(cursor.getString(ConversationContract.vw_CallHistory.DISPOSTION.ordinal()));
        userRecentSession.setParticipantCount(cursor.getInt(ConversationContract.vw_CallHistory.PARTICIPANT_COUNT.ordinal()));
        userRecentSession.setCallbackAddress(cursor.getString(ConversationContract.vw_CallHistory.CALLBACK_ADDRESS.ordinal()));

        LocusParticipantInfo otherParticipant = new LocusParticipantInfo();
        userRecentSession.setOther(otherParticipant);
        otherParticipant.setId(cursor.getString(ConversationContract.vw_CallHistory.OTHER_UUID.ordinal()));
        otherParticipant.setEmail(cursor.getString(ConversationContract.vw_CallHistory.OTHER_EMAIL.ordinal()));
        otherParticipant.setName(cursor.getString(ConversationContract.vw_CallHistory.OTHER_NAME.ordinal()));
        otherParticipant.setExternal(cursor.getInt(ConversationContract.vw_CallHistory.OTHER_IS_EXTERNAL.ordinal()) != 0);
        otherParticipant.setPrimaryDisplayString(cursor.getString(ConversationContract.vw_CallHistory.OTHER_PRIMARY_DISPLAY_STRING.ordinal()));
        otherParticipant.setSecondaryDisplayString(cursor.getString(ConversationContract.vw_CallHistory.OTHER_SECONDARY_DISPLAY_STRING.ordinal()));
        otherParticipant.setPhoneNumber(cursor.getString(ConversationContract.vw_CallHistory.OTHER_PHONE_NUMBER.ordinal()));
        otherParticipant.setSipUrl(cursor.getString(ConversationContract.vw_CallHistory.OTHER_SIP_URL.ordinal()));
        otherParticipant.setTelUrl(cursor.getString(ConversationContract.vw_CallHistory.OTHER_TEL_URL.ordinal()));
        otherParticipant.setOwnerId(cursor.getString(ConversationContract.vw_CallHistory.OTHER_OWNER_ID.ordinal()));

        String conversationUrl = cursor.getString(ConversationContract.vw_CallHistory.CONVERSATION_URL.ordinal());
        if (conversationUrl != null) {
            userRecentSession.setConversationUrl(conversationUrl);
            userRecentSession.setConversationDisplayName(cursor.getString(ConversationContract.vw_CallHistory.CONVERSATION_DISPLAY_NAME.ordinal()));

            ConversationResolver resolver = new ConversationResolver(injector, cursor);
            userRecentSession.setConversationResolver(resolver);
        }

        return userRecentSession;
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEvent(UserRecentSessionsEvent event) {
        Ln.i("CallHistoryService.onEvent(UserRecentSessionsEvent)");

        writeCallHistoryEntryToDB(event.getUserRecentSessions());
    }

    /**
     * Retrieve Call History data from server.  Note that this first queries DB for latest entry
     * already stored and usees that as "from" parameter when querying history endpoint.  Store
     * results in DB.
     */
    private void getCallHistory() {
        Date fromDate = new Date(DateUtils.getTimestampDaysAgo(NUMBER_DAYS_HISTORY));

        // check DB for latest call history entry....query from that date on
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(ConversationContract.CallHistory.CONTENT_URI,
                    new String[]{ConversationContract.vw_CallHistory.END_TIME.name()},
                    null, null,
                    ConversationContract.CallHistory.END_TIME + " DESC LIMIT 1");
            if (cursor != null && cursor.moveToFirst()) {
                fromDate = new Date(cursor.getLong(0) + 1);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        Ln.d("getRecentCalls(), from date = " + fromDate);
        String fromDateString = DateUtils.formatUTCDateString(fromDate);

        try {
            Response<UserRecentSessions> response = apiClientProvider.getJanusClient().getRecentCalls(fromDateString,
                    CALLS_LIMIT, SORT_ORDER).execute();
            if (response.isSuccessful()) {
                UserRecentSessions userRecentSessions = response.body();
                writeCallHistoryEntryToDB(userRecentSessions);
            }
        } catch (IOException e) {
            Ln.w(e, "Failed getting call history");
        }
    }

    protected void writeCallHistoryEntryToDB(UserRecentSessions userRecentSessions) {
        Batch batch = batchProvider.get();
        for (UserRecentSession userRecentSession : userRecentSessions.getUserSessions()) {
            LocusParticipantInfo otherParticipant = userRecentSession.getOther();

            if (otherParticipant != null) {
                ContentProviderOperation.Builder cpo = ContentProviderOperation.newInsert(ConversationContract.CallHistory.CONTENT_URI)
                        .withValue(ConversationContract.CallHistory.URL.name(), userRecentSession.getUrl().toString())
                        .withValue(ConversationContract.CallHistory.START_TIME.name(), userRecentSession.getStartTime().getTime())
                        .withValue(ConversationContract.CallHistory.END_TIME.name(), userRecentSession.getEndTime().getTime())
                        .withValue(ConversationContract.CallHistory.DURATION_SECS.name(), userRecentSession.getDurationSeconds())
                        .withValue(ConversationContract.CallHistory.JOINED_DURATION_SECS.name(), userRecentSession.getJoinedDurationSeconds())
                        .withValue(ConversationContract.CallHistory.DIRECTION.name(), userRecentSession.getDirection())
                        .withValue(ConversationContract.CallHistory.DISPOSTION.name(), userRecentSession.getDisposition())
                        .withValue(ConversationContract.CallHistory.OTHER_UUID.name(), otherParticipant.getId().toString())
                        .withValue(ConversationContract.CallHistory.OTHER_EMAIL.name(), otherParticipant.getEmail())
                        .withValue(ConversationContract.CallHistory.OTHER_NAME.name(), otherParticipant.getName())
                        .withValue(ConversationContract.CallHistory.OTHER_IS_EXTERNAL.name(), otherParticipant.isExternal())
                        .withValue(ConversationContract.CallHistory.OTHER_PRIMARY_DISPLAY_STRING.name(), otherParticipant.getPrimaryDisplayString())
                        .withValue(ConversationContract.CallHistory.OTHER_SECONDARY_DISPLAY_STRING.name(), otherParticipant.getSecondaryDisplayString())
                        .withValue(ConversationContract.CallHistory.OTHER_PHONE_NUMBER.name(), otherParticipant.getPhoneNumber())
                        .withValue(ConversationContract.CallHistory.OTHER_SIP_URL.name(), otherParticipant.getSipUrl())
                        .withValue(ConversationContract.CallHistory.OTHER_TEL_URL.name(), otherParticipant.getTelUrl())
                        .withValue(ConversationContract.CallHistory.CALLBACK_ADDRESS.name(), userRecentSession.getCallbackAddress())
                        .withValue(ConversationContract.CallHistory.PARTICIPANT_COUNT.name(), userRecentSession.getParticipantCount())
                        .withValue(ConversationContract.CallHistory.CONVERSATION_URL.name(), userRecentSession.getConversationUrl());
                if (otherParticipant.getOwnerId() != null) {
                    cpo.withValue(ConversationContract.CallHistory.OTHER_OWNER_ID.name(), otherParticipant.getOwnerId().toString());
                }

                batch.add(cpo.build());
            }
        }
        batch.apply();

        writeCallHistoryEntryToCallLog(userRecentSessions);
    }

    public void writeCallHistoryToCallLog() {
        Date fromDate = new Date(DateUtils.getTimestampDaysAgo(NUMBER_DAYS_HISTORY));

        Ln.d("getRecentCalls(), from date = " + fromDate);
        String fromDateString = DateUtils.formatUTCDateString(fromDate);
        try {
            Response<UserRecentSessions> response = apiClientProvider.getJanusClient().getRecentCalls(fromDateString,
                    CALLS_LIMIT, SORT_ORDER).execute();

            if (response.isSuccessful()) {
                UserRecentSessions userRecentSessions = response.body();
                if (userRecentSessions.getUserSessions() != null) {
                    writeCallHistoryEntryToCallLog(userRecentSessions);
                }
            } else {
                Ln.i("Failed downloading recent calls for call log");
            }
        } catch (IOException e) {
            Ln.i(e, "Failed downloading recent calls for call log");
        }
    }

    @TargetApi(21)
    protected void writeCallHistoryEntryToCallLog(UserRecentSessions userRecentSessions) {
        if (!settings.isContactSyncEnabled())
            return;

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            Ln.w("No permission to write call log");
            return;
        }

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            Ln.w("No permission to read call log");
            return;
        }

        try {
            for (UserRecentSession userRecentSession : userRecentSessions.getUserSessions()) {
                if (callLogExists(userRecentSession))
                    continue;

                int callType = CallLog.Calls.OUTGOING_TYPE;

                if ("MISSED".equals(userRecentSession.getDisposition()))
                    callType = CallLog.Calls.MISSED_TYPE;
                else if ("INCOMING".equals(userRecentSession.getDirection()))
                    callType = CallLog.Calls.INCOMING_TYPE;

                String convUri = userRecentSession.getConversationUrl();
                String convId = null;
                if (!TextUtils.isEmpty(convUri)) {
                    Uri uri = Uri.parse(convUri);
                    convId = uri.getLastPathSegment();
                    convUri = IntentUtils.getSparkConversationUri(convId).toString();
                }

                if (TextUtils.isEmpty(convUri) && userRecentSession.getOther() != null) {
                    convUri = userRecentSession.getOther().getSipUrl();
                }

                String displayName = userRecentSession.getConversationDisplayName();
                if (TextUtils.isEmpty(displayName))
                    displayName = getConversationDisplayName(convId);
                if (TextUtils.isEmpty(displayName) && userRecentSession.getOther() != null)
                    displayName = userRecentSession.getOther().getDisplayName();

                long duration = userRecentSession.getJoinedDurationSeconds();
                if (duration == 0)
                    duration = userRecentSession.getDurationSeconds();

                if (!TextUtils.isEmpty(convUri)) {
                    ContentValues values = new ContentValues();
                    values.put(CallLog.Calls.NUMBER, convUri);
                    values.put(CallLog.Calls.DATE, userRecentSession.getStartTime().getTime());
                    values.put(CallLog.Calls.DURATION, duration);
                    values.put(CallLog.Calls.TYPE, callType);
                    values.put(CallLog.Calls.NEW, 0);
                    values.put(CallLog.Calls.CACHED_NUMBER_LABEL, displayName);

                    if (UIUtils.hasLollipop())
                        values.put(CallLog.Calls.FEATURES, CallLog.Calls.FEATURES_VIDEO);

                    if (userRecentSession.getParticipantCount() > 2) {
                        values.put("logtype", 1550);
                    } else {
                        values.put("logtype", 1500);
                        if (userRecentSession.getOther() != null)
                            values.put("sec_custom2", userRecentSession.getOther().getEmail());
                    }
                    Uri ret = context.getContentResolver().insert(SAMSUNG_CALL_LOGS_CONTENT_URI, values);
                    Ln.d("Inserted call log " + ret);
                }
            }
        } catch (Exception e) {
            //TODO better handling for Samsung detection would be nice
            Ln.d("Failed adding to call log (probably not a Samsung device");
        }
    }

    private String getConversationDisplayName(String convId) {
        if (TextUtils.isEmpty(convId)) {
            return null;
        }
        Bundle conv = ConversationContentProviderQueries.getConversationById(context.getContentResolver(), convId);
        return conv.getString(ConversationContract.ConversationEntry.CONVERSATION_DISPLAY_NAME.name());
    }

    private boolean callLogExists(UserRecentSession userRecentSession) {
        Cursor c = null;
        try {
            c = context.getContentResolver().query(
                    SAMSUNG_CALL_LOGS_CONTENT_URI,
                    null,
                    CallLog.Calls.DATE + "=? AND " + CallLog.Calls.DURATION + "=?",
                    new String[]{String.valueOf(userRecentSession.getStartTime().getTime()), String.valueOf(userRecentSession.getJoinedDurationSeconds())},
                    null);

            if (c != null && c.moveToFirst()) {
                String str = DBHelperUtils.cursorToString(c);
                Ln.w(str);
            }

            return c != null && c.getCount() > 0;
        } finally {
            if (c != null)
                c.close();
        }
    }

    private void deleteOldCallHistoryData() {
        Batch batch = batchProvider.get();
        ContentProviderOperation op = ContentProviderOperation
                .newDelete(ConversationContract.CallHistory.CONTENT_URI)
                .withSelection(ConversationContract.CallHistory.START_TIME + "< ?",
                        new String[]{Long.toString(DateUtils.getTimestampDaysAgo(NUMBER_DAYS_HISTORY))}).build();
        batch.add(op);
        batch.apply();
    }

    @Override
    public boolean shouldStart() {
        return true;
    }

    @Override
    public void start() {
        Ln.i("App->CallHistoryService: start()");
        deleteOldCallHistoryData();
        getCallHistory();
        if (!bus.isRegistered(this))
            bus.register(this);
    }

    @Override
    public void stop() {
        Ln.i("App->CallHistoryService: stop()");
        if (bus.isRegistered(this))
            bus.unregister(this);
    }

    public void setApplicationController(ApplicationController applicationController) {
        applicationController.register(this);
    }
}

