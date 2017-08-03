package com.cisco.spark.android.whiteboard.snapshot;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.LruCache;

import com.cisco.crypto.scr.SecureContentReference;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.model.ActionMimeType;
import com.cisco.spark.android.model.Comment;
import com.cisco.spark.android.model.ContentAction;
import com.cisco.spark.android.model.Conversation;
import com.cisco.spark.android.model.File;
import com.cisco.spark.android.model.KeyManager;
import com.cisco.spark.android.model.KeyObject;
import com.cisco.spark.android.sync.ContentManager;
import com.cisco.spark.android.sync.operationqueue.PostContentActivityOperation;
import com.cisco.spark.android.sync.operationqueue.core.OperationQueue;
import com.cisco.spark.android.util.FileUtils;
import com.cisco.spark.android.util.ImageUtils;
import com.cisco.spark.android.util.MimeUtils;
import com.cisco.spark.android.util.Strings;
import com.cisco.spark.android.whiteboard.WhiteboardChannelImageClearedEvent;
import com.cisco.spark.android.whiteboard.WhiteboardChannelImageScrChangedEvent;
import com.cisco.spark.android.whiteboard.WhiteboardCreationCompleteEvent;
import com.cisco.spark.android.whiteboard.WhiteboardService;
import com.cisco.spark.android.whiteboard.WhiteboardStore;
import com.cisco.spark.android.whiteboard.loader.FileLoader;
import com.cisco.spark.android.whiteboard.persistence.AbsRemoteWhiteboardStore;
import com.cisco.spark.android.whiteboard.persistence.model.Channel;
import com.cisco.spark.android.whiteboard.persistence.model.ChannelImage;
import com.cisco.spark.android.whiteboard.util.WhiteboardConstants;
import com.github.benoitdion.ln.Ln;
import com.nimbusds.jose.JOSEException;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import de.greenrobot.event.EventBus;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;

import static com.cisco.spark.android.content.ContentShareSource.SCREENSHOT;
import static com.cisco.spark.android.model.ContentAction.TYPE_EDIT;
import static com.cisco.spark.android.whiteboard.util.WhiteboardConstants.WB_SNAPSHOT_FILENAME_BASE;
import static com.cisco.spark.android.whiteboard.util.WhiteboardConstants.WB_SNAPSHOT_FILENAME_EXTENSION;

public class SnapshotManager {
    private final KeyManager keyManager;
    private final Injector injector;
    private final OperationQueue operationQueue;
    private final FileLoader fileLoader;
    private final EventBus bus;
    private final Context context;
    private final WhiteboardService whiteboardService;
    private final Map<UUID, PendingChannelCreationRequest> pendingChannelCreationRequests = new HashMap<>();
    private final LruCache<String, Uri> pendingSaveToSparkSnapshot = new LruCache<>(100);
    private final Map<UUID, Channel> lastCreatedChannels = new HashMap<>();
    private final ContentManager contentManager;
    private final ContentResolver contentResolver;

    private static final String SNAPSHOT_UPLOAD_DIR = "/snapshot/";
    private static final String SAVE_TO_SPARK_DIR = "/saveToSpark/";

    private volatile boolean running;

    public SnapshotManager(Injector injector, KeyManager keyManager,
                           OperationQueue operationQueue, FileLoader fileLoader, EventBus bus, Context context,
                           WhiteboardService whiteboardService, ContentManager contentManager, ContentResolver contentResolver) {
        this.injector = injector;
        this.keyManager = keyManager;
        this.operationQueue = operationQueue;
        this.fileLoader = fileLoader;
        this.bus = bus;
        this.context = context;
        this.whiteboardService = whiteboardService;
        this.contentManager = contentManager;
        this.contentResolver = contentResolver;

        new java.io.File(getSnapshotUploadDir()).mkdir();
        new java.io.File(getSaveToSparkDir()).mkdir();
    }

