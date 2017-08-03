package com.cisco.spark.android.log;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.Nullable;

import com.cisco.spark.android.authenticator.ApiTokenProvider;
import com.cisco.spark.android.callcontrol.model.Call;
import com.cisco.spark.android.client.AdminClient;
import com.cisco.spark.android.client.AvatarClient;
import com.cisco.spark.android.client.TrackingIdGenerator;
import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.Settings;
import com.cisco.spark.android.locus.model.Locus;
import com.cisco.spark.android.media.MediaEngine;
import com.cisco.spark.android.model.LogMetadataRequest;
import com.cisco.spark.android.provisioning.ProvisioningClient;
import com.cisco.spark.android.provisioning.ProvisioningClientProvider;
import com.cisco.spark.android.room.RoomService;
import com.cisco.spark.android.sdk.SdkClient;
import com.cisco.spark.android.util.DateUtils;
import com.cisco.spark.android.util.DiagnosticManager;
import com.cisco.spark.android.util.LoggingUtils;
import com.cisco.spark.android.util.SafeAsyncTask;
import com.cisco.spark.android.util.SystemUtils;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.github.benoitdion.ln.Ln;

import java.io.File;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import de.greenrobot.event.EventBus;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Response;

public class UploadLogsService {

    private Context context;
    private ApiClientProvider apiClientProvider;
    private ProvisioningClientProvider provisioningClientProvider;
    private ApiTokenProvider apiTokenProvider;
    private MediaEngine mediaEngine;
    private LogFilePrint logFilePrint;
    private Settings settings;
    private TrackingIdGenerator trackingIdGenerator;
    private DeviceRegistration deviceRegistration;
    private DiagnosticManager diagnosticManager;
    private EventBus eventBus;
    private RoomService roomService;

    private String lastFeedbackId = null;
    private Calendar lastFeedbackTime = null;
    private SdkClient sdkClient;

    public UploadLogsService(Context context, ApiClientProvider apiClientProvider,
                             ApiTokenProvider apiTokenProvider, MediaEngine mediaEngine,
                             LogFilePrint logFilePrint, Settings settings,
                             TrackingIdGenerator trackingIdGenerator, DeviceRegistration deviceRegistration,
                             DiagnosticManager diagnosticManager, EventBus eventBus, RoomService roomService,
                             ProvisioningClientProvider provisioningClientProvider, SdkClient sdkClient) {
        this.context = context;
        this.apiClientProvider = apiClientProvider;
        this.apiTokenProvider = apiTokenProvider;
        this.mediaEngine = mediaEngine;
        this.logFilePrint = logFilePrint;
        this.settings = settings;
        this.trackingIdGenerator = trackingIdGenerator;
        this.deviceRegistration = deviceRegistration;
        this.diagnosticManager = diagnosticManager;
        this.eventBus = eventBus;
        this.roomService = roomService;
        this.provisioningClientProvider = provisioningClientProvider;
        this.sdkClient = sdkClient;
    }

    @Nullable
    public String getLastFeedbackId() {
        return lastFeedbackId;
    }

    @Nullable
    public Calendar getLastFeedbackTime() {
        return lastFeedbackTime;
    }

    public void uploadLogs(Call call) {
        new AdminUploadLogsAsyncTask(call).execute();
    }

    public void uploadLogs(String feedbackId) {
        new AdminUploadLogsAsyncTask(feedbackId).execute();
    }

    public void uploadLogsWithEmail(String email) {
        new ProvisionUploadLogsAsyncTask(email).execute();
    }

    public void uploadRoomLogsIfConnected(String feedbackId) {
        if (roomService.isInRoom()) {
            Ln.i("Request connected system to upload logs using FeedbackId = %s", feedbackId);
            roomService.uploadRoomLogs(feedbackId);
        }
    }

    protected Uri generateLogFiles() {
        // include sysinfo.txt in our reported logs
        generateSysInfoFile();
        return logFilePrint.generateZippedLogs(true);
    }

