package com.cisco.spark.android.whiteboard;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;

import com.cisco.crypto.scr.SecureContentReference;
import com.cisco.spark.android.callcontrol.CallControlService;
import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.events.ForceLoadBoardEvent;
import com.cisco.spark.android.events.SnapshotEvent;
import com.cisco.spark.android.features.CoreFeatures;
import com.cisco.spark.android.locus.model.LocusDataCache;
import com.cisco.spark.android.model.ActionMimeType;
import com.cisco.spark.android.model.Conversation;
import com.cisco.spark.android.model.File;
import com.cisco.spark.android.model.KeyManager;
import com.cisco.spark.android.model.KeyObject;
import com.cisco.spark.android.sdk.SdkClient;
import com.cisco.spark.android.util.FileUtils;
import com.cisco.spark.android.util.ImageUtils;
import com.cisco.spark.android.util.SchedulerProvider;
import com.cisco.spark.android.whiteboard.loader.FileLoader;
import com.cisco.spark.android.whiteboard.persistence.model.Channel;
import com.cisco.spark.android.whiteboard.persistence.model.ChannelImage;
import com.cisco.spark.android.whiteboard.persistence.model.ChannelType;
import com.cisco.spark.android.whiteboard.persistence.model.Content;
import com.cisco.spark.android.whiteboard.snapshot.SnapshotManager;
import com.cisco.spark.android.whiteboard.util.WhiteboardConstants;
import com.github.benoitdion.ln.Ln;
import com.jakewharton.rxrelay.PublishRelay;
import com.nimbusds.jose.JOSEException;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import de.greenrobot.event.EventBus;
import rx.Observable;
import rx.schedulers.Schedulers;

import static com.cisco.spark.android.whiteboard.AnnotationCreator.State.ATTACH_SNAPSHOT;
import static com.cisco.spark.android.whiteboard.AnnotationCreator.State.CREATED_BOARD;
import static com.cisco.spark.android.whiteboard.AnnotationCreator.State.CREATE_COMPLETE;
import static com.cisco.spark.android.whiteboard.AnnotationCreator.State.FAIL;
import static com.cisco.spark.android.whiteboard.AnnotationCreator.State.GOT_KEYS;
import static com.cisco.spark.android.whiteboard.AnnotationCreator.State.NONE;
import static com.cisco.spark.android.whiteboard.AnnotationCreator.State.RECEIVED_SNAPSHOT;
import static com.cisco.spark.android.whiteboard.AnnotationCreator.State.SCALED_BITMAP;
import static com.cisco.spark.android.whiteboard.AnnotationCreator.State.SNAPSHOT_UPLOAD;
import static com.cisco.spark.android.whiteboard.AnnotationCreator.State.STARTED;
import static com.cisco.spark.android.whiteboard.util.WhiteboardConstants.WB_SNAPSHOT_FILENAME_EXTENSION;

public class AnnotationCreator {

    public static final int SNAPSHOT_WAIT_PERIOD = 5;
    public static final TimeUnit SNAPSHOT_WAIT_UNIT = TimeUnit.SECONDS;

    private final CallControlService callControlService;
    private final ApiClientProvider apiClientProvider;
    private final WhiteboardService whiteboardService;
    private final SchedulerProvider schedulerProvider;
    private final SnapshotManager snapshotManager;
    private final LocusDataCache locusDataCache;
    private final CoreFeatures coreFeatures;
    private final KeyManager keyManager;
    private final FileLoader fileLoader;
    private final SdkClient sdkClient;
    private final Context context;
    private final EventBus bus;

    private final Map<UUID, Data> annotationRequests;

    private final PublishRelay<Update> annotationStream;

    private static final int SNAPSHOT_HEIGHT = 1080;

    private static final String ANNOTATION_DIR = "/annotation/";

    private int mCreateCount;

    public AnnotationCreator(ApiClientProvider apiClientProvider,
                             WhiteboardService whiteboardService, SchedulerProvider schedulerProvider, SnapshotManager snapshotManager,
                             CallControlService callControlService, LocusDataCache locusDataCache,
                             CoreFeatures coreFeatures, KeyManager keyManager, SdkClient sdkClient, Context context,
                             EventBus bus, FileLoader fileLoader) {

        this.callControlService = callControlService;
        this.apiClientProvider = apiClientProvider;
        this.whiteboardService = whiteboardService;
        this.schedulerProvider = schedulerProvider;
        this.snapshotManager = snapshotManager;
        this.locusDataCache = locusDataCache;
        this.coreFeatures = coreFeatures;
        this.fileLoader = fileLoader;
        this.keyManager = keyManager;
        this.sdkClient = sdkClient;
        this.context = context;
        this.bus = bus;

        annotationRequests = new HashMap<>();
        annotationStream = PublishRelay.create();

        new java.io.File(getAnnotationDir()).mkdir();

        bus.register(this);
    }

