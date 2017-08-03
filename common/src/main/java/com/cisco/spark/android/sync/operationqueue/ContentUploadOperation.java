package com.cisco.spark.android.sync.operationqueue;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.cisco.crypto.scr.SecureContentReference;
import com.cisco.crypto.scr.TypedSecureContentReference;
import com.cisco.spark.android.client.CountedTypedOutput;
import com.cisco.spark.android.client.WebExFilesClient;
import com.cisco.spark.android.content.ContentLoader;
import com.cisco.spark.android.content.ContentUploadMonitor;
import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.metrics.MetricsReporter;
import com.cisco.spark.android.metrics.TimingProvider;
import com.cisco.spark.android.metrics.model.GenericMetric;
import com.cisco.spark.android.metrics.value.ClientMetricField;
import com.cisco.spark.android.metrics.value.ClientMetricNames;
import com.cisco.spark.android.metrics.value.ClientMetricTag;
import com.cisco.spark.android.metrics.value.EncryptionMetrics;
import com.cisco.spark.android.metrics.value.GenericMetricTagEnums;
import com.cisco.spark.android.model.CompleteContentUploadSession;
import com.cisco.spark.android.model.ContentUploadSession;
import com.cisco.spark.android.model.File;
import com.cisco.spark.android.model.Image;
import com.cisco.spark.android.sync.ContentManager;
import com.cisco.spark.android.sync.ConversationContentProviderQueries;
import com.cisco.spark.android.sync.ConversationContract;
import com.cisco.spark.android.sync.ConversationContract.ContentDataCacheEntry.Cache;
import com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.OperationType;
import com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncState;
import com.cisco.spark.android.sync.operationqueue.core.Operation;
import com.cisco.spark.android.sync.operationqueue.core.OperationQueue;
import com.cisco.spark.android.sync.operationqueue.core.RetryPolicy;
import com.cisco.spark.android.util.Metadata;
import com.cisco.spark.android.util.MimeUtils;
import com.cisco.spark.android.util.Strings;
import com.cisco.spark.android.util.UriUtils;
import com.github.benoitdion.ln.Ln;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import retrofit2.Response;

public class ContentUploadOperation extends Operation implements ConversationOperation {

    protected Uri spaceUrl;
    protected Uri spaceUrlHidden;
    protected String conversationId;
    protected List<File> files;
    List<Image> thumbnails;
    private boolean contentAttached;

    @Inject
    transient ApiClientProvider apiClientProvider;

    @Inject
    transient ContentLoader contentLoader;

    @Inject
    transient ContentManager contentManager;

    @Inject
    transient OperationQueue operationQueue;


    @Inject
    transient MetricsReporter metricsReporter;

    @Inject
    transient ContentUploadMonitor uploadMonitor;

    @Inject
    transient TimingProvider timingProvider;

    public ContentUploadOperation(Injector injector, String conversationId, File content) {
        this(injector, conversationId, new ArrayList<File>());
        files.add(content);
    }

    public ContentUploadOperation(Injector injector, String conversationId, List<File> content) {
        super(injector);
        this.conversationId = conversationId;
        this.files = content;
        this.thumbnails = new ArrayList<>();
    }

    public ContentUploadOperation(Injector injector, Uri spaceUrl, File content) {
        this(injector, spaceUrl, new ArrayList<File>());
        files.add(content);
    }

    public ContentUploadOperation(Injector injector, Uri spaceUrl, List<File> content) {
        super(injector);
        this.spaceUrl = spaceUrl;
        this.spaceUrlHidden = spaceUrl;
        this.files = content;
        this.thumbnails = new ArrayList<>();
    }

    public String getConversationId() {
        return conversationId;
    }

    public List<File> getFiles() {
        return files;
    }

    public boolean isContentAttached() {
        return this.contentAttached;
    }

    public void setContentAttached(boolean contentAttached) {
        this.contentAttached = contentAttached;
    }

