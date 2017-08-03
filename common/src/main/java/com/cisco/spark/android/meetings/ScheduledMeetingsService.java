package com.cisco.spark.android.meetings;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.text.TextUtils;

import com.cisco.spark.android.client.CalendarServiceClient;
import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.ApplicationController;
import com.cisco.spark.android.core.Component;
import com.cisco.spark.android.core.PermissionsHelper;
import com.cisco.spark.android.events.ObtpRemoveEvent;
import com.cisco.spark.android.events.ObtpShowEvent;
import com.cisco.spark.android.locus.events.LocusDataCacheChangedEvent;
import com.cisco.spark.android.locus.model.Locus;
import com.cisco.spark.android.locus.model.LocusData;
import com.cisco.spark.android.locus.model.LocusDataCache;
import com.cisco.spark.android.locus.model.LocusKey;
import com.cisco.spark.android.locus.model.LocusScheduledMeeting;
import com.cisco.spark.android.locus.model.LocusState;
import com.cisco.spark.android.model.CalendarMeeting;
import com.cisco.spark.android.util.Action;
import com.cisco.spark.android.util.SafeAsyncTask;
import com.cisco.spark.android.util.Strings;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.github.benoitdion.ln.Ln;
import com.google.gson.Gson;

import java.util.Date;

import javax.inject.Inject;
import javax.inject.Singleton;

import de.greenrobot.event.EventBus;
import retrofit2.adapter.rxjava.HttpException;
import rx.schedulers.Schedulers;

@Singleton
public class ScheduledMeetingsService implements Component {
    private Context context;
    private ApiClientProvider apiClientProvider;
    private MeetingCryptoUtils meetingCryptoUtils;
    private LocusDataCache locusDataCache;
    private DeviceRegistration deviceRegistration;
    private PermissionsHelper permissionsHelper;
    private Gson gson;
    private EventBus bus;

    @Inject
    public ScheduledMeetingsService(Context context, ApiClientProvider apiClientProvider, MeetingCryptoUtils meetingCryptoUtils, LocusDataCache locusDataCache, DeviceRegistration deviceRegistration, PermissionsHelper permissionsHelper, Gson gson, EventBus bus) {
        this.context = context;
        this.apiClientProvider = apiClientProvider;
        this.meetingCryptoUtils = meetingCryptoUtils;
        this.locusDataCache = locusDataCache;
        this.deviceRegistration = deviceRegistration;
        this.permissionsHelper = permissionsHelper;
        this.gson = gson;
        this.bus = bus;
    }

    public void onEvent(LocusDataCacheChangedEvent event) {
        if (!deviceRegistration.getFeatures().isScheduledMeetingV2Enabled()) return;
        Locus locus = locusDataCache.getLocus(event.getLocusKey());
        if (locus != null) {
            if (locus.getFullState().getState() == LocusState.State.INACTIVE || locus.getFullState().getState() == LocusState.State.TERMINATING)
                bus.post(new ObtpRemoveEvent(event.getLocusKey()));
        }

    }

    private void resumeActiveObtp(LocusKey locusKey) {
        Locus locus = locusDataCache.getLocus(locusKey);
        if (locus != null && locus.getMeeting() != null) {
            LocusData locusData = locusDataCache.getLocusData(locusKey);
            if (locus.getFullState().getState() == LocusState.State.INITIALIZING || locus.getFullState().getState() == LocusState.State.ACTIVE && locusData != null && !locusData.isOneOnOne()) {

                LocusScheduledMeeting locusScheduledMeeting = locus.getMeeting();

                // poke the local spark client sqlite db for this meeting
                String meetingId = locusScheduledMeeting.getMeetingId();
                CalendarMeeting calendarMeeting = getMeetingFromLocalDb(meetingId);
                if (calendarMeeting != null) {
                    if (!isAtSparkMeeting(calendarMeeting))
                        return;
                    bus.post(new ObtpShowEvent(locus, calendarMeeting));
                    Ln.i("Meeting retrieved from the local client sqlite db");
                    return;
                }

                // poke the local calendar service for this meeting
                String seriesId = locusScheduledMeeting.getIcalUid();
                calendarMeeting = getMeetingFromLocalCalendar(seriesId);
                if (calendarMeeting != null) {
                    if (!isAtSparkMeeting(calendarMeeting))
                        return;
                    bus.post(new ObtpShowEvent(locus, calendarMeeting));
                    Ln.i("Meeting retrieved from the local calendar service");
                    return;
                }

                // if the above two fails, go get it from the remote calendar service
                getRemoteMeetingResource(locusScheduledMeeting.getResourceUrl().toString());
            }
        }
    }