    private boolean canAnnotate() {
        return coreFeatures.isWhiteboardEnabled();
    }

    public UUID beginAnnotation(AnnotationParams args) {

        UUID uuid = UUID.randomUUID();
        synchronized (annotationRequests) {
            annotationRequests.put(uuid, new Data(uuid, args));
        }

        Observable.timer(SNAPSHOT_WAIT_PERIOD, SNAPSHOT_WAIT_UNIT, schedulerProvider.computation())
                .subscribe(t -> {
                    synchronized (annotationRequests) {
                        Data data = getData(uuid);
                        if (data != null && data.state == STARTED) {
                            // This means that the snapshot took too long
                            annotationFailed(uuid, RECEIVED_SNAPSHOT, "Snapshot request timed out");
                        }
                    }
                }, Ln::e);
        return uuid;
    }

    @Nullable
    public Data getData(UUID uuid) {
        synchronized (annotationRequests) {
            return annotationRequests.get(uuid);
        }
    }

    @NonNull
    public State getState(UUID uuid) {
        Data data = getData(uuid);
        if (data == null) {
            return NONE;
        } else {
            return data.state;
        }
    }

    public void setState(Data data, State state) {
        synchronized (data) {
            data.state = state;
            annotationStream.call(new Update(data.uuid, data.state));
        }
    }

    @Nullable
    public Bitmap getSnapshot(UUID uuid) {
        Data data = getData(uuid);
        if (data == null) {
            return null;
        } else {
            return data.bitmap;
        }
    }

    @SuppressWarnings("ConstantConditions")
    public void snapshotReceived(UUID uuid, Bitmap bitmap) {

        Data data = getData(uuid);

        if (!isValid(uuid, data)) {
            return;
        }

        data.bitmap = bitmap;
        setState(data, RECEIVED_SNAPSHOT);
        if (data.shouldEndCall) {
            callControlService.endCall(callControlService.getActiveCall().getLocusKey());
        }
    }


    private void unrecoverableFailure(State state, String message) {
        Ln.e(new IllegalStateException(String.format("Annotation creation failed at state %s with message %s", state, message)));
        reportFailure(null, state, message, true);
    }

    private void annotationFailed(UUID uuid, State state, String message) {

        if (uuid != null) {
            synchronized (annotationRequests) {
                annotationRequests.remove(uuid);
            }
        }

        if (state == FAIL) {
            Ln.w("You should pass the state that failed, not FAIL");
        }

        reportFailure(uuid, state, message, false);
    }

    private void reportFailure(UUID uuid, State state, String message, boolean unrecoverable) {

        Update update = new Update(uuid, FAIL);
        update.setError(state, message);
        update.setUnrecoverable(unrecoverable);
        annotationStream.call(update);
    }

    public void onEventAsync(SnapshotEvent event) {

        if (!canAnnotate()) {
            return;
        }

        UUID snapshotUUID = event.getSnapshotUUID();
        synchronized (annotationRequests) {
            if (!annotationRequests.containsKey(snapshotUUID)) {
                Ln.w("Ignoring snapshot event for unknown request");
                return;
            }
        }

        if (event.getBitmap() == null) {
            annotationFailed(snapshotUUID, RECEIVED_SNAPSHOT, "Didn't receive a valid bitmap from the media engine");
            return;
        }

        snapshotReceived(snapshotUUID, event.getBitmap());

        WhiteboardCreator.CreateData createData = new WhiteboardCreator.CreateData(whiteboardService.getWhiteboardContext());
        createData.setType(ChannelType.ANNOTATION);
        UUID createId = whiteboardService.createBoard(createData, new WhiteboardCreator.CreateCallback() {

            @Override
            public void onSuccess(UUID createID, Channel channel) {
                createBoardComplete(snapshotUUID, channel);
            }

            @Override
            public void onFail(UUID uuid) {
                annotationFailed(snapshotUUID, CREATED_BOARD, "Failed to create whiteboard");
            }
        });
    }