    @NonNull
    @Override
    protected SyncState doWork() throws IOException {
        try {
            if (spaceUrl == null || spaceUrlHidden == null) {

                conversationId = ConversationContentProviderQueries.getOneValue(
                        getContentResolver(),
                        Uri.withAppendedPath(ConversationContract.ConversationEntry.CONTENT_URI, conversationId),
                        ConversationContract.ConversationEntry.CONVERSATION_ID.name(),
                        null, null);

                if (!Strings.isEmpty(conversationId)) {
                    loadConversationData(conversationId);
                }
            }

            // Since images and content can be shared together, we need to keep a separate counter for the number of images
            for (int i = 0; i < files.size(); i++) {
                File contentFile = files.get(i);
                timingProvider.get(contentFile.getUrlOrSecureLocation().toString());

                GenericMetric metric = GenericMetric.buildOperationalMetric(ClientMetricNames.CLIENT_POST_CONTENT);
                GenericMetric thumbnailMetric = GenericMetric.buildOperationalMetric(ClientMetricNames.CLIENT_POST_CONTENT);
                metric.addTag(ClientMetricTag.METRIC_TAG_DATA_TYPE, GenericMetricTagEnums.DataTypeMetricTagValue.DATA_TYPE_FILE);
                thumbnailMetric.addTag(ClientMetricTag.METRIC_TAG_DATA_TYPE, GenericMetricTagEnums.DataTypeMetricTagValue.DATA_TYPE_THUMBNAIL);

                Ln.d("uploading " + contentFile);

                // If the file is local, we need to perform the upload, otherwise it was already successfully uploaded
                if (contentFile.getUrl().getScheme().equals(ContentResolver.SCHEME_FILE)) {
                    Image thumbnailImage = contentFile.getImage();
                    File thumbnailFileModel = null;
                    // Upload Thumbnail
                    if (thumbnailImage != null && thumbnailImage.getUrl().getScheme().equals(ContentResolver.SCHEME_FILE)) {

                        java.io.File thumbnailFile = new java.io.File(new URI(thumbnailImage.getUrl().toString()));
                        thumbnailFileModel = uploadFile(thumbnailFile, true, thumbnailMetric);
                        thumbnailMetric.addField(ClientMetricField.METRIC_FIELD_CONTENT_SIZE,  thumbnailFile.length());
                        operationQueue.postGenericMetric(thumbnailMetric);

                        if (thumbnailFileModel == null) {
                            Ln.i("Failed to upload thumbnail");
                            return SyncState.READY;
                        }

                        contentManager.addUploadedContent(thumbnailFile, thumbnailFileModel.getUrl(), Cache.THUMBNAIL);
                        thumbnailImage.copyFromFile(thumbnailFileModel);
                    }

                    // Upload File
                    java.io.File fileToUpload = new java.io.File(new URI(contentFile.getUrlOrSecureLocation().toString()));

                    File uploadedFile = uploadFile(fileToUpload, contentFile.isHidden(), metric);
                    metric.addField(ClientMetricField.METRIC_FIELD_CONTENT_SIZE, fileToUpload.length());
                    metric.addField(ClientMetricField.METRIC_FIELD_PERCIEVED_DURATION, timingProvider.get(contentFile.getUrlOrSecureLocation().toString()));

                    if (uploadedFile == null || uploadedFile.getUrl() == null) {
                        Ln.i("Failed to upload file");
                        return SyncState.READY;
                    }

                    uploadedFile.setDisplayName(contentFile.getUrlOrSecureLocation().getLastPathSegment());
                    uploadedFile.setImage(thumbnailImage);
                    uploadedFile.setMimeType(MimeUtils.getMimeType(contentFile.getUrl().toString()));
                    uploadedFile.setActions(contentFile.getActions());
                    files.set(i, uploadedFile);

                    contentManager.addUploadedContent(fileToUpload, uploadedFile.getUrl(), Cache.MEDIA);

                    Ln.v("Uploaded file " + contentFile.getUrl());
                    metric.addField(ClientMetricField.METRIC_FIELD_PERCIEVED_DURATION, timingProvider.get(contentFile.getUrlOrSecureLocation().toString()).finish());
                    operationQueue.postGenericMetric(metric);
                }
            }
        } catch (Throwable e) {
            setErrorMessage("Failed to upload file: " + e.getMessage());
            Ln.e(e, "Failed uploading file.");
            return SyncState.READY;
        }

        return SyncState.SUCCEEDED;
    }

    private void loadConversationData(String conversationId) {
        Bundle bundle = ConversationContentProviderQueries.getConversationById(getContentResolver(), conversationId);
        if (bundle != null) {
            spaceUrl = UriUtils.parseIfNotNull(bundle.getString(ConversationContract.ConversationEntry.SPACE_URL.name()));
            spaceUrlHidden = UriUtils.parseIfNotNull(bundle.getString(ConversationContract.ConversationEntry.SPACE_URL_HIDDEN.name()));
        }
    }

    //
    // Uploads a file. This creates Files space and then uploads the file.
    //
    private File uploadFile(final java.io.File file, boolean hidden, GenericMetric metric) {
        boolean isImage = MimeUtils.isImageExt(MimeUtils.getExtension(file.getName()));
        boolean isThumbnail = contentManager.isThumbnailFile(file);
        Uri spaceUri = hidden ? spaceUrlHidden : spaceUrl;

        File wx2file = null;
        try {
            long startTime = System.currentTimeMillis();

            wx2file = uploadFileToSwift(spaceUri, file, metric);
            if (wx2file != null) {
                EncryptionMetrics.ContentMetric encryptionMetric = new EncryptionMetrics.ContentMetric(
                        EncryptionMetrics.ContentMetric.Direction.up,
                        file.length(),
                        System.currentTimeMillis() - startTime,
                        isThumbnail
                                ? EncryptionMetrics.ContentMetric.Kind.thumbnail
                                : EncryptionMetrics.ContentMetric.Kind.fromFilename(file.getName()));

                metricsReporter.enqueueMetricsReport(metricsReporter.newEncryptionSplunkMetricsBuilder().reportValue(encryptionMetric).build());

                Ln.d("File uploaded successfully. file:'%s'", file.toString());
            }
        } catch (IOException e) {
            Ln.e(e, "Failed uploading file to space " + spaceUri);
            setErrorMessage("Failed to upload file: network error");
        } catch (Exception e) {
            Ln.e(e, "Failed uploading file to space " + spaceUri);
            setErrorMessage("Failed to upload file: " + e.getMessage());
        }
        return wx2file;
    }

