package com.cisco.spark.android.sync.queue;

import android.content.ContentProviderOperation;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.model.KeyObject;
import com.cisco.spark.android.sync.Batch;
import com.cisco.spark.android.sync.ConversationContentProviderOperation;
import com.cisco.spark.android.sync.ConversationContract;
import com.cisco.spark.android.sync.Message;
import com.cisco.spark.android.util.CryptoUtils;
import com.cisco.spark.android.util.UriUtils;
import com.github.benoitdion.ln.Ln;

import java.text.ParseException;

public class CatchUpSyncTask extends IncrementalSyncTask {
    private static boolean healEncrypted = true;

    public CatchUpSyncTask(Injector injector) {
        super(injector);
        withMaxActivities(50);
        withMaxConversations(MAX_CONVERSATIONS);
        withSinceTime(HIGH_WATER_MARK);
        withMaxParticipants(0);
    }

    @Override
    public boolean execute() {

        if (healEncrypted && ConversationSyncQueue.getHighWaterMark(getContentResolver()) > 0) {
            healEncryptedActivitiesAndTitles();
            healEncrypted = false;
        }

        return super.execute();
    }

    /**
     * Query activities with an encryption key url but without a key. If we have the key in cache,
     * decrypt the activity and store it back in the db. If we don't, request the key.
     *
     * TODO this may not be necessary anymore
     */
    private void healEncryptedActivitiesAndTitles() {
        if (!healEncrypted)
            return;

        Batch batch = batchProvider.get();
        Cursor c = null;
        try {

            c = getContentResolver().query(
                    ConversationContract.ActivityEntry.CONTENT_URI,
                    ConversationContract.ActivityEntry.DEFAULT_PROJECTION,
                    ConversationContract.ActivityEntry.ENCRYPTION_KEY_URL + " IS NOT NULL AND " + ConversationContract.ActivityEntry.IS_ENCRYPTED + " =1",
                    null, null);

            int activitiesDecrypted = 0;
            int activityDecryptionFailures = 0;
            while (c != null && c.moveToNext()) {
                final ConversationContract.ActivityEntry.Type type = ConversationContract.ActivityEntry.Type.values()[c.getInt(ConversationContract.ActivityEntry.ACTIVITY_TYPE.ordinal())];
                if (!type.isEncryptable())
                    continue;

                final long id = c.getLong(ConversationContract.ActivityEntry._id.ordinal());
                final String activityData = c.getString(ConversationContract.ActivityEntry.ACTIVITY_DATA.ordinal());
                final Uri keyUrl = Uri.parse(c.getString(ConversationContract.ActivityEntry.ENCRYPTION_KEY_URL.ordinal()));
                final Message message = gson.fromJson(activityData, type.getSyncClass());
                if (message == null) {
                    continue;
                }

                KeyObject key = conversationProcessor.getKeyForDecryption(keyUrl);
                if (key == null || TextUtils.isEmpty(key.getKey())) {
                    keysRequested.add(keyUrl);
                } else {
                    try {
                        message.decrypt(key);
                        batch.add(ConversationContentProviderOperation.insertEncryptionKey(keyUrl.toString(), key.getKey(), UriUtils.toString(key.getKeyId()), 0));
                        batch.add(ContentProviderOperation.newUpdate(ConversationContract.ActivityEntry.CONTENT_URI)
                                .withValue(ConversationContract.ActivityEntry.ACTIVITY_DATA.name(), gson.toJson(message))
                                .withValue(ConversationContract.ActivityEntry.IS_ENCRYPTED.name(), String.valueOf(0))
                                .withValue(ConversationContract.ActivityEntry.ENCRYPTION_KEY_URL.name(), keyUrl.toString())
                                .withSelection(ConversationContract.ActivityEntry._ID + "=?", new String[]{String.valueOf(id)})
                                .withYieldAllowed(getYieldAllowed()).build());
                        if (type.isSearchable()) {
                            searchManager.updateActivitySearch(type, message, c.getString(ConversationContract.ActivityEntry.ACTIVITY_ID.ordinal()), c.getString(ConversationContract.ActivityEntry.CONVERSATION_ID.ordinal()), c.getLong(ConversationContract.ActivityEntry.ACTIVITY_PUBLISHED_TIME.ordinal()), c.getString(ConversationContract.ActivityEntry.SYNC_OPERATION_ID.ordinal()));
                        }
                        activitiesDecrypted++;
                        encryptionDurationMetricManager.onError(100, "HEALED_ACTIVITY");
                    } catch (Exception e) {
                        Ln.d(e, "Decryption failure");
                        activityDecryptionFailures++;
                        if (e instanceof ParseException) {
                            Ln.d("Clear EncryptionKey Url due to ParseException");
                            if (CryptoUtils.isPlainTextMessage(message.getText())) {
                                batch.add(ConversationContentProviderOperation.clearActivityEncryptedFlag(id, gson.toJson(message)));
                            }
                        }
                    }
                }
            }
            batch.apply();
            Ln.d("Catch-Up healing result: Requested %d keys, decrypted %d activities, failed to decrypt %d activities", keysRequested.size(), activitiesDecrypted, activityDecryptionFailures);
        } catch (Exception e) {
            Ln.e(e);
        } finally {
            if (c != null)
                c.close();
        }

        if (CryptoUtils.healEncryptedConversationTitlesAndSummaries(getContentResolver(), conversationProcessor, batchProvider)) {
            encryptionDurationMetricManager.onError(100, "HEALED_TITLE");
        }

        if (CryptoUtils.healEncryptedConversationAvatars(getContentResolver(), conversationProcessor, batchProvider, gson)) {
            encryptionDurationMetricManager.onError(100, "HEALED_AVATAR");
        }
    }

}
