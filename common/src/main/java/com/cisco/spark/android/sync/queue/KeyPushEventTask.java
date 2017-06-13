package com.cisco.spark.android.sync.queue;

import android.content.ContentProviderOperation;
import android.database.Cursor;

import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.model.Activity;
import com.cisco.spark.android.model.Comment;
import com.cisco.spark.android.model.Content;
import com.cisco.spark.android.model.Conversation;
import com.cisco.spark.android.model.EventObject;
import com.cisco.spark.android.model.KeyObject;
import com.cisco.spark.android.model.Person;
import com.cisco.spark.android.sync.ActorRecord;
import com.cisco.spark.android.sync.Batch;
import com.cisco.spark.android.sync.ConversationContentProviderOperation;
import com.cisco.spark.android.sync.Message;
import com.cisco.spark.android.util.CryptoUtils;
import com.cisco.spark.android.util.UriUtils;
import com.github.benoitdion.ln.Ln;

import java.text.ParseException;
import java.util.Date;
import java.util.List;

import static com.cisco.spark.android.sync.ConversationContract.ActivityEntry;
import static com.cisco.spark.android.util.CryptoUtils.decryptMessage;

public class KeyPushEventTask extends AbstractConversationSyncTask {
    private List<KeyObject> keys;
    private Batch batch;

    protected KeyPushEventTask(Injector injector, List<KeyObject> keys) {
        super(injector);
        this.keys = keys;
    }

    @Override
    protected boolean execute() {
        if (keys == null)
            return true;

        Ln.get("THROTTLED").d("executing keyPushEventTask for " + keys.size() + " keys");

        batch = batchProvider.get();

        for (KeyObject key : keys) {
            insertThenUpdateEncryptionKeyInfo(UriUtils.toString(key.getKeyUrl()), key.getKey(), UriUtils.toString(key.getKeyUrl()));
            decryptActivitiesForKey(key);
            String decryptedTitleConv = CryptoUtils.decryptConversationTitleAndSummary(getContentResolver(), batch, key);
            String decryptedAvatarConv = CryptoUtils.decryptConversationAvatar(getContentResolver(), batch, key, gson);
            if (null != decryptedTitleConv || null != decryptedAvatarConv) {
                eventsProcessed++;
            }
        }

        batch.apply();

        eventBus.post(activityDecryptedEvent);

        sendEncryptionMetrics();

        return true;
    }

    private void insertThenUpdateEncryptionKeyInfo(String keyUrlString, String keyValue, String keyIdString) {
        batch.add(ConversationContentProviderOperation.insertEncryptionKey(keyUrlString, keyValue, keyIdString, 0));
        batch.add(ConversationContentProviderOperation.updateEncryptionKey(keyUrlString, keyValue, keyIdString));
    }

    private void decryptActivitiesForKey(KeyObject key) {
        Cursor c = null;
        try {
            c = getContentResolver().query(
                    ActivityEntry.CONTENT_URI,
                    ActivityEntry.DEFAULT_PROJECTION,
                    ActivityEntry.ENCRYPTION_KEY_URL + "=? AND " + ActivityEntry.IS_ENCRYPTED + " =1",
                    new String[]{key.getKeyUrl().toString()}, null);

            while (c != null && c.moveToNext()) {
                final ActivityEntry.Type type = ActivityEntry.Type.values()[c.getInt(ActivityEntry.ACTIVITY_TYPE.ordinal())];

                if (!type.isEncryptable())
                    continue;

                final long id = c.getLong(ActivityEntry._id.ordinal());
                final String activityData = c.getString(ActivityEntry.ACTIVITY_DATA.ordinal());
                final Message message = gson.fromJson(activityData, type.getSyncClass());

                if (message == null) {
                    Ln.w("Failed parsing Message from Json; _ID=" + id);
                    continue;
                }

                try {
                    decryptMessage(key, message);

                    batch.add(ContentProviderOperation.newUpdate(ActivityEntry.CONTENT_URI)
                            .withValue(ActivityEntry.ACTIVITY_DATA.name(), gson.toJson(message))
                            .withValue(ActivityEntry.IS_ENCRYPTED.name(), 0)
                            .withValue(ActivityEntry.ENCRYPTION_KEY_URL.name(), key.getKeyUrl().toString())
                            .withSelection(ActivityEntry._ID + "=?", new String[]{String.valueOf(id)}).build());

                    // Build an activity for the event
                    Activity activity = new Activity();
                    activity.setId(c.getString(ActivityEntry.ACTIVITY_ID.ordinal()));
                    ActorRecord actorRecord = actorRecordProvider.get(message.getActorKey());
                    if (actorRecord != null) {
                        activity.setActor(new Person(actorRecord.getEmail(), actorRecord.getDisplayName()));
                        if (actorRecord.isSquaredEntitled())
                            activity.getActor().getTags().addAll(actorRecord.getTags());
                    }
                    activity.setTarget(new Conversation(c.getString(ActivityEntry.CONVERSATION_ID.ordinal())));
                    activity.setPublished(new Date(c.getLong(ActivityEntry.ACTIVITY_PUBLISHED_TIME.ordinal())));
                    activity.setVerb(type.verb);
                    activity.setEncryptionKeyUrl(key.getKeyUrl());

                    switch (type) {
                        case MESSAGE:
                        case UPDATE_TITLE_AND_SUMMARY:
                            activity.setObject(new Comment(message.getText()));
                            break;
                        case FILE:
                        case PHOTO:
                        case ASSIGN_ROOM_AVATAR:
                        case WHITEBOARD:
                            activity.setObject(new Content(message.getContent()));
                            break;
                        case SCHEDULED_SYNCUP:
                            EventObject event = new EventObject();
                            event.setDisplayName(message.getText());
                            activity.setObject(event);
                            break;
                        default:
                            try {
                                throw new UnsupportedOperationException("Failed setting object for encrypted type " + type);
                            } catch (UnsupportedOperationException e) {
                                Ln.e(e);
                            }
                    }
                    activityDecryptedEvent.addActivity(activity);
                    eventsProcessed++;
                } catch (Exception e) {
                    Ln.e(e, "Unable to decrypt message");
                    if (e instanceof ParseException && !CryptoUtils.looksLikeCipherText(message.getText())) {
                        Ln.e(false, "Clear EncryptionKey Url due to ParseException");
                        batch.add(ConversationContentProviderOperation.clearActivityEncryptedFlag(id, gson.toJson(message)));
                    }
                }
            }
        } finally {
            if (c != null)
                c.close();
        }
    }
}