    public void uploadWhiteboardSnapshot(Uri imageUrl, Channel channel, String conversationId, UUID snapshotId, boolean shouldPatch,
                                         SnapshotManagerUploadListener snapshotManagerUploadListener) {

        WhiteboardStore store = whiteboardService.getChannelWhiteboardStore(channel);
        if (channel.getHiddenSpaceUrl() != null) {
            uploadWhiteboardSnapshot(imageUrl, channel, snapshotId, conversationId, channel.getHiddenSpaceUrl(), shouldPatch,
                                     snapshotManagerUploadListener);
        } else {
            store.fetchHiddenSpaceUrl(channel, new WhiteboardStore.OnHiddenSpaceUrlFetched() {
                @Override
                public void onSuccess(@NonNull Uri hiddenSpaceUrl) {
                    channel.setHiddenSpaceUrl(hiddenSpaceUrl);
                    uploadWhiteboardSnapshot(imageUrl, channel, snapshotId, conversationId, channel.getHiddenSpaceUrl(), shouldPatch,
                                             snapshotManagerUploadListener);
                }

                @Override
                public void onFailure(String errorMessage) {
                    snapshotManagerUploadListener.onFailure(errorMessage);
                }
            });
        }
    }

    public void uploadWhiteboardSnapshot(Uri imageUrl, UUID channelCreationRequestId, String conversationId, SnapshotManagerUploadListener listener) {
        Channel channel = lastCreatedChannels.get(channelCreationRequestId);
        if (channel != null) {
            lastCreatedChannels.remove(channelCreationRequestId);
            uploadWhiteboardSnapshot(imageUrl, channel, conversationId, null, true, listener);
        } else {
            pendingChannelCreationRequests.put(channelCreationRequestId, new PendingChannelCreationRequest(imageUrl, conversationId, listener));
        }
    }

    public void onEventAsync(WhiteboardCreationCompleteEvent event) {
        final Channel channel = event.getChannel();
        final UUID channelCreationRequestId = event.getChannelCreationRequestId();

        lastCreatedChannels.put(channelCreationRequestId, channel);

        PendingChannelCreationRequest request = pendingChannelCreationRequests.get(channelCreationRequestId);
        if (request != null && channel != null) {
            pendingChannelCreationRequests.remove(channelCreationRequestId);
            uploadWhiteboardSnapshot(request.getImageUrl(), channel, request.getConversationId(), null, true, request.getListener());
        }

    }

    public void uploadWhiteboardSnapshot(@NonNull final Uri imageUrl, @NonNull final Channel channel, UUID snapshotId,
                                         final String aclId, final Uri spaceUrl, boolean shouldPatch,
                                         @NonNull final SnapshotManagerUploadListener snapshotManagerUploadListener) {

        final Uri encryptionKeyUrl = channel.getDefaultEncryptionKeyUrl();
        if (encryptionKeyUrl != null) {
            if (spaceUrl == null) {
                String message = "Could not upload a snapshot to a null url for channel " + channel.getChannelId();
                Ln.e(new IllegalArgumentException(message));
                snapshotManagerUploadListener.onFailure(message);
            } else {
                performUploadSnapshotToSpaceUrl(imageUrl, spaceUrl, channel, snapshotId, shouldPatch, aclId, encryptionKeyUrl, snapshotManagerUploadListener);
            }
        } else {
            final String channelId = channel.getChannelId();
            snapshotManagerUploadListener.onFailure("uploadWhiteboardSnapshot failed: encryptionKey is null for channel: " + channelId);
        }
    }