    @SuppressWarnings("ConstantConditions")
    public void createBoardComplete(UUID snapshotId, Channel channel) {

        if (!canAnnotate()) {
            return;
        }

        Data data = getData(snapshotId);
        if (!isValid(snapshotId, data)) {
            return;
        }

        setState(data, CREATED_BOARD);

        if (data.bitmap == null) {
            annotationFailed(snapshotId, CREATED_BOARD, "Got as far as creating board without snapshot");
            return;
        }

        if (channel.getOpenSpaceUrl() != null && channel.getHiddenSpaceUrl() != null) {

            if (data.shouldShare && data.shareShortcut) {
                share(data, channel);
            }
            uploadSnapshot(data, channel);

        } else {

            Observable<Channel> open = (channel.getOpenSpaceUrl()) != null ? Observable.just(channel) : createOpenSpace(channel);
            Observable<Channel> hidden = (channel.getHiddenSpaceUrl()) != null ? Observable.just(channel) : createHiddenSpace(channel);

            Observable.concat(open, hidden).last()
                    .subscribe(channelWithUrls -> {

                          if (data.shareShortcut) {
                              share(data, channelWithUrls);
                          }
                          uploadSnapshot(data, channel);
                      }, t -> annotationFailed(data.uuid, State.CREATED_BOARD,
                                               "Failed adding open/hidden space to channel"));
        }
    }

    private Observable<Channel> createOpenSpace(Channel channel) {
        return apiClientProvider.getWhiteboardPersistenceClient().getOpenSpace(channel.getChannelId()).subscribeOn(
                Schedulers.newThread()).map(spaceUrl -> {
            channel.setOpenSpaceUrl(spaceUrl.getSpaceUrl());
            return channel;
        });
    }

    private Observable<Channel> createHiddenSpace(Channel channel) {
        return apiClientProvider.getWhiteboardPersistenceClient().getHiddenSpaceRx(channel.getChannelId()).subscribeOn(
                Schedulers.newThread()).map(spaceUrl -> {
            channel.setHiddenSpaceUrl(spaceUrl.getSpaceUrl());
            return channel;
        });
    }

