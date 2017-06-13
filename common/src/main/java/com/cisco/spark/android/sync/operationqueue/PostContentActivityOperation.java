package com.cisco.spark.android.sync.operationqueue;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.cisco.spark.android.R;
import com.cisco.spark.android.authenticator.AuthenticatedUserProvider;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.model.Activity;
import com.cisco.spark.android.model.Comment;
import com.cisco.spark.android.model.Content;
import com.cisco.spark.android.model.ContentAction;
import com.cisco.spark.android.model.ContentCategory;
import com.cisco.spark.android.model.Conversation;
import com.cisco.spark.android.model.File;
import com.cisco.spark.android.model.Image;
import com.cisco.spark.android.model.ItemCollection;
import com.cisco.spark.android.model.Verb;
import com.cisco.spark.android.reachability.NetworkReachability;
import com.cisco.spark.android.sync.ActorRecord;
import com.cisco.spark.android.sync.ContentManager;
import com.cisco.spark.android.sync.ConversationContentProviderQueries;
import com.cisco.spark.android.sync.ConversationContract;
import com.cisco.spark.android.sync.ConversationContract.ConversationEntry;
import com.cisco.spark.android.sync.EncryptedConversationProcessor;
import com.cisco.spark.android.sync.operationqueue.core.Operation;
import com.cisco.spark.android.sync.operationqueue.core.RetryPolicy;
import com.cisco.spark.android.util.ImageUtils;
import com.cisco.spark.android.util.MimeUtils;
import com.cisco.spark.android.util.Toaster;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.github.benoitdion.ln.Ln;
import com.google.gson.Gson;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import retrofit2.Response;

import static com.cisco.spark.android.sync.ConversationContract.ContentDataCacheEntry.Cache;
import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.OperationType;
import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncState;

/**
 * Operation that handles sending a message with content. As the operation is enqueued it handles
 * generating a thumbnail if possible, adding the files to the ContentManager for later GC, and
 * submitting the activity to the ActivitySyncQueue where it will be pushed into the database and
 * displayed immediately.
 * <p/>
 * Uploading the files to the space and posting the activity is an async task kicked off in {@link
 * ActivityOperation#doWork()}.
 */
public class PostContentActivityOperation extends ActivityOperation {
    @Inject
    transient Context context;

    @Inject
    transient EncryptedConversationProcessor conversationProcessor;

    @Inject
    transient Gson gson;

    @Inject
    transient NetworkReachability networkReachability;

    @Inject
    transient AuthenticatedUserProvider authenticatedUserProvider;

    @Inject
    transient DeviceRegistration deviceRegistration;

    protected Comment comment;
    protected ItemCollection<File> files;
    protected int participantCount;
    protected ShareContentData shareContentData;
    private String selfPresence;
    private String otherPresence;

    public static class ShareContentData implements Parcelable {
        private String contentCategory;
        private boolean hasTranscodeableDocument;
        private List<ContentItem> contentItems;

        public static final Parcelable.Creator<ShareContentData> CREATOR = new Parcelable.Creator<ShareContentData>() {
            public ShareContentData createFromParcel(Parcel pc) {
                return new ShareContentData(pc);
            }

            public ShareContentData[] newArray(int size) {
                return new ShareContentData[size];
            }
        };

        public ShareContentData() {
            this.contentCategory = ContentCategory.IMAGES;
            this.contentItems = new ArrayList<>();
        }

        public ShareContentData(Parcel parcel) {
            this.contentCategory = parcel.readString();
            this.hasTranscodeableDocument = parcel.readByte() == 1 ? true : false;
            this.contentItems = parcel.readArrayList(ContentItem.class.getClassLoader());
        }

        public String getContentCategory() {
            return this.contentCategory;
        }

        public boolean doesHaveTranscodeableDocuments() {
            return this.hasTranscodeableDocument;
        }

        public boolean isEmpty() {
            return this.contentItems.isEmpty();
        }

        public void addContentItem(ContentItem item) {
            this.contentItems.add(0, item);

            MimeUtils.ContentType type = MimeUtils.getContentTypeByFilename(item.file.getAbsolutePath());
            if (type != MimeUtils.ContentType.IMAGE) {
                contentCategory = ContentCategory.DOCUMENTS;
            }

            if (type.shouldTranscode()) {
                hasTranscodeableDocument = true;
            }
        }