    public com.cisco.spark.android.model.File uploadFileToSwift(Uri spaceUri, java.io.File file, GenericMetric metric) throws IOException {
        WebExFilesClient filesClient = apiClientProvider.getFilesClient();
        Uri uploadSessionUri = spaceUri.buildUpon().appendPath("upload_sessions").build();

        // Sanitize the file's metadata before uploading
        Metadata metadata = new Metadata(file);
        metadata.sanitize();

        // Upload to Files
        String mimeType = MimeUtils.getMimeType(file.getAbsolutePath());
        if (mimeType == null) {
            mimeType = "application/unknown";
        }

        CountedTypedOutput typedOutput = new TypedSecureContentReference(mimeType, file, timingProvider);
        uploadMonitor.addProgressProvider(Uri.fromFile(file).toString(), typedOutput);


        timingProvider.get(Uri.fromFile(file).toString()).start();
        Response<ContentUploadSession> sessionResponse = filesClient.createUploadSession(uploadSessionUri.toString()).execute();
        metric.addField(ClientMetricField.METRIC_FIELD_SERVER_INTERACTION_DURATION, timingProvider.get(Uri.fromFile(file).toString()).finish());

        if (!sessionResponse.isSuccessful()) {
            setErrorMessage(String.format(Locale.US, "Failed to create upload session: %s", sessionResponse.message()));
            metric.addNetworkStatus(sessionResponse);
            return null;
        }

        ContentUploadSession session = sessionResponse.body();

        timingProvider.get(String.format(Locale.getDefault(), "%d_first", file.hashCode())).start();
        Response<Void> uploadResponse = filesClient.uploadFile(session.getUploadUrl().toString(), typedOutput).execute();
        if (!uploadResponse.isSuccessful()) {
            setErrorMessage(String.format(Locale.US, "Failed to upload file: %s", uploadResponse.message()));
            metric.addNetworkStatus(uploadResponse);
            return null;
        }

        metric.addField(ClientMetricField.METRIC_FIELD_STORAGE_TRADCKING_ID, uploadResponse.raw().header("X-Trans-Id"));
        metric.addField(ClientMetricField.METRIC_FIELD_DURATION_TO_FIRST_RECORD, timingProvider.get(String.format(Locale.getDefault(), "%d_first", file.hashCode())).getTotalDuration());
        metric.addField(ClientMetricField.METRIC_FIELD_DURATION_FROM_FIRST_RECORD_TO_END, timingProvider.get(String.format(Locale.getDefault(), "%d_total", file.hashCode())).getTotalDuration());

        Response<File> updateSessionResponse = filesClient.updateUploadSession(session.getFinishUploadUrl().toString(), new CompleteContentUploadSession((int) file.length())).execute();
        metric.addNetworkStatus(updateSessionResponse);

        if (!updateSessionResponse.isSuccessful()) {
            setErrorMessage(String.format(Locale.US, "Failed to update upload session: %s", updateSessionResponse.message()));
            return null;
        }

        File fileReference = updateSessionResponse.body();
        fileReference.setUri(fileReference.getUrl().buildUpon()
                .appendPath("versions")
                .appendPath(fileReference.getVersion())
                .appendPath("bytes").build());

        SecureContentReference scr = ((TypedSecureContentReference) typedOutput).getSecureContentReference();
        scr.setLoc(fileReference.getUrl().toString());
        fileReference.setSecureContentReference(scr);

        fileReference.setFileSize(file.length());
        return fileReference;
    }

    @NonNull
    @Override
    public OperationType getOperationType() {
        return OperationType.CONTENT_UPLOAD;
    }

    @NonNull
    @Override
    protected SyncState onEnqueue() {
        fetchSpaceUrlIfNeeded();
        return SyncState.READY;
    }

    @NonNull
    @Override
    protected SyncState onRestart() {
        fetchSpaceUrlIfNeeded();
        return SyncState.READY;
    }

    @Override
    protected void onNewOperationEnqueued(Operation newOperation) {

    }

    @Override
    protected SyncState checkProgress() {
        return getState();
    }

    @Override
    @SuppressWarnings("MissingSuperCall")
    public boolean isSafeToRemove() {
        return super.isSafeToRemove() && (isContentAttached() || isCanceled());
    }

    @NonNull
    @Override
    public RetryPolicy buildRetryPolicy() {
        return RetryPolicy.newJobTimeoutPolicy(10, TimeUnit.MINUTES)
                .withMaxAttempts(3)
                .withRetryDelay(TimeUnit.SECONDS.toMillis(5));
    }

    private void fetchSpaceUrlIfNeeded() {
        if (!Strings.isEmpty(conversationId)) {
            loadConversationData(conversationId);
        }

        if (spaceUrl == null || spaceUrlHidden == null) {
            setDependsOn(operationQueue.fetchSpaceUrl(getConversationId()));
        }
    }
}