    protected void generateSysInfoFile() {
        SystemUtils.generateSysInfoFile(logFilePrint.getLogDirectory(), context, apiTokenProvider, mediaEngine,
                                        settings, null, deviceRegistration, trackingIdGenerator,
                                        diagnosticManager, sdkClient);
    }


    class AdminUploadLogsAsyncTask extends SafeAsyncTask<LogMetadataRequest> {

        private String feedbackId;
        private Call call;

        AdminUploadLogsAsyncTask(String feedbackId) {
            this.feedbackId = feedbackId;
        }

        AdminUploadLogsAsyncTask(Call call) {
            this.call = call;
        }

        @Override
        public LogMetadataRequest call() throws Exception {
            AdminClient adminClient = apiClientProvider.getAdminClient();
            final Uri logFileUri = generateLogFiles();
            DateFormat dfmt = DateUtils.buildIso8601Format();
            // This is the name we want the file to have on the server
            String serverFileName;
            LogMetadataRequest metadataRequest;

            if (call != null && call.getLocusData() != null) {
                Ln.i("Upload logs using locusData");
                Locus locus = call.getLocusData().getLocus();
                String timestamp = dfmt.format(locus.getFullState().getLastActive());
                serverFileName = locus.getKey().getLocusId() + '_' + timestamp + ".zip";
                metadataRequest = new LogMetadataRequest(serverFileName);
                metadataRequest.addData("locusId", locus.getKey().getLocusId());
                metadataRequest.addData("callStart", timestamp);
                if (roomService.isStarted() && roomService.getCallController().wasPreviousCallConnected()) {
                    roomService.uploadRoomLogs(call.getLocusData().getKey(), timestamp);
                }
            } else if (feedbackId != null) {
                Ln.i("Upload logs using feedbackId: %s", feedbackId);
                serverFileName = feedbackId + '_' + dfmt.format(new Date()) + ".zip";
                metadataRequest = new LogMetadataRequest(serverFileName);
                metadataRequest.addData("feedbackId", feedbackId);

                // log locus id/start time for last call that took place (if any)
                LogCallIndexEntry lastLogCallIndexEntry = logFilePrint.getCallIndex().getLastCallIndexEntry();
                if (lastLogCallIndexEntry != null) {
                    metadataRequest.addData("locusId", lastLogCallIndexEntry.getLocusId());
                    metadataRequest.addData("callStart", lastLogCallIndexEntry.getLastActive());
                }

                uploadRoomLogsIfConnected(feedbackId);
            } else {
                serverFileName = dfmt.format(new Date()) + ".zip";
                metadataRequest = null;
            }
            // first, upload the log archive to the admin server
            AdminClient.LogUploadRequest req = new AdminClient.LogUploadRequest(serverFileName);
            Response<AdminClient.LogURLResponse> response = adminClient.getUploadFileUrl(req).execute();
            if (!response.isSuccessful()) {
                Ln.e("Upload logs failed: " + LoggingUtils.toString(response));
            }
            Ln.i("got log upload response: " + response.body().getTempURL());
            AvatarClient uploadClient = apiClientProvider.getAvatarClient();
            Uri url = response.body().getTempURL();
            File logFile = new File(new java.net.URI(logFileUri.toString()));
            long start = System.currentTimeMillis();
            uploadClient.uploadFile(url.toString(), RequestBody.create(MediaType.parse("application/zip"), logFile)).execute();
            // TODO Check whether this succeeded or not
            long duration = System.currentTimeMillis() - start;
            Ln.i("uploaded post-call logs, size=" + (logFile.length() / 1000)
                    + " kb, duration=" + duration + " ms. ");
            // associate the log file name with appropriate metadata in the admin server
            if (metadataRequest != null) {
                start = System.currentTimeMillis();
                adminClient.setLogMetadata(metadataRequest).execute();
                duration = System.currentTimeMillis() - start;
                Ln.i("associated metadata with log file, server call took " + duration + " ms.");
            }
            return metadataRequest;
        }