        public void removeContentItem(int position) {
            this.contentItems.remove(position);

            this.contentCategory = ContentCategory.IMAGES;

            for (ContentItem item : this.contentItems) {
                MimeUtils.ContentType type = MimeUtils.getContentTypeByFilename(item.file.getAbsolutePath());
                if (type != MimeUtils.ContentType.IMAGE) {
                    contentCategory = ContentCategory.DOCUMENTS;
                }

                if (type.shouldTranscode()) {
                    hasTranscodeableDocument = true;
                }
            }
        }

        public List<ContentItem> getContentItems() {
            return this.contentItems;
        }

        public List<File> getContentFiles() {
            List<com.cisco.spark.android.model.File> content = new ArrayList<>();
            List<ContentItem> items = new ArrayList<>(contentItems);
            for (ContentItem item : items) {
                content.add(0, item.contentFile);
            }
            return content;
        }

        public List<String> getOperationIds() {
            List<String> operationIds = new ArrayList<>();
            List<ContentItem> items = new ArrayList<>(contentItems);
            for (ContentItem item : items) {
                operationIds.add(0, item.operationId);
            }
            return operationIds;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.contentCategory);
            dest.writeByte((byte) (hasTranscodeableDocument ? 1 : 0));
            dest.writeList(contentItems);
        }
    }

    public static class ContentItem implements Parcelable {
        String operationId;
        File contentFile;
        transient java.io.File file;
        transient Bitmap thumbnail;
        String contentShareSource;

        public ContentItem(java.io.File file, String contentShareSource) {
            this(null, null, file, contentShareSource);
        }

        public ContentItem(String operationId, File contentFile, java.io.File file, String contentShareSource) {
            this(operationId, contentFile, file, null, contentShareSource);
        }

        public ContentItem(String operationId, File contentFile, java.io.File file, Bitmap thumbnail, String contentShareSource) {
            this.operationId = operationId;
            this.contentFile = contentFile;
            this.file = file;
            this.thumbnail = thumbnail;
            this.contentShareSource = contentShareSource;
        }

        public ContentItem(Parcel input) {
            this.operationId = input.readString();
            Uri uri = Uri.parse(input.readString());

            this.file = new java.io.File(URI.create(uri.toString()));
            this.contentFile = cachedContentFile(uri, null, null, null, null);
            this.contentShareSource = input.readString();
        }

        public static final Creator<ContentItem> CREATOR = new Creator<ContentItem>() {
            @Override
            public ContentItem createFromParcel(Parcel in) {
                return new ContentItem(in);
            }

            @Override
            public ContentItem[] newArray(int size) {
                return new ContentItem[size];
            }
        };

        public String getOperationId() {
            return this.operationId;
        }

        public void setOperationId(String operationId) {
            this.operationId = operationId;
        }

        public File getContentFile() {
            return this.contentFile;
        }

        public void setContentFile(File contentFile) {
            this.contentFile = contentFile;
        }

        public java.io.File getFile() {
            return file;
        }

        public Bitmap getThumbnail() {
            return this.thumbnail;
        }

        public void setThumbnail(Bitmap thumbnail) {
            this.thumbnail = thumbnail;
        }

        public String getContentShareSource() {
            return contentShareSource;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(operationId);
            dest.writeValue(Uri.fromFile(file).toString());
            dest.writeString(contentShareSource);
        }
    }

    public PostContentActivityOperation(Injector injector, String conversationId, ShareContentData shareContentData,
                                        Comment comment, List<File> content) {
        this(injector, conversationId, shareContentData, comment, content, null);
    }

    public PostContentActivityOperation(Injector injector, String conversationId, ShareContentData shareContentData,
                                        Comment comment, List<File> content, List<String> operationIds) {
        super(injector, conversationId);

        Ln.d("New PostContentActivityOperation conversationId=" + conversationId);

        this.shareContentData = shareContentData;
        this.comment = comment;

        if (operationIds != null) {
            this.setDependsOn(operationIds);
        }

        this.files = new ItemCollection<>(content);
    }

    /**
     * Add File to a list, and cache it locally
     */
    public static File cachedContentFile(Uri contentUri, String conversationId, ContentManager contentManager, ContentResolver contentResolver, List<ContentAction> actions) {
        try {
            contentManager.addUploadedContent(new java.io.File(new URI(contentUri.toString())), contentUri, Cache.MEDIA);

            File modelFile = new File();
            modelFile.setUri(contentUri);
            modelFile.setMimeType(MimeUtils.getMimeType(contentUri.toString()));
            modelFile.setDisplayName(contentUri.getLastPathSegment());
            MimeUtils.ContentType type = MimeUtils.getContentTypeByFilename(contentUri.getLastPathSegment());
            if (type == MimeUtils.ContentType.IMAGE || type == MimeUtils.ContentType.VIDEO) {
                java.io.File thumbDir = contentManager.getContentDirectory(ConversationContract.ContentDataCacheEntry.Cache.THUMBNAIL, modelFile.getUrl());
                java.io.File thumbFile = new java.io.File(thumbDir, modelFile.getUrl().getLastPathSegment() + ".png");
                Image newThumb = ImageUtils.getThumbnailTempFile(thumbFile, contentUri, type);
                modelFile.setImage(newThumb);
            }

            if (type == MimeUtils.ContentType.IMAGE) {
                modelFile.setActions(actions);
            }
            return modelFile;
        } catch (URISyntaxException e) {
            Ln.e(e, "Failed parsing content URI.");
            return null;
        } finally {
            if (contentResolver != null) {
                contentResolver
                        .notifyChange(ConversationEntry.getConversationActivitiesUri(conversationId), null, false);
            }
        }
    }

    @Override
    protected void configureActivity() {
        super.configureActivity(Verb.share);
        addLocationData();

        Bundle bundle = ConversationContentProviderQueries.getConversationById(getContentResolver(), conversationId);
        if (bundle != null) {
            String participantCountStr = bundle.getString(ConversationEntry.PARTICIPANT_COUNT.name());
            if (!TextUtils.isEmpty(participantCountStr)) {
                participantCount = Integer.valueOf(participantCountStr);
            }
        } else {
            Ln.i("bundle is null for conversationId " + conversationId);
        }

        String contentCategory = shareContentData.getContentCategory();
        Content object = new Content(contentCategory);
        object.setContent(comment.getContent());
        object.setDisplayName(comment.getDisplayName());
        object.setMentions(comment.getMentions());
        object.setFiles(files);
        activity.setObject(object);
    }

    @NonNull
    @Override
    protected SyncState onEnqueue() {
        SyncState ret = super.onEnqueue();

        Bundle convValues = ConversationContentProviderQueries.getConversationById(getContentResolver(), getConversationId());
        selfPresence = ConversationContentProviderQueries.getOneValue(getContentResolver(), ConversationContract.ActorEntry.CONTENT_URI, ConversationContract.ActorEntry.PRESENCE_STATUS.name(), ConversationContract.ActorEntry.ACTOR_UUID + "=?", new String[] { authenticatedUserProvider.getAuthenticatedUser().getUserId() });
        selfPresence = (selfPresence != null) ? selfPresence : "unknown";
        otherPresence = null;

        if (convValues != null) {
            String lookup = convValues.getString(ConversationContract.ConversationEntry.ONE_ON_ONE_PARTICIPANT.name(), null);

            if (lookup != null) {
                ActorRecord oneOneParticipant = actorRecordProvider.get(lookup);
                otherPresence = (oneOneParticipant.getPresenceStatus() != null) ? oneOneParticipant.getPresenceStatus().toString() : "unknown";
            }
        }

        return ret;
    }

    @Override
    protected SyncState doWork() throws IOException {
        super.doWork();
        int pos = 0;
        List<String> shareSourceList = new ArrayList<>();
        List<String> mimeTypeList = new ArrayList<>();
        List<Long> fileSizeList = new ArrayList<>();


        if (!networkReachability.isNetworkConnected()) {
            return SyncState.READY;
        }

        for (String operationId : getDependsOn()) {
            Operation operation = operationQueue.get(operationId);

            if (operation == null || operation.getOperationType() != OperationType.CONTENT_UPLOAD) {
                continue;
            }

            ContentUploadOperation contentUploadOperation = ((ContentUploadOperation) operation);

            if (operation == null || operation.getState() != SyncState.SUCCEEDED) {
                return SyncState.READY;
            }

            for (File file : contentUploadOperation.getFiles()) {
                if (!checkUriIsSafe(file.getUrl())) {
                    Ln.w("Invalid file url scheme : " + file.getUrl().getScheme());
                    continue;
                }

                if (file.getImage() != null && !checkUriIsSafe(file.getImage().getUrl())) {
                    Ln.w("Invalid file url scheme for thumbnail : " + file.getImage().getUrl().getScheme());
                    continue;
                }

                ContentItem contentItem = shareContentData.getContentItems().get(pos);
                shareSourceList.add(contentItem.getContentShareSource().toString());
                mimeTypeList.add(file.getMimeType());
                fileSizeList.add(file.getFileSize() / 1024);

                ((Content) activity.getObject()).getFiles().getItems().set(pos++, file);
            }
        }

        List items = ((Content) activity.getObject()).getFiles().getItems();

        // pos was incremented. Make sure we don't have any items other than the ones we uploaded.
        while (items.size() > pos) {
            items.remove(pos);
        }

        if (items.isEmpty()) {
            Ln.w("Canceling PostContentOperation because items list is empty");
            Toaster.showLong(context, R.string.error_sending_message);
            cancel();
            return SyncState.FAULTED;
        }

        try {
            activity.setEncryptionKeyUrl(keyUri);
            activity = conversationProcessor.copyAndEncryptActivity(activity);
            Response<Activity> response = postContent(activity, shareContentData.doesHaveTranscodeableDocuments());

            if (response.isSuccessful()) {
                return SyncState.SUCCEEDED;
            }
        } catch (FileNotFoundException e) {
            Ln.e(e, "Failed sending file");
            Toaster.showLong(context, R.string.error_file_not_found);
            setErrorMessage("File not found");
            return SyncState.FAULTED;
        }

        return SyncState.READY;
    }

    @Override
    protected SyncState checkProgress() {
        return getState();
    }

    public static Activity buildContentActivity(Conversation conversation,
                                                List<File> files,
                                                String displayName,
                                                String clientId,
                                                String category) {

        Content c = new Content(category);
        ItemCollection<File> itemCollection = new ItemCollection<File>();
        for (File file : files) {
            itemCollection.addItem(file);
        }
        c.setFiles(itemCollection);
        c.setDisplayName(displayName);
        return Activity.share(null, c, conversation, clientId);
    }

    @NonNull
    @Override
    public OperationType getOperationType() {
        return OperationType.MESSAGE_WITH_CONTENT;
    }

    @Override
    public boolean needsNetwork() {
        // still need to do work without network to update the UI and prompt for retries
        return getState() == SyncState.EXECUTING;
    }

    @Override
    public boolean isSafeToRemove() {
        if (!super.isSafeToRemove())
            return false;

        // Keep faulted operations around unless they are canceled, the user can manually retry
        return (isCanceled() || isSucceeded());
    }

    private boolean checkUriIsSafe(Uri url) {
        return (url == null || url.getScheme().startsWith("http"));
    }

    @Override
    protected void onStateChanged(SyncState oldState) {
        super.onStateChanged(oldState);

        if (getState() == SyncState.SUCCEEDED) {
            for (String operationId : getDependsOn()) {
                Operation operation = operationQueue.get(operationId);

                if (operation == null || operation.getOperationType() != OperationType.CONTENT_UPLOAD) {
                    continue;
                }

                ((ContentUploadOperation) operation).setContentAttached(true);
            }
        } else if (getState() == SyncState.FAULTED) {
            switch (getFailureReason()) {
                case DEPENDENCY:
                    break;
                case CANCELED:
                    Toaster.showLong(context, R.string.error_message_canceled);
                    break;
                default:
                    Toaster.showLong(context, R.string.error_sending_message);
            }
        }
    }

    @NonNull
    @Override
    public RetryPolicy buildRetryPolicy() {
        return RetryPolicy.newJobTimeoutPolicy(3, TimeUnit.MINUTES)
                .withRetryDelay(0);
    }
}
