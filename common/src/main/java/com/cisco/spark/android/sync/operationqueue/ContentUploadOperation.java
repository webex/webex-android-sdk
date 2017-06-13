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
import com.cisco.spark.android.metrics.value.EncryptionMetrics;
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
                Ln.d("uploading " + contentFile);

                // If the file is local, we need to perform the upload, otherwise it was already successfully uploaded
                if (contentFile.getUrl().getScheme().equals(ContentResolver.SCHEME_FILE)) {
                    Image thumbnailImage = contentFile.getImage();
                    File thumbnailFileModel = null;
                    // Upload Thumbnail
                    if (thumbnailImage != null && thumbnailImage.getUrl().getScheme().equals(ContentResolver.SCHEME_FILE)) {
                        java.io.File thumbnailFile = new java.io.File(new URI(thumbnailImage.getUrl().toString()));
                        thumbnailFileModel = uploadFile(thumbnailFile, true);
                        if (thumbnailFileModel == null) {
                            Ln.i("Failed to upload thumbnail");
                            return SyncState.READY;
                        }

                        contentManager.addUploadedContent(thumbnailFile, thumbnailFileModel.getUrl(), Cache.THUMBNAIL);
                        thumbnailImage.copyFromFile(thumbnailFileModel);
                    }

                    // Upload File
                    java.io.File fileToUpload = new java.io.File(new URI(contentFile.getUrlOrSecureLocation().toString()));

                    File uploadedFile = uploadFile(fileToUpload, contentFile.isHidden());
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
    private File uploadFile(final java.io.File file, boolean hidden) {
        boolean isImage = MimeUtils.isImageExt(MimeUtils.getExtension(file.getName()));
        boolean isThumbnail = contentManager.isThumbnailFile(file);
        Uri spaceUri = hidden ? spaceUrlHidden : spaceUrl;

        File wx2file = null;
        try {
            long startTime = System.currentTimeMillis();

            wx2file = uploadFileToSwift(spaceUri, file);
            if (wx2file != null) {
                EncryptionMetrics.ContentMetric metric = new EncryptionMetrics.ContentMetric(
                        EncryptionMetrics.ContentMetric.Direction.up,
                        file.length(),
                        System.currentTimeMillis() - startTime,
                        isThumbnail
                                ? EncryptionMetrics.ContentMetric.Kind.thumbnail
                                : EncryptionMetrics.ContentMetric.Kind.fromFilename(file.getName()));

                metricsReporter.enqueueMetricsReport(metricsReporter.newEncryptionSplunkMetricsBuilder().reportValue(metric).build());

                Ln.d("File uploaded successfully. file:'%s'", file.toString());

                contentLoader.reportMetrics(true, true, isThumbnail, isImage, true);
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

    public com.cisco.spark.android.model.File uploadFileToSwift(Uri spaceUri, java.io.File file) throws IOException {
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

        CountedTypedOutput typedOutput = new TypedSecureContentReference(mimeType, file);
        uploadMonitor.addProgressProvider(Uri.fromFile(file).toString(), typedOutput);

        Response<ContentUploadSession> sessionResponse = filesClient.createUploadSession(uploadSessionUri.toString()).execute();

        if (!sessionResponse.isSuccessful()) {
            setErrorMessage(String.format(Locale.US, "Failed to create upload session: %s", sessionResponse.message()));
            return null;
        }

        ContentUploadSession session = sessionResponse.body();

        filesClient.uploadFile(session.getUploadUrl().toString(), typedOutput).execute();

        Response<File> updateSessionResponse = filesClient.updateUploadSession(session.getFinishUploadUrl().toString(), new CompleteContentUploadSession((int) file.length())).execute();
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
