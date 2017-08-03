package com.cisco.spark.android.core;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

import com.cisco.spark.android.model.ItemCollection;
import com.cisco.spark.android.model.MultiRetentionPolicyRequest;
import com.cisco.spark.android.model.RetentionPolicy;
import com.cisco.spark.android.sync.Batch;
import com.cisco.spark.android.sync.ConversationContentProviderOperation;
import com.cisco.spark.android.sync.ConversationContentProviderQueries;
import com.cisco.spark.android.sync.ConversationContract.ConversationEntry;
import com.cisco.spark.android.sync.ConversationContract.OrganizationEntry;
import com.cisco.spark.android.util.Strings;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.github.benoitdion.ln.Ln;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import retrofit2.Response;

public class ActivityPruningIntentService extends SquaredIntentService {
    public static final String TAG = "[ActivityPruning]";

    @Inject
    Batch batch;

    @Inject
    ApiClientProvider apiClientProvider;

    @Inject
    DeviceRegistration deviceRegistration;

    public ActivityPruningIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // intent may be null if the service is being restarted after its process has gone away,
        // and it had previously returned anything except START_STICKY_COMPATIBILITY.
        // https://developer.android.com/reference/android/app/IntentService.html#onStartCommand(android.content.Intent,%20int,%20int)
        if (intent == null) {
            Ln.i("%s onHandleIntent null, abort (service is being restarted after its process has gone away)", TAG);
            return;
        }

        super.onHandleIntent(intent);

        if (!isInitialized()) {
            Ln.v("%s IntentService not initialized.", TAG);
            return;
        }

        Ln.v("%s IntentService onHandleIntent.", TAG);

        Map<String, String> retentionUrlMap = getOrgRetentionUrls();
        List<String> conversationRetentionUrlList = getConversationRetentionUrls();
        conversationRetentionUrlList.addAll(retentionUrlMap.keySet());
        try {
            // already in work thread, so don't use the operation and just call the api directly
            Response<ItemCollection<RetentionPolicy>> response = apiClientProvider.getRetentionClient()
                    .getMultiRetentionPolicy(new MultiRetentionPolicyRequest(conversationRetentionUrlList)).execute();

            if (response.isSuccessful()) {
                List<RetentionPolicy> retentionPolicys = response.body().getItems();
                updateOrgRetentionPolicy(retentionUrlMap, retentionPolicys);

                purgeGroupSpaceData();
                purgeOneOnOneSpaceData();
            }
        } catch (IOException e) {
            Ln.e(e, "Exception happened when getMultiRetentionPolicy");
        }
    }

    private Map<String, String> getOrgRetentionUrls() {
        // update all organizations' retentionPolicy
        Map<String, String> retentionUrlMap = new HashMap<>();
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(OrganizationEntry.CONTENT_URI,
                    new String[]{OrganizationEntry.ORG_ID.name(), OrganizationEntry.RETENTION_URL.name()},
                    null, null, null);
            while (cursor != null && cursor.moveToNext()) {
                String orgId = cursor.getString(0);
                String retentionUrl = cursor.getString(1);
                if (Strings.isEmpty(retentionUrl)) {
                    // TODO: when conversation service side add the retentionUrl for 1:1 into the conversation meta data. We can discard this constructing logic.
                    retentionUrl = constructRetentionUrl(orgId);
                }
                if (!Strings.isEmpty(retentionUrl)) {
                    retentionUrlMap.put(retentionUrl, orgId);
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return retentionUrlMap;
    }

    private List<String> getConversationRetentionUrls() {
        List<String> retentionUrlList = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(ConversationEntry.CONTENT_URI,
                    new String[]{"DISTINCT " + ConversationEntry.RETENTION_URL.name()},
                    null, null, null);
            while (cursor != null && cursor.moveToNext()) {
                String retentionUrl = cursor.getString(0);
                if (!Strings.isEmpty(retentionUrl)) {
                    retentionUrlList.add(retentionUrl);
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return retentionUrlList;
    }

    private void updateOrgRetentionPolicy(Map<String, String> retentionUrlMap, List<RetentionPolicy> retentionPolicys) {
        if (retentionPolicys == null || retentionPolicys.isEmpty()) {
            return;
        }

        for (RetentionPolicy retentionPolicy : retentionPolicys) {
            Ln.d("retention response: %s", retentionPolicy.toString());
            if (retentionPolicy.getStatus() == 200) {
                batch.add(ConversationContentProviderOperation.updateConversationRetentionPolicy(retentionPolicy));

                String orgId = retentionUrlMap.get(retentionPolicy.getRetentionUrl());
                if (!Strings.isEmpty(orgId)) {
                    batch.add(ConversationContentProviderOperation.updateRetentionPolicyWithOrgId(orgId, retentionPolicy));
                }
            }
        }
        batch.apply();
        batch.clear();
    }

    private void purgeGroupSpaceData() {
        long startTime = System.currentTimeMillis();
        batch.add(ConversationContentProviderOperation.deleteGroupSpaceActivitiesBeyondRetentionDate());
        batch.apply();
        batch.clear();

        Ln.i("%s purge group data used " + (System.currentTimeMillis() - startTime) + " ms.", TAG);
    }

    private void purgeOneOnOneSpaceData() {
        long startTime = System.currentTimeMillis();
        List<String> activityIds = ConversationContentProviderQueries.getOneOnOneSpaceExpiredActivityIdsForPurge(getContentResolver());
        if (!activityIds.isEmpty()) {
            int batchCount = 0;
            for (String activityId : activityIds) {
                batch.add(ConversationContentProviderOperation.deleteActivityWithId(activityId));
                batchCount++;
                if (batchCount % 100 == 0) {
                    batch.apply();
                    batch.clear();
                }
            }
            batch.apply();
            batch.clear();
        }

        Ln.i("%s purge 1:1 space data used " + (System.currentTimeMillis() - startTime) + " ms.", TAG);
    }

    private String constructRetentionUrl(String orgId) {
        if (Strings.isEmpty(orgId)) {
            return null;
        }

        Uri retentionServiceUrl = deviceRegistration.getRetentionServiceUrl();
        if (retentionServiceUrl == null) {
            return null;
        }

        Ln.d("orgId: %s; retentionUrl: %s", orgId, Uri.withAppendedPath(retentionServiceUrl, "retention/organization/" + orgId).toString());
        return Uri.withAppendedPath(retentionServiceUrl, "retention/organization/" + orgId).toString();
    }

    @Override
    public void onDestroy() {
        Ln.i("[%s] onDestroy()", TAG);
        super.onDestroy();
    }

}
