package com.cisco.spark.android.whiteboard.snapshot;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.LruCache;

import com.cisco.crypto.scr.SecureContentReference;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.model.File;
import com.cisco.spark.android.model.KeyManager;
import com.cisco.spark.android.sync.operationqueue.core.OperationQueue;
import com.cisco.spark.android.util.ImageUtils;
import com.cisco.spark.android.util.Strings;
import com.cisco.spark.android.whiteboard.SnapshotUploadOperationCompleteEvent;
import com.cisco.spark.android.whiteboard.WhiteboardChannelImageClearedEvent;
import com.cisco.spark.android.whiteboard.WhiteboardChannelImageScrChangedEvent;
import com.cisco.spark.android.whiteboard.WhiteboardService;
import com.cisco.spark.android.whiteboard.WhiteboardStore;
import com.cisco.spark.android.whiteboard.loader.FileLoader;
import com.cisco.spark.android.whiteboard.persistence.model.Channel;
import com.cisco.spark.android.whiteboard.persistence.model.ChannelImage;
import com.cisco.spark.android.whiteboard.util.WhiteboardConstants;
import com.github.benoitdion.ln.Ln;
import com.nimbusds.jose.JOSEException;

import java.io.IOException;

import de.greenrobot.event.EventBus;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Action3;

public class SnapshotManager {
    private final KeyManager keyManager;
    private final Injector injector;
    private final OperationQueue operationQueue;
    private final FileLoader fileLoader;
    private final EventBus bus;
    private final Context context;
    private final WhiteboardService whiteboardService;
    private final LruCache<String, PendingUploadRequest> pendingUploadRequests = new LruCache<>(100);

    public SnapshotManager(Injector injector, KeyManager keyManager,
                           OperationQueue operationQueue, FileLoader fileLoader, EventBus bus, Context context,
                           WhiteboardService whiteboardService) {
        this.injector = injector;
        this.keyManager = keyManager;
        this.operationQueue = operationQueue;
        this.fileLoader = fileLoader;
        this.bus = bus;
        this.context = context;
        this.whiteboardService = whiteboardService;
    }

    public void uploadWhiteboardSnapshot(final Uri imageUrl, final Channel channel, final WhiteboardStore store,
                                         final Uri keyUrl, final String aclId,
                                         @NonNull final SnapshotManagerUploadListener snapshotManagerUploadListener) {
        if (channel.isUsingOldEncryption() && keyUrl != null) { //TODO fix this
            channel.setDefaultEncryptionKeyUrl(keyUrl); //TODO remove global getKeyUrl()
        }

        final Uri encryptionKeyUrl = channel.getDefaultEncryptionKeyUrl();
        if (encryptionKeyUrl != null) {
            Uri hiddenSpaceUrl = channel.getHiddenSpaceUrl();
            if (hiddenSpaceUrl != null /*|| channel.isUsingOldEncryption()*/) {
                performUploadSnapshotToSpaceUrl(imageUrl, hiddenSpaceUrl, channel, store, aclId, encryptionKeyUrl, snapshotManagerUploadListener);
            } else {
                store.fetchHiddenSpaceUrl(channel, new WhiteboardStore.OnHiddenSpaceUrlFetched() {
                    @Override
                    public void onSuccess(Uri hiddenSpaceUrl) {
                        channel.setHiddenSpaceUrl(hiddenSpaceUrl);
                        performUploadSnapshotToSpaceUrl(imageUrl, hiddenSpaceUrl, channel, store, aclId, encryptionKeyUrl, snapshotManagerUploadListener);
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        snapshotManagerUploadListener.onFailure(errorMessage);
                    }
                });
            }
        } else {
            final String channelId = channel != null ? channel.getChannelId() : null;
            snapshotManagerUploadListener.onFailure("uploadWhiteboardSnapshot failed: encryptionKey is null for channel: " + channelId);
        }
    }

    private void performUploadSnapshotToSpaceUrl(final Uri imageUrl, final Uri spaceUrl, final Channel channel, final WhiteboardStore store,
                                                 final String aclId, final Uri encryptionKeyUrl, @NonNull final SnapshotManagerUploadListener snapshotManagerUploadListener) {

        File snapshot = new File();
        snapshot.setUri(imageUrl);

        try {
            snapshot.encrypt(keyManager.getBoundKey(encryptionKeyUrl));
            SnapshotUploadOperation snapshotUploadOperation;
            if (channel.isUsingOldEncryption()) {
                snapshotUploadOperation = new SnapshotUploadOperation(injector, bus, aclId, snapshot);
            } else {
                snapshotUploadOperation = new SnapshotUploadOperation(injector, bus, spaceUrl, snapshot);
            }
            pendingUploadRequests.put(snapshotUploadOperation.getOperationId(), new PendingUploadRequest(snapshotUploadOperation, channel, imageUrl, snapshotManagerUploadListener));
            operationQueue.submit(snapshotUploadOperation);
        } catch (IOException e) {
            final String channelId = channel != null ? channel.getChannelId() : null;
            snapshotManagerUploadListener.onFailure("Error encrypting a snapshot, for channel: " + channelId + " not uploading." + e.getMessage());
        }
    }

