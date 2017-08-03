package com.cisco.spark.android.sync.operationqueue;

import android.support.annotation.NonNull;

import com.cisco.spark.android.client.AvatarClient;
import com.cisco.spark.android.client.AvatarUrl;
import com.cisco.spark.android.client.EmptyBody;
import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.metrics.MetricsReporter;
import com.cisco.spark.android.metrics.SegmentService;
import com.cisco.spark.android.metrics.model.GenericMetric;
import com.cisco.spark.android.metrics.value.ClientMetricField;
import com.cisco.spark.android.metrics.value.ClientMetricNames;
import com.cisco.spark.android.model.AvatarSession;
import com.cisco.spark.android.sync.ContentManager;
import com.cisco.spark.android.sync.operationqueue.core.Operation;
import com.cisco.spark.android.sync.operationqueue.core.RetryPolicy;
import com.cisco.spark.android.util.MimeUtils;
import com.github.benoitdion.ln.Ln;
import com.segment.analytics.Properties;

import java.io.File;

import javax.inject.Inject;

import okhttp3.RequestBody;
import retrofit2.Response;

import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.OperationType;
import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncState;

/**
 * Operation for updating one's own avatar. The avatar is updated in the local cache as part of
 * enqueueing the operation. Uploading the file to the avatar service happens asynchronously when
 * doWork is called.
 */
public class AvatarUpdateOperation extends Operation {

    @Inject
    transient ApiClientProvider apiClientProvider;

    @Inject
    transient MetricsReporter metricsReporter;

    @Inject
    transient ContentManager contentManager;

    @Inject
    transient SegmentService segmentService;

    private File file;

    public AvatarUpdateOperation(Injector injector, File file) {
        super(injector);
        this.file = file;
    }

    @NonNull
    @Override
    public OperationType getOperationType() {
        return OperationType.SET_AVATAR;
    }

    @NonNull
    @Override
    protected SyncState onEnqueue() {
        contentManager.setSelfAvatar(file);
        return SyncState.READY;
    }

    @NonNull
    @Override
    protected SyncState doWork() {
        Ln.d("doWork " + this);
        // Create an upload session and get an upload URL
        AvatarClient avatarClient = apiClientProvider.getAvatarClient();
        try {
            Response<AvatarSession> sessionResponse = avatarClient.createUploadSession(new EmptyBody()).execute();
            AvatarSession avatarSession = sessionResponse.body();

            // Upload the content to that URL
            RequestBody requestBody = RequestBody.create(MimeUtils.getMediaType(file.getAbsolutePath()), file);
            avatarClient.uploadFile(avatarSession.getUrl().toString(), requestBody).execute();

            // Update session with completed upload
            Response<AvatarUrl> avatarUriResponse = avatarClient.updateUploadSession(avatarSession.getId(), new AvatarUrl(avatarSession.getUrl())).execute();

            GenericMetric metric = GenericMetric.buildBehavioralMetric(ClientMetricNames.ONBOARDING_UPLOADED_AVATAR)
                    .withNetworkTraits(avatarUriResponse);
            addFileTraits(metric, file);
            operationQueue.postGenericMetric(metric);

            Properties segmentMetricProperties = new SegmentService.PropertiesBuilder()
                    .setNetworkResponse(avatarUriResponse)
                    .setFileTraits(file).build();
            segmentService.reportMetric(SegmentService.ONBOARDING_UPLOADED_AVATAR, segmentMetricProperties);

            if (avatarUriResponse.isSuccessful()) {
                AvatarUrl avatarUri = avatarUriResponse.body();
                if (avatarUri != null) {
                    return SyncState.SUCCEEDED;
                }
            }
        } catch (Throwable e) {
            ln.w(e, "Failed creating avatar upload session");
        }
        return SyncState.READY;
    }

    private void addFileTraits(GenericMetric metric, File file) {
        if (file != null) {
            metric.addField(ClientMetricField.METRIC_FIELD_MIME_TYPE, MimeUtils.getMimeType(file.getAbsolutePath()));
            metric.addField(ClientMetricField.METRIC_FIELD_FILE_SIZE, file.length());
        }
    }

    @Override
    protected SyncState checkProgress() {
        return getState();
    }

    @Override
    protected void onNewOperationEnqueued(Operation newOperation) {
        // Only one avatar upload task in the queue at a time
        if (newOperation.getOperationType() == OperationType.SET_AVATAR) {
            Ln.d("Canceling because a newer avatar operation was just posted. " + this);
            cancel();
        }
    }

    @NonNull
    @Override
    public RetryPolicy buildRetryPolicy() {
        return RetryPolicy.newLimitAttemptsPolicy()
                .withExponentialBackoff();
    }
}
