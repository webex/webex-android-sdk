package com.cisco.spark.android.sync.operationqueue;

import android.content.ContentProviderOperation;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.cisco.spark.android.R;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.model.Activity;
import com.cisco.spark.android.model.Content;
import com.cisco.spark.android.model.ContentCategory;
import com.cisco.spark.android.model.File;
import com.cisco.spark.android.model.ItemCollection;
import com.cisco.spark.android.model.Verb;
import com.cisco.spark.android.sync.Batch;
import com.cisco.spark.android.sync.ConversationAvatarContentReference;
import com.cisco.spark.android.sync.ConversationContentProviderQueries;
import com.cisco.spark.android.sync.ConversationContract;
import com.cisco.spark.android.sync.EncryptedConversationProcessor;
import com.cisco.spark.android.sync.operationqueue.core.Operation;
import com.cisco.spark.android.util.Toaster;
import com.github.benoitdion.ln.Ln;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import retrofit2.Response;

import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.OperationType;
import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.OperationType.ASSIGN_ROOM_AVATAR;
import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.OperationType.REMOVE_ROOM_AVATAR;
import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncState;

public class AssignRoomAvatarOperation extends ActivityOperation {
    @Inject
    transient EncryptedConversationProcessor conversationProcessor;

    private File file;

    public AssignRoomAvatarOperation(Injector injector, String conversationId, File file) {
        super(injector, conversationId);
        this.file = file;
    }

    protected void configureActivity() {
        super.configureActivity(Verb.assign);

        Content object = new Content(ContentCategory.IMAGES);
        object.setFiles(new ItemCollection<>(Arrays.asList(file)));
        activity.setObject(object);
    }

    @NonNull
    @Override
    protected SyncState onEnqueue() {
        Bundle conversationValues = ConversationContentProviderQueries.getConversationById(getContentResolver(), getConversationId());
        if (conversationValues != null) {
            String cacr = conversationValues.getString(ConversationContract.ConversationEntry.CONVERSATION_AVATAR_CONTENT_REFERENCE.name());
            ConversationAvatarContentReference conversationAvatarContentReference = gson.fromJson(cacr, ConversationAvatarContentReference.class);
            if (conversationAvatarContentReference == null) {
                conversationAvatarContentReference = new ConversationAvatarContentReference();
            }
            conversationAvatarContentReference.setUrl(file.getUrl());
            conversationAvatarContentReference.setSecureContentReference(null);
            conversationAvatarContentReference.setScr(null);
            ContentProviderOperation op = ContentProviderOperation.newUpdate(ConversationContract.ConversationEntry.CONTENT_URI)
                    .withValue(ConversationContract.ConversationEntry.CONVERSATION_AVATAR_CONTENT_REFERENCE.name(), gson.toJson(conversationAvatarContentReference, ConversationAvatarContentReference.class))
                    .withSelection(ConversationContract.ConversationEntry.CONVERSATION_ID + "=?", new String[]{getConversationId()})
                    .build();
            Batch batch = newBatch();
            batch.add(op);
            batch.apply();
        }
        return super.onEnqueue();
    }

    @NonNull
    @Override
    protected ConversationContract.SyncOperationEntry.SyncState doWork() throws IOException {
        super.doWork();

        for (String operationId : getDependsOn()) {
            Operation operation = operationQueue.get(operationId);

            if (operation == null || operation.getOperationType() != OperationType.CONTENT_UPLOAD) {
                continue;
            }

            ContentUploadOperation contentUploadOperation = ((ContentUploadOperation) operation);

            if (operation == null || operation.getState() != SyncState.SUCCEEDED) {
                return SyncState.READY;
            }

            File file = contentUploadOperation.getFiles().get(0);

            if (!checkUriIsSafe(file.getUrl())) {
                Ln.w("Invalid file url scheme : " + file.getUrl().getScheme());
                continue;
            }

            if (file.getImage() != null && !checkUriIsSafe(file.getImage().getUrl())) {
                Ln.w("Invalid file url scheme for thumbnail : " + file.getImage().getUrl().getScheme());
                continue;
            }

            file.setAuthor(null);
            file.setTranscodedCollection(null);
            file.setVersion(null);
            file.setDisplayName(null);

            ((Content) activity.getObject()).getFiles().getItems().set(0, file);

            break;
        }

        List items = ((Content) activity.getObject()).getFiles().getItems();

        if (items.isEmpty()) {
            Toaster.showLong(context, R.string.error_setting_space_avatar);
            cancel();
            return SyncState.FAULTED;
        }

        try {
            activity.setEncryptionKeyUrl(keyUri);
            activity = conversationProcessor.copyAndEncryptActivity(activity);
            Response<Activity> response = postActivity(activity);

            if (response.isSuccessful()) {
                return SyncState.SUCCEEDED;
            }
        } catch (FileNotFoundException e) {
            Ln.e(e, "Failed uploading avatar");
            Toaster.showLong(context, R.string.error_file_not_found);
            setErrorMessage("File not found");
            return SyncState.FAULTED;
        } catch (IOException e) {
            Ln.e(e, "Failed uploading avatar");
            return SyncState.FAULTED;
        }

        return SyncState.READY;
    }

    @Override
    protected void onNewOperationEnqueued(Operation newOperation) {
        super.onNewOperationEnqueued(newOperation);

        if (newOperation.getOperationType() != REMOVE_ROOM_AVATAR && newOperation.getOperationType() != ASSIGN_ROOM_AVATAR)
            return;

        // If assigning or removing the avatar again, the newer operation wins.
        if (((ActivityOperation) newOperation).getConversationId().equals(getConversationId())) {
            cancel();
        }
    }

    @NonNull
    @Override
    public OperationType getOperationType() {
        return ASSIGN_ROOM_AVATAR;
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
                default:
                    Toaster.showLong(context, R.string.error_setting_space_avatar);
            }
        }
    }

    private boolean checkUriIsSafe(Uri url) {
        return (url == null || url.getScheme().startsWith("http"));
    }
}