    private CalendarMeeting getMeetingFromLocalDb(String meetingId) {
        return CalendarMeeting.buildFromContentResolver(context.getContentResolver(), gson, meetingId);
    }

    /**
     * @param seriesId the seriesId of a meeting, aka global id, iCalUid, or lastSync id
     * @return decrypted plain text meeting resource populated with details from the local calendar
     */

    private CalendarMeeting getMeetingFromLocalCalendar(String seriesId) {
        if (!permissionsHelper.hasCalendarPermission() || TextUtils.isEmpty(seriesId))
            return null;

        Cursor cursor = null;
        Uri uri = CalendarContract.Events.CONTENT_URI
                .buildUpon()
                .build();
        try {
            cursor = context.getContentResolver().query(uri,
                    new String[]{
                            CalendarContract.Events._ID,
                            CalendarContract.Events.TITLE,
                            CalendarContract.Events.DTSTART,
                            CalendarContract.Events.DURATION,
                            CalendarContract.Events.EVENT_LOCATION
                    },
                    CalendarContract.Events.SYNC_DATA1 + " =? or " + CalendarContract.Events.SYNC_DATA2 + " =?",
                    new String[]{
                            seriesId,
                            seriesId
                    },
                    CalendarContract.Events.DTSTART + " ASC",
                    null);
            if (cursor != null && cursor.getCount() != 0 && cursor.moveToFirst()) {
                CalendarMeeting localCalendarMeeting = new CalendarMeeting();
                localCalendarMeeting.setEventId(cursor.getLong(cursor.getColumnIndex(CalendarContract.Events._ID)));
                localCalendarMeeting.setSubject(cursor.getString(cursor.getColumnIndex(CalendarContract.Events.TITLE)));
                localCalendarMeeting.setStartTime(new Date(cursor.getLong(cursor.getColumnIndex(CalendarContract.Events.DTSTART))));
                localCalendarMeeting.setDurationMinutes(cursor.getInt(cursor.getColumnIndex(CalendarContract.Events.DURATION)));
                localCalendarMeeting.setLocation(cursor.getString(cursor.getColumnIndex(CalendarContract.Events.EVENT_LOCATION)));
                return localCalendarMeeting;
            }
            return null;
        } catch (Exception e) {
            Ln.e(e, "An error occurred while trying to get sync'd meetings");
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public void getMeetingFromLocalCalendar(final String id, Action<CalendarMeeting> sucess) {
        this.new GetMeetingFromLocalCalendar(id, sucess).execute();
    }

    private void getRemoteMeetingResource(String resourceUrl) {
        Ln.i("Getting meeting resource");
        CalendarServiceClient calendarServiceClient = apiClientProvider.getCalendarServiceClient();
        String id = Strings.getLastBitFromUrl(resourceUrl);
        calendarServiceClient.getMeetingResource(id).subscribeOn(Schedulers.io()).subscribe(calendarMeeting -> onRemoteMeetingResourceArrival(calendarMeeting), throwable -> {
            if (throwable instanceof HttpException) {
                HttpException httpResponse = (HttpException) throwable;
                Ln.i(httpResponse, "Getting remote meeting resource failed");
            }
        });
    }

    /**
     * Meeting resource is an encrypted meeting details that is
     * available either locally in the clients db or remotedly by querying a meeting resource url
     *
     * @param calendarMeeting meeting details with title, time, location, description and others
     */

    private void onRemoteMeetingResourceArrival(CalendarMeeting calendarMeeting) {
        Ln.i("Remote meeting resource request finished successfully");
        calendarMeeting = meetingCryptoUtils.decryptMeeting(calendarMeeting);
        processMeetingResource(calendarMeeting);
    }


    private void processMeetingResource(CalendarMeeting calendarMeeting) {
        if (calendarMeeting == null)
            return;

        if (!isAtSparkMeeting(calendarMeeting))
            return;
        Ln.i("Preparing meeting resource for render");
        LocusData locusData = locusDataCache.getLocusData(LocusKey.fromString(calendarMeeting.getCallURI()));
        if (locusData != null) {
            Locus locus = locusData.getLocus();
            if (locus != null)
                bus.post(new ObtpShowEvent(locus, calendarMeeting));
        }
    }

    // add check for @spark tag in case the meeting is from the local calendar
    private boolean isAtSparkMeeting(CalendarMeeting calendarMeeting) {
        return calendarMeeting != null && (calendarMeeting.isSparkMeeting() || calendarMeeting.getLocation().contains(CalendarMeeting.MeetingTag.SPARK.getTag()));
    }

    public void checkForActiveObtps() {
        if (!deviceRegistration.getFeatures().isScheduledMeetingV2Enabled()) return;
        ActiveObtpsScanner activeObtpsScanner = new ActiveObtpsScanner();
        activeObtpsScanner.execute();
    }

    public void checkForActiveObtps(LocusKey locusKey) {
        if (!deviceRegistration.getFeatures().isScheduledMeetingV2Enabled()) return;
        SingleActiveObtpScanner singleActiveObtpScanner = new SingleActiveObtpScanner(locusKey);
        singleActiveObtpScanner.execute();
    }

    class SingleActiveObtpScanner extends SafeAsyncTask<Void> {

        private LocusKey locusKey;

        public SingleActiveObtpScanner(LocusKey locusKey) {
            this.locusKey = locusKey;
        }

        @Override
        public Void call() throws Exception {
            resumeActiveObtp(locusKey);
            return null;
        }
    }

    class ActiveObtpsScanner extends SafeAsyncTask<Void> {

        @Override
        public Void call() throws Exception {
            for (LocusKey locusKey : locusDataCache.getCallsWithMeetings()) {
                resumeActiveObtp(locusKey);
            }
            return null;
        }

        @Override
        protected void onSuccess(Void aVoid) throws Exception {
            super.onSuccess(aVoid);
            Ln.i("Checking for active obtps ended successfully");
            return;
        }
    }


    private class GetMeetingFromLocalCalendar extends SafeAsyncTask<CalendarMeeting> {
        private final Action<CalendarMeeting> success;
        private final String id;

        public GetMeetingFromLocalCalendar(String id, Action<CalendarMeeting> success) {
            this.success = success;
            this.id = id;
        }

        @Override
        protected void onSuccess(CalendarMeeting calendarMeeting) throws Exception {
            success.call(calendarMeeting);
        }

        @Override
        protected void onException(Exception e) throws RuntimeException {
            super.onException(e);
            Ln.e(e, "An error occurred getting calendar meetings from local calendar");
        }

        @Override
        public CalendarMeeting call() throws Exception {
            return getMeetingFromLocalCalendar(id);
        }
    }


    public void setApplicationController(ApplicationController applicationController) {
        Ln.i("setApplicationController()");
        applicationController.register(this);
    }

    @Override
    public boolean shouldStart() {
        Ln.i("App->ScheduledMeetingsService: shouldStart = true");
        return true;
    }

    @Override
    public void start() {
        Ln.i("App->ScheduledMeetingsService: start()");
        if (!bus.isRegistered(this))
            bus.register(this);
    }

    @Override
    public void stop() {
        Ln.i("App->ScheduledMeetingsService: stop()");
        if (bus.isRegistered(this))
            bus.unregister(this);
    }
}