        @Override
        protected void onSuccess(LogMetadataRequest o) throws Exception {
            lastFeedbackId = feedbackId;
            lastFeedbackTime = Calendar.getInstance();
            eventBus.post(new LogsUploadedEvent(feedbackId, call));
        }

        @Override
        protected void onThrowable(Throwable t) throws RuntimeException {
            eventBus.post(new LogsUploadedEvent(feedbackId, call, t));
            Ln.e(t, "Upload logs failed");
        }
    }

    class ProvisionUploadLogsAsyncTask extends SafeAsyncTask {
        private String email;
        private String feedbackId;

        ProvisionUploadLogsAsyncTask(String email) {
            this.email = email;
            this.feedbackId = String.valueOf(UUID.randomUUID());
        }

        @Override
        public Object call() throws Exception {
            ProvisioningClient provisioningClient = provisioningClientProvider.getProvisioningClient();
            final Uri logFileUri = generateLogFiles();
            DateFormat dfmt = DateUtils.buildIso8601Format();

            // This is the name we want the file to have on the server
            String serverFileName;
            LogMetadataRequest metadataRequest;

            Ln.i("Upload logs using feedbackId: %s", feedbackId);
            serverFileName = feedbackId + '_' + dfmt.format(new Date()) + ".zip";
            metadataRequest = new LogMetadataRequest(serverFileName);
            metadataRequest.addData("feedbackId", feedbackId);
            metadataRequest.addData("email", email);

            // first, upload the log archive to the admin server
            AdminClient.LogUploadRequest req = new AdminClient.LogUploadRequest(serverFileName);
            Response<AdminClient.LogURLResponse> logURLResponse = provisioningClient.getUploadFileUrl(req).execute();

            if (logURLResponse.isSuccessful()) {
                Ln.i("got log upload response: " + logURLResponse.body().getTempURL());
                AvatarClient uploadClient = apiClientProvider.getAvatarClient();
                Uri uri = logURLResponse.body().getTempURL();
                File logFile = new File(new java.net.URI(logFileUri.toString()));
                long start = System.currentTimeMillis();

                Response<ResponseBody> uploadResponse = uploadClient.uploadFile(uri.toString(), RequestBody.create(MediaType.parse("application/zip"), logFile)).execute();
                if (!uploadResponse.isSuccessful()) {
                    // TODO Throw an exception here?
                    return null;
                }

                long duration = System.currentTimeMillis() - start;
                Ln.i("uploaded post-call logs, size=" + (logFile.length() / 1000)
                        + " kb, duration=" + duration + " ms. ");
                // associate the log file name with appropriate metadata in the admin server
                if (metadataRequest != null) {
                    String userId = logURLResponse.body().getUserId();
                    metadataRequest.setUserId(userId);
                    start = System.currentTimeMillis();
                    provisioningClient.setLogMetadata(metadataRequest);
                    duration = System.currentTimeMillis() - start;
                    Ln.i("associated metadata with log file, server call took " + duration + " ms.");
                }

                return metadataRequest;
            }

            return null;
        }

        @Override
        protected void onSuccess(Object o) throws Exception {
            lastFeedbackId = feedbackId;
            lastFeedbackTime = Calendar.getInstance();
            eventBus.post(new LogsUploadedEvent(feedbackId, null));
        }

        @Override
        protected void onThrowable(Throwable t) throws RuntimeException {
            eventBus.post(new LogsUploadedEvent(feedbackId, null, t));
            Ln.e(t, "Upload logs failed");
        }
    }

    public static class LogsUploadedEvent {
        private String feedbackId;
        private Call call;
        private Throwable uploadError;

        public boolean isSuccess() {
            return uploadError == null;
        }

        public LogsUploadedEvent(String feedbackId, Call call, Throwable uploadError) {
            this.feedbackId = feedbackId;
            this.call = call;
            this.uploadError = uploadError;
        }

        public LogsUploadedEvent(String feedbackId, Call call) {
            this(feedbackId, call, null);
        }

        public String getFeedbackId() {
            return feedbackId;
        }

        public Call getCall() {
            return call;
        }

        public Throwable getUploadError() {
            return uploadError;
        }
    }
}