    // FIXME De-duplicate this with the SDK
    private Bitmap getScaledBitmap(Bitmap bitmap) {

        if (bitmap.getHeight() <= SNAPSHOT_HEIGHT) {
            return bitmap;
        }

        Bitmap newbmp = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(newbmp);
        canvas.drawColor(Color.WHITE);
        canvas.drawBitmap(bitmap, 0, 0, null);
        canvas.save(Canvas.ALL_SAVE_FLAG);
        canvas.restore();

        double aspect = (double) SNAPSHOT_HEIGHT / (double) bitmap.getHeight();
        int width = (int) (bitmap.getWidth() * aspect);

        Bitmap scaledBitmap = Bitmap.createScaledBitmap(newbmp, width, SNAPSHOT_HEIGHT, true);
        Matrix matrix = new Matrix();
        scaledBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(),
                matrix, true);
        newbmp.recycle();
        return scaledBitmap;
    }

    @Nullable
    private Uri writeToFile(Bitmap bitmap, String id) {

        String filePath = getAnnotationDir() + WhiteboardConstants.WB_SNAPSHOT_FILENAME_BASE + "_" + id + WB_SNAPSHOT_FILENAME_EXTENSION;
        java.io.File file = new java.io.File(filePath);

        if (ImageUtils.writeBitmap(file, bitmap)) {
            java.io.File f = new java.io.File(filePath);
            if (!f.exists() || f.isDirectory()) {
                return null;
            }

            return Uri.parse("file://" + f.getAbsolutePath());
        }

        return null;
    }

    private void share(Data requestData, Channel channel) {

        if (requestData.shouldShare && locusDataCache.isInCall()) {
            if (channel.isPrivateChannel()) {
                whiteboardService.privateShare(locusDataCache.getActiveLocus());
            } else {
                whiteboardService.share();
            }
        }
    }

    private void uploadSnapshot(Data requestData, Channel channel) {

        Bitmap bitmap = requestData.bitmap;

        // This shouldn't happen as it was checked before, but it could have been cleared
        if (bitmap == null) {
            annotationFailed(requestData.uuid, SCALED_BITMAP, "Could not scale bitmap because it had been cleared");
            return;
        }

        final Bitmap snapshot = getScaledBitmap(bitmap);
        setState(requestData, SCALED_BITMAP);

        Uri imageUrl = writeToFile(snapshot, channel.getChannelId());
        if (imageUrl == null) {
            annotationFailed(requestData.uuid, SCALED_BITMAP, "Failed to write bitmap to file");
            return;
        }

        final UUID snapshotId = requestData.uuid;
        snapshotManager.uploadWhiteboardSnapshot(imageUrl, channel, snapshotId, whiteboardService.getAclId(), channel.getOpenSpaceUrl(), false,
                new SnapshotManager.SnapshotManagerUploadListener() {
                    @Override
                    public void onSuccess(String operationId, File file1, Channel patchedChannel) {
                        fileLoader.putInCache(file1.getUrl().toString(), snapshot);

                        if (!canAnnotate()) {
                            return;
                        }

                        if (annotationRequests.size() == 0) {
                            Ln.d("Snapshot cannot be matched up with an annotation request");
                            return;
                        }

                        Data data;
                        if (snapshotId == null) {
                            unrecoverableFailure(SNAPSHOT_UPLOAD, "Snapshot cannot be matched up with an annotation request");
                            return;
                        } else {
                            data = getData(snapshotId);
                        }

                        if (data == null) {
                            annotationFailed(snapshotId, SNAPSHOT_UPLOAD, "Could not find data for snapshot ID " + snapshotId);
                            return;
                        }

                        setState(data, State.SNAPSHOT_UPLOAD_COMPLETE);
                        attachSnapshotToBoard(data, snapshotId, channel, file1, imageUrl);
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        delete(imageUrl);
                        annotationFailed(requestData.uuid, SNAPSHOT_UPLOAD, "Couldn't upload snapshot to space URL");
                    }

                });

    }

    private void delete(Uri filePath) {
        if (!new java.io.File(filePath.getPath()).delete()) {
            Ln.w("Couldn't delete file: " + filePath.getPath());
        }
    }


    private void attachSnapshotToBoard(Data data, UUID snapshotId, Channel channel, File uploadedFile, Uri imageUrl) {

        ArrayList<Content> contents = new ArrayList<>();
        Bitmap bitmap = data.bitmap;
        Uri encryptionKeyUrl = channel.getDefaultEncryptionKeyUrl();

        Content image = Content.createFile(imageUrl, sdkClient.getDeviceType(), encryptionKeyUrl.toString(), bitmap.getWidth(), bitmap.getHeight());
        ChannelImage channelImage = image.getBackgroundImage();

        SecureContentReference secureContentReference = uploadedFile.getSecureContentReference();

        try {
            channelImage.setScr(secureContentReference.toJWE(keyManager.getBoundKey(channel.getDefaultEncryptionKeyUrl())
                    .getKeyBytes()));
        } catch (JOSEException e) {
            annotationFailed(data.uuid, ATTACH_SNAPSHOT, "");
            return;
        }

        setState(data, ATTACH_SNAPSHOT);

        channelImage.setMimeType(uploadedFile.getMimeType());
        channelImage.setEncryptionKeyUrl(channel.getDefaultEncryptionKeyUrl());
        channelImage.setUrl(uploadedFile.getUrl());
        channelImage.setFileSize(uploadedFile.getFileSize());
        channelImage.setDimensions(bitmap.getWidth(), bitmap.getHeight());

        keyManager.getBoundKeySync(encryptionKeyUrl).subscribe(ko -> {

            if (!ko.isValid()) {
                annotationFailed(data.uuid, GOT_KEYS, "");
                return;
            }

            setState(data, GOT_KEYS);

            if (image.getBackgroundImage().getScr() != null) {

                contents.add(image);
                whiteboardService.getChannelWhiteboardStore(channel).saveContent(channel, contents, null);

                bus.post(new ForceLoadBoardEvent(channel.getChannelId(), bitmap));

                if (locusDataCache.isInCall() && data.shouldShare && !data.shareShortcut) {
                    share(data, channel);
                }

                setState(data, CREATE_COMPLETE);

                if (data.conversationSnapshot != null) {
                    uploadSnapshotForChat(data, channel, imageUrl, ko);
                } else {
                    delete(imageUrl);
                }

                synchronized (annotationRequests) {
                    annotationRequests.remove(data.uuid);
                }

            } else {
                annotationFailed(data.uuid, GOT_KEYS, "Background image does not have a valid SCR");
            }
        }, t -> {
            Ln.e(t);
            annotationFailed(snapshotId, GOT_KEYS, "Could not get keys for " + snapshotId);
        });
    }

    private void uploadSnapshotForChat(Data data, Channel channel, Uri imageUrl, KeyObject ko) {
        Ln.i("Uploading annotation snapshot for chat");
        snapshotManager.uploadWhiteboardSnapshot(imageUrl, channel, UUID.randomUUID(), whiteboardService.getAclId(), channel.getHiddenSpaceUrl(), true,
                                                 new SnapshotManager.SnapshotManagerUploadListener() {
                                                     @Override
                                                     public void onSuccess(String operationId, File file, Channel channel) {
                                                         try {
                                                             channel.getImage().decrypt(ko);
                                                             postSnapshotToChat(data.conversationSnapshot, data.annotationFilename, channel);
                                                         } catch (IOException | ParseException e) {
                                                             Ln.e(e, "Failed to upload annotation snapshot for chat");
                                                         }
                                                         delete(imageUrl);

                                                     }

                                                     @Override
                                                     public void onFailure(String errorMessage) {
                                                         Ln.e("Failed to upload channel snapshot: " + errorMessage);
                                                         delete(imageUrl);
                                                     }
                                                 });
    }

    private void postSnapshotToChat(Conversation conversation, String filename, Channel channel) {
        Ln.i("Posting annotation snapshot to chat");
        snapshotManager.postWhiteboardsSnapshotsToConversation(conversation, channel, ActionMimeType.ANNOTATION, filename);
    }

    private boolean isValid(UUID uuid, Data data) {

        if (data == null || data.state == FAIL) {
            String message = String.format(Locale.UK, "Data unregistered for UUID %s on snapshot", uuid);
            Ln.e(new IllegalStateException(message));
            annotationFailed(uuid, RECEIVED_SNAPSHOT, message);
            return false;
        }

        return true;
    }

    public Observable<Update> register(UUID uuid) {

        if (!canAnnotate()) {
            return Observable.empty();
        }

        return annotationStream.filter(update -> uuid.equals(update.uuid) || update.isUnrecoverable());
    }

    public String getAnnotationDir() {
        return context.getCacheDir() + ANNOTATION_DIR;
    }

    public void cleanDiskCache() {
        for (java.io.File file: new java.io.File(getAnnotationDir()).listFiles())
            FileUtils.deleteRecursive(file);
    }

    public static class AnnotationParams {

        boolean shouldEndCall;
        boolean shouldShare;
        boolean shareShortcut;

        Conversation converstaionSnapshot;
        public String annotationFilename;

        public AnnotationParams() {

        }

        public AnnotationParams setShouldEndCall(boolean shouldEndCall) {
            this.shouldEndCall = shouldEndCall;
            return this;
        }

        public AnnotationParams setShouldShare(boolean shouldShare) {
            this.shouldShare = shouldShare;
            return this;
        }

        public AnnotationParams setShareShortcut(boolean shareShortcut) {
            this.shareShortcut = shareShortcut;
            return this;
        }

        public AnnotationParams postSnapshotTo(Conversation conversation) {
            this.converstaionSnapshot = conversation;
            return this;
        }

        public AnnotationParams annotationFileName(String name) {
            this.annotationFilename = name;
            return this;
        }
    }

    public static class Data {

        final UUID uuid;
        State state;

        UUID channelId;
        Bitmap bitmap;

        final boolean shouldEndCall;
        final boolean shouldShare;
        final boolean shareShortcut;

        // For posting to chat
        final Conversation conversationSnapshot;
        final String annotationFilename;

        public Data(UUID id, AnnotationParams args) {
            this.uuid = id;
            this.state = STARTED;

            this.shouldEndCall = args.shouldEndCall;
            this.shouldShare = args.shouldShare;
            this.shareShortcut = args.shareShortcut;
            this.conversationSnapshot = args.converstaionSnapshot;
            this.annotationFilename = args.annotationFilename;
        }

        public Bitmap getBitmap() {
            return bitmap;
        }
    }

    public static class Update {

        final UUID uuid;
        final State state;

        boolean unrecoverable;
        Pair<State, String> error;

        public Update(UUID uuid, State state) {
            this.uuid = uuid;
            this.state = state;
        }

        public UUID getUuid() {
            return uuid;
        }

        public State getState() {
            return state;
        }

        public boolean isUnrecoverable() {
            return unrecoverable;
        }

        public void setUnrecoverable(boolean unrecoverable) {
            this.unrecoverable = unrecoverable;
        }

        @Nullable
        public String getErrorMessage() {

            if (error == null)
                return null;

            return error.second;
        }

        @Nullable
        public State getFailureState() {

            if (error == null)
                return null;

            return error.first;
        }

        public void setError(State failureState, String error) {
            this.error = new Pair<>(failureState, error);
        }
    }

    public enum State {
        NONE,
        STARTED,
        RECEIVED_SNAPSHOT,
        CREATED_BOARD,
        SCALED_BITMAP,
        SNAPSHOT_UPLOAD,
        SNAPSHOT_UPLOAD_COMPLETE,
        ATTACH_SNAPSHOT,
        GOT_KEYS,
        // ...
        CREATE_COMPLETE,
        FAIL
    }
}