    public void onEventBackgroundThread(SnapshotUploadOperationCompleteEvent event) {
        String operationId = event.getOperationId();
        PendingUploadRequest request = pendingUploadRequests.get(operationId);
        if (event.isSuccess() && request != null) {
            patchChannelWithSnapshot(request.operation, request.channel, request.imageUrl, request.snapshotManagerUploadListener);
        } else {
            if (request != null && request.snapshotManagerUploadListener != null) {
                String channelId = request.channel != null ? request.channel.getChannelId() : null;
                request.snapshotManagerUploadListener.onFailure("Failed uploading snapshot file for channel: " + channelId);
            } else {
                Ln.e("SnapshotUploadOperationCompleteEvent without a proper operation operationId: " + operationId);
            }
        }
    }

    private void patchChannelWithSnapshot(SnapshotUploadOperation operation, Channel channel, Uri imageUrl, SnapshotManagerUploadListener listener) {
        if (operation != null) {
            File uploadedFile = operation.getFiles().get(0);

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
                    store.patchChannel(patchedChannel, null);
                    listener.onSuccess();
                }
            } catch (JOSEException e) {
                String message = "SNAPX Whiteboard snapshot was successfully uploaded but couldn't encrypt the scr channel: " + channel.getChannelId();
                Ln.e(message, e);
                listener.onFailure(message);
            }
        }
    }

    public void downloadWhiteboardSnapshot(SecureContentReference secureContentReference, final SnapshotManagerDownloadListener snapshotManagerDownloadListener) {
        if (secureContentReference != null) {
            fileLoader.getPreview(secureContentReference, new Action3<Bitmap, SecureContentReference, Boolean>() {
                @Override
                public void call(final Bitmap bitmap, final SecureContentReference scr, Boolean fromCache) {
                    Observable.just(true).subscribeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Action1<Boolean>() {
                                @Override
                                public void call(Boolean b) {
                                    snapshotManagerDownloadListener.onSuccess(scr, bitmap);
                                }
                            }, new Action1<Throwable>() {
                                @Override
                                public void call(Throwable throwable) {
                                    snapshotManagerDownloadListener.onFailure(scr, "Failed fetching bitmap: " + throwable.getMessage());
                                }
                            });
                }
            }, new Action1<SecureContentReference>() {
                @Override
                public void call(SecureContentReference scr) {
                    snapshotManagerDownloadListener.onFailure(scr, "Failed fetching bitmap");
                }
            }, 3);
        } else {
            snapshotManagerDownloadListener.onFailure(secureContentReference, "Null secureContentReference");
        }
    }


    public void clearWhiteboardSnapshot(final Channel channel, WhiteboardStore store, Uri encryptionKeyUrl, String aclId) {
        final String filePath = context.getCacheDir() + "//" + WhiteboardConstants.WB_EMPTY_SNAPSHOT_FILENAME;
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
        if (hasEmptyBitmapFile)
            uploadWhiteboardSnapshot(uri, channel, store, encryptionKeyUrl, aclId, new SnapshotManagerUploadListener() {
                @Override
                public void onSuccess() {
                    bus.post(new WhiteboardChannelImageClearedEvent(channel));
                }

                @Override
                public void onFailure(String errorMessage) {
                    Ln.e("SnapshotManager: Couldn't clear snapshot for channel: " + channelId + " : " + errorMessage);
                }
            });
        else
            Ln.e("SnapshotManager: Couldn't clear snapshot for channel: " + channelId + " : Missing empty bitmap file");
    }

    public void start() {
        if (!bus.isRegistered(this)) {
            bus.register(this);
        }
    }

    public void stop() {
        if (bus.isRegistered(this)) {
            bus.unregister(this);
        }
    }

    private class PendingUploadRequest {
        private final SnapshotManagerUploadListener snapshotManagerUploadListener;
        private final SnapshotUploadOperation operation;
        private final Channel channel;
        private final Uri imageUrl;

        public PendingUploadRequest(SnapshotUploadOperation snapshotUploadOperation, Channel channel, Uri imageUrl,
                                    SnapshotManagerUploadListener snapshotManagerUploadListener) {
            this.operation = snapshotUploadOperation;
            this.channel = channel;
            this.snapshotManagerUploadListener = snapshotManagerUploadListener;
            this.imageUrl = imageUrl;
        }
    }

    public interface SnapshotManagerUploadListener {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    public interface SnapshotManagerDownloadListener {
        void onSuccess(SecureContentReference scr, Bitmap bitmap);
        void onFailure(SecureContentReference scr, String errorMessage);
    }

}
