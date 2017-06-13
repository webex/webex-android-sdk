package com.cisco.spark.android.sync.operationqueue;

import android.content.ContentProviderOperation;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.model.ActivityReference;
import com.cisco.spark.android.model.Image;
import com.cisco.spark.android.sync.Batch;
import com.cisco.spark.android.sync.ContentDataCacheRecord;
import com.cisco.spark.android.sync.ContentManager;
import com.cisco.spark.android.sync.ConversationContentProviderQueries;
import com.cisco.spark.android.sync.ConversationContract;
import com.cisco.spark.android.sync.DisplayableFile;
import com.cisco.spark.android.sync.DisplayableFileSet;
import com.cisco.spark.android.sync.FileThumbnail;
import com.cisco.spark.android.sync.operationqueue.core.Operation;
import com.cisco.spark.android.sync.operationqueue.core.RetryPolicy;
import com.cisco.spark.android.util.Action;
import com.cisco.spark.android.util.ImageUtils;
import com.cisco.spark.android.util.MimeUtils;
import com.cisco.spark.android.util.UriUtils;
import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.OperationType;
import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncState;


/**
 * VideoThumbnailOperation <p/> This operation generates a thumbnail from a uri or local file using
 * logic in the ContentManager. If the file is remote it will be downloaded.
 */
public class VideoThumbnailOperation extends Operation {

    @Inject
    transient Gson gson;

    @Inject
    transient ContentManager contentManager;

    private String activityId;

    private static final int MAX_FILE_SIZE_TO_DOWNLOAD = 1024 * 1024 * 100; //100megs

    public VideoThumbnailOperation(Injector injector, String activityId) {
        super(injector);
        this.activityId = activityId;
    }

    @NonNull
    @Override
    public OperationType getOperationType() {
        return OperationType.VIDEO_THUMBNAIL;
    }

    @NonNull
    @Override
    protected SyncState onEnqueue() {
        return SyncState.READY;
    }

    @NonNull
    @Override
    protected SyncState doWork() throws IOException {

        try {
            DisplayableFileSet fileSet = getDisplayableFileSet();

            for (DisplayableFile f : fileSet.getItems()) {
                if (!f.isVideo())
                    continue;

                if (f.hasThumbnail())
                    continue;

                if (f.getFilesize() > MAX_FILE_SIZE_TO_DOWNLOAD)
                    continue;

                contentManager.getCacheRecord(ConversationContract.ContentDataCacheEntry.Cache.MEDIA,
                        f.getUrl(), f.getSecureContentReference(), f.getName(), new UpdateActivityAction());
            }
        } catch (Exception e) {
            setErrorMessage("Failed parsing message");
            return SyncState.FAULTED;
        }
        return checkProgress();
    }

    class UpdateActivityAction extends Action<ContentDataCacheRecord> {

        @Override
        public void call(ContentDataCacheRecord item) {
            if (item == null) {
                return;
            }

            synchronized (VideoThumbnailOperation.this) {
                DisplayableFileSet fileSet = getDisplayableFileSet();
                for (DisplayableFile f : fileSet.getItems()) {
                    if (UriUtils.equals(f.getUrl(), item.getRemoteUri())) {
                        if (item.getLocalUriAsFile() == null || !item.getLocalUriAsFile().exists())
                            break;

                        if (item.getLocalUriAsFile() == null || !item.getLocalUriAsFile().exists())
                            break;

                        File thumbDir = contentManager.getContentDirectory(ConversationContract.ContentDataCacheEntry.Cache.THUMBNAIL, f.getUrl());
                        File thumbFile = new File(thumbDir, f.getName() + ".png");
                        Image newThumb = ImageUtils.getThumbnailTempFile(thumbFile, item.getLocalUri(), MimeUtils.ContentType.VIDEO);
                        f.setThumbnail(FileThumbnail.from(newThumb));
                        writeActivityData(fileSet);

                        break;
                    }
                }
            }
        }
    }

    @Override
    public boolean isOperationRedundant(Operation newOperation) {
        if (newOperation.getOperationType() != getOperationType())
            return false;

        VideoThumbnailOperation newOp = (VideoThumbnailOperation) newOperation;

        if (!TextUtils.equals(newOp.activityId, activityId))
            return false;

        return true;
    }

    @Override
    protected void onNewOperationEnqueued(Operation newOperation) {
    }

    @Override
    protected SyncState checkProgress() {
        if (hasWorkRemaining())
            return SyncState.EXECUTING;

        return SyncState.SUCCEEDED;
    }

    protected DisplayableFileSet getDisplayableFileSet() {
        ActivityReference ar = ConversationContentProviderQueries.getActivityReference(getContentResolver(), activityId);
        if (ar == null)
            return null;

        return (DisplayableFileSet) gson.fromJson(ar.getData(), ar.getType().getSyncClass());
    }

    protected void writeActivityData(DisplayableFileSet fileSet) {
        Batch batch = newBatch();
        ContentProviderOperation cpo = ContentProviderOperation
                .newUpdate(ConversationContract.ActivityEntry.CONTENT_URI)
                .withValue(ConversationContract.ActivityEntry.ACTIVITY_DATA.name(), gson.toJson(fileSet))
                .withSelection(ConversationContract.ActivityEntry.ACTIVITY_ID.name() + "=?", new String[]{activityId})
                .build();
        batch.add(cpo);
        batch.apply();
    }

    protected boolean hasWorkRemaining() {
        DisplayableFileSet fileSet = getDisplayableFileSet();
        for (DisplayableFile file : fileSet.getItems()) {
            if (!file.isVideo())
                continue;

            if (file.hasThumbnail())
                continue;

            if (file.getFilesize() <= MAX_FILE_SIZE_TO_DOWNLOAD)
                return true;
        }
        return false;
    }

    @NonNull
    @Override
    public RetryPolicy buildRetryPolicy() {
        return RetryPolicy.newJobTimeoutPolicy(2, TimeUnit.MINUTES)
                .withMaxAttempts(3)
                .withRetryDelay(0)
                .withAttemptTimeout(30, TimeUnit.SECONDS);
    }
}