    private void performUploadSnapshotToSpaceUrl(final Uri imageUrl, final Uri spaceUrl, final Channel channel, final UUID snapshotId, boolean shouldPatch,
                                                 final String aclId, final Uri encryptionKeyUrl, @NonNull final SnapshotManagerUploadListener snapshotManagerUploadListener) {


        if (!running) {
            Ln.i("Abandoning snapshot upload for %s because ApplicationController is stopping", snapshotId);
            return;
        }

        File snapshot = new File();
        snapshot.setUri(imageUrl);

        try {
            snapshot.encrypt(keyManager.getBoundKey(encryptionKeyUrl));
            SnapshotUploadOperation snapshotUploadOperation;
            SnapshotUploadOperation.Callback callback = new SnapshotUploadOperation.Callback() {
                @Override
                public void onSuccess(String operationId, File file) {
                    KeyObject boundKey = keyManager.getBoundKey(channel.getDefaultEncryptionKeyUrl());
                    if (boundKey != null) {
                        if (shouldPatch) {
                            patchChannelWithSnapshot(file, channel, imageUrl, snapshotManagerUploadListener, operationId);
                        } else {
                            snapshotManagerUploadListener.onSuccess(operationId, file, null);
                        }
                    } else {
                        onFailure(operationId, "Failed uploading snapshot file for channel: " + (channel != null ? channel.getChannelId() : "null"));
                    }
                }

                @Override
                public void onFailure(String operationId, String errorMessage) {
                    if (snapshotManagerUploadListener != null) {
                        String channelId = channel != null ? channel.getChannelId() : null;
                        snapshotManagerUploadListener.onFailure("Failed uploading snapshot file for channel: " + channelId);
                    } else {
                        Ln.e("SnapshotUploadOperationCompleteEvent without a proper operation operationId: " + operationId);
                    }
                }
            };
            if (channel.isUsingOldEncryption()) {
                snapshotUploadOperation = new SnapshotUploadOperation(injector, bus, aclId, snapshot, callback);
            } else {
                snapshotUploadOperation = new SnapshotUploadOperation(injector, bus, spaceUrl, snapshot, callback);
            }
            operationQueue.submit(snapshotUploadOperation);
        } catch (IOException e) {
            final String channelId = channel != null ? channel.getChannelId() : null;
            snapshotManagerUploadListener.onFailure("Error encrypting a snapshot, for channel: " + channelId + " not uploading." + e.getMessage());
        }
    }

    private void patchChannelWithSnapshot(File uploadedFile, Channel channel, Uri imageUrl,
                                          SnapshotManagerUploadListener listener, String operationId) {
        try {
            SecureContentReference secureContentReference = uploadedFile.getSecureContentReference();
            if (secureContentReference != null) {
                fileLoader.putInCache(Strings.sha256(secureContentReference.getLoc()), imageUrl);
                bus.post(new WhiteboardChannelImageScrChangedEvent(channel, secureContentReference));

                ChannelImage image = new ChannelImage();
                image.setScr(secureContentReference.toJWE(keyManager.getBoundKey(channel.getDefaultEncryptionKeyUrl()).getKeyBytes()));
                image.setMimeType(uploadedFile.getMimeType());
                image.setEncryptionKeyUrl(channel.getDefaultEncryptionKeyUrl());
                image.setUrl(uploadedFile.getUrl());
                image.setFileSize(uploadedFile.getFileSize());

                Channel patchedChannel = new Channel();
                patchedChannel.setChannelId(channel.getChannelId());
                patchedChannel.setImage(image);

                WhiteboardStore store = whiteboardService.getChannelWhiteboardStore(channel);
                store.patchChannel(patchedChannel, new AbsRemoteWhiteboardStore.ClientCallback() {
                    @Override
                    public void onSuccess(Channel channel) {
                        listener.onSuccess(operationId, uploadedFile, channel);
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        listener.onFailure(errorMessage);
                    }
                });
            }
        } catch (JOSEException e) {
            String message = "SNAPX Whiteboard snapshot was successfully uploaded but couldn't encrypt the scr channel: " + channel.getChannelId();
            Ln.e(message, e);
            listener.onFailure(message);
        }
    }

    public void downloadWhiteboardSnapshot(SecureContentReference secureContentReference, final SnapshotManagerDownloadListener snapshotManagerDownloadListener) {
        if (secureContentReference != null) {
            fileLoader.getPreview(secureContentReference,
                    (bitmap, scr, fromCache) ->
                            Observable.just(true).subscribeOn(AndroidSchedulers.mainThread())
                                    .subscribe(b -> snapshotManagerDownloadListener.onSuccess(scr, bitmap),
                                            throwable -> snapshotManagerDownloadListener.onFailure(scr, "Failed fetching bitmap: " + throwable.getMessage())), scr -> snapshotManagerDownloadListener.onFailure(scr, "Failed fetching bitmap"), 3);
        } else {
            snapshotManagerDownloadListener.onFailure(secureContentReference, "Null secureContentReference");
        }
    }


