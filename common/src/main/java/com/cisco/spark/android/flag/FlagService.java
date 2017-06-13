package com.cisco.spark.android.flag;


import android.content.ContentProviderOperation;

import com.cisco.spark.android.mercury.events.UserFeatureUpdate;
import com.cisco.spark.android.sync.Batch;
import com.cisco.spark.android.sync.ConversationContract;
import com.google.gson.Gson;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import de.greenrobot.event.EventBus;

@Singleton
public class FlagService {
    private final Gson gson;
    private final Provider<Batch> batchProvider;

    @Inject
    public FlagService(EventBus bus, Gson gson, Provider<Batch> batchProvider) {
        this.gson = gson;
        this.batchProvider = batchProvider;
        bus.register(this);
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEventAsync(UserFeatureUpdate event) {
        if ("flags".equals(event.getAppName())) {
            Flag flag = gson.fromJson(event.getData(), Flag.class);

            if ("delete".equals(event.getAction())) {
                Batch batch = batchProvider.get();
                batch.add(ContentProviderOperation.newDelete(ConversationContract.FlagEntry.CONTENT_URI)
                        .withSelection(ConversationContract.FlagEntry.FLAG_ID + "=?", new String[]{flag.getId()})
                        .build());
                batch.apply();
            } else {
                Batch batch = batchProvider.get();
                batch.add(ContentProviderOperation.newInsert(ConversationContract.FlagEntry.CONTENT_URI)
                        .withValue(ConversationContract.FlagEntry.FLAG_STATE.name(), 1)
                        .withValue(ConversationContract.FlagEntry.ACTIVITY_ID.name(), flag.getActivityId())
                        .withValue(ConversationContract.FlagEntry.FLAG_ID.name(), flag.getId())
                        .withValue(ConversationContract.FlagEntry.FLAG_UPDATE_TIME.name(), flag.getDateUpdated().getTime())
                        .build());
                batch.apply();
            }
        }
    }
}