    public void clearWhiteboardSnapshot(final Channel channel, WhiteboardStore store, String aclId) {
        final String filePath = getSnapshotUploadDir() + WhiteboardConstants.WB_EMPTY_SNAPSHOT_FILENAME;
        final Uri uri = Uri.parse("file://" + filePath);

        java.io.File file = new java.io.File(filePath);
        boolean hasEmptyBitmapFile = false;
        if (!file.exists()) {
            Bitmap emptyBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
            if (ImageUtils.writeBitmap(file, emptyBitmap)) {
                hasEmptyBitmapFile = true;
            }
        } else {
            hasEmptyBitmapFile = true;
        }

        final String channelId = channel != null ? channel.getChannelId() : null;
        if (hasEmptyBitmapFile && channelId != null && channel.getHiddenSpaceUrl() != null) {
            uploadWhiteboardSnapshot(uri, channel, null, aclId, channel.getHiddenSpaceUrl(), true, new SnapshotManagerUploadListener() {
                @Override
                public void onSuccess(String operationId, File file1, Channel patchedChannel) {
                    bus.post(new WhiteboardChannelImageClearedEvent(patchedChannel));
                }

                @Override
                public void onFailure(String errorMessage) {
                    Ln.e("SnapshotManager: Couldn't clear snapshot for channel: " + channelId + " : " + errorMessage);
                }
            });
        } else {
            Ln.e("SnapshotManager: Couldn't clear snapshot for channel: " + channelId + " : Missing empty bitmap file");
        }
    }

    public void postWhiteboardsSnapshotsToConversation(Conversation conversation, List<Channel> channels) {
        for (Channel channel: channels) {
            postWhiteboardsSnapshotsToConversation(conversation, channel, ActionMimeType.WHITEBOARD,
                                                   WB_SNAPSHOT_FILENAME_BASE + "_" + channel.getChannelId() + WB_SNAPSHOT_FILENAME_EXTENSION);
        }
    }

    public void postWhiteboardsSnapshotsToConversation(Conversation conversation, Channel channel, String actionMimeType, final String filename) {
        if (channel.getImage() == null || channel.getImage().getSecureContentReference() == null) {
            Ln.i("Null image or secureContentReference for channelId: " + channel.getChannelId() + ", fetching channel info");
            whiteboardService.getChannelInfo(channel.getChannelId(), new AbsRemoteWhiteboardStore.ChannelInfoCallback() {
                @Override
                public void onSuccess(Channel channel) {
                    if (channel.getImage() != null && channel.getImage().getSecureContentReference() != null) {
                        postWhiteboardSnapshotToConversation(conversation, channel, actionMimeType, filename);
                    } else {
                        Ln.w("Couldn't get secureContentReference for channel: " + channel.getChannelId());
                    }
                }

                @Override
                public void onFailure(String errorMessage) {
                    Ln.w("Failed getting updated channel for channel: " + channel.getChannelId());
                }
            });
        } else {
            postWhiteboardSnapshotToConversation(conversation, channel, actionMimeType, filename);
        }
    }

    @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
    private void postWhiteboardSnapshotToConversation(Conversation conversation, Channel channel, String actionMimeType, String filename) {

        final SecureContentReference secureContentReference = channel.getImage().getSecureContentReference();

        fileLoader.getPreview(secureContentReference, (bitmap, scr, aBoolean) -> {
            java.io.File ioFile = new java.io.File(SnapshotManager.this.getSaveToSparkDir() + filename);

            if (ImageUtils.writeBitmap(ioFile, bitmap)) {
                final Uri contentUri = Uri.fromFile(ioFile);
                File file = new File();
                file.setUri(contentUri);
                try {
                    file.encrypt(keyManager.getBoundKey(conversation.getDefaultActivityEncryptionKeyUrl()));
                } catch (IOException e) {
                    Ln.e("Could not encrypt snapshot file", e);
                    return;
                }
                String mimeType = MimeUtils.getMimeType(contentUri.toString());
                file.setMimeType(mimeType);
                file.setDisplayName(contentUri.getLastPathSegment());

                List<ContentAction> actions = Arrays.asList(new ContentAction(TYPE_EDIT, actionMimeType, whiteboardService.getBoardServiceUrl(channel.getChannelId())));
                PostContentActivityOperation.ContentItem item = new PostContentActivityOperation.ContentItem(ioFile, SCREENSHOT.toString());
                item.setContentFile(PostContentActivityOperation.cachedContentFile(contentUri, conversation.getId(), contentManager, contentResolver, actions));

                SnapshotUploadOperation.Callback callback = new SnapshotUploadOperation.Callback() {
                    @Override
                    public void onSuccess(String operationId, File file) {
                        deleteFile(contentUri);
                    }

                    @Override
                    public void onFailure(String operationId, String errorMessage) {
                        Ln.w("Snapshot upload failed for operation: " + operationId + ", " + errorMessage);
                        deleteFile(contentUri);
                    }
                };
                SnapshotUploadOperation op = new SnapshotUploadOperation(injector, bus, conversation.getId(), item.getContentFile(), callback);
                pendingSaveToSparkSnapshot.put(op.getOperationId(), contentUri);
                operationQueue.submit(op);

                item.setOperationId(op.getOperationId());

                PostContentActivityOperation.ShareContentData shareContentData = new PostContentActivityOperation.ShareContentData();
                shareContentData.addContentItem(item);

                operationQueue.submit(new PostContentActivityOperation(injector, conversation.getId(), shareContentData, new Comment(), Arrays.asList(file), shareContentData.getOperationIds()));
            }
        }, scr -> Ln.w("Could not get preview for channel " + channel.getChannelId()), 3);
    }

    private void deleteFile(Uri contentUri) {
        if (contentUri != null) {
            final boolean deleted = new java.io.File(contentUri.getPath()).delete();
            if (deleted) {
                Ln.d("Snapshot file deleted after upload: " + contentUri);
            } else {
                Ln.w("Could not delete snapshot file: " + contentUri);
            }
        }
    }


    public void start() {
        if (!bus.isRegistered(this)) {
            bus.register(this);
        }
        running = true;
    }

    public void stop() {
        running = false;
        if (bus.isRegistered(this)) {
            bus.unregister(this);
        }
    }

    public static class PendingUploadRequest {

        private final UUID snapshotId;
        private final SnapshotManagerUploadListener snapshotManagerUploadListener;
        public final SnapshotUploadOperation operation;
        private final Channel channel;
        private final Uri imageUrl;

        public PendingUploadRequest(SnapshotUploadOperation snapshotUploadOperation, Channel channel, Uri imageUrl,
                                    SnapshotManagerUploadListener snapshotManagerUploadListener, UUID snapshotId) {
            this.operation = snapshotUploadOperation;
            this.channel = channel;
            this.snapshotManagerUploadListener = snapshotManagerUploadListener;
            this.imageUrl = imageUrl;
            this.snapshotId = snapshotId;
        }

        public Uri getImageUrl() {
            return imageUrl;
        }

        public Channel getChannel() {
            return channel;
        }

        public UUID getSnapshotId() {
            return snapshotId;
        }
    }

    public String getSnapshotUploadDir() {
        return context.getCacheDir() + SNAPSHOT_UPLOAD_DIR;
    }

    public String getSaveToSparkDir() {
        return context.getCacheDir() + SAVE_TO_SPARK_DIR;
    }

    public void cleanDiskCache() {
        for (java.io.File file: new java.io.File(getSnapshotUploadDir()).listFiles())
            FileUtils.deleteRecursive(file);

        for (java.io.File file: new java.io.File(getSaveToSparkDir()).listFiles())
            FileUtils.deleteRecursive(file);
    }

    public static class PendingChannelCreationRequest {

        private Uri imageUrl;
        private String conversationId;
        private SnapshotManagerUploadListener listener;


        public PendingChannelCreationRequest(Uri imageUrl, String conversationId, SnapshotManagerUploadListener listener) {
            this.imageUrl = imageUrl;
            this.conversationId = conversationId;
            this.listener = listener;
        }

        public Uri getImageUrl() {
            return imageUrl;
        }

        public String getConversationId() {
            return conversationId;
        }

        public SnapshotManagerUploadListener getListener() {
            return listener;
        }
    }

    public interface SnapshotManagerUploadListener {
        void onSuccess(String operationId, File file, @Nullable Channel patchedChannel);
        void onFailure(String errorMessage);
    }

    public interface SnapshotManagerDownloadListener {
        void onSuccess(SecureContentReference scr, Bitmap bitmap);
        void onFailure(SecureContentReference scr, String errorMessage);
    }
}
