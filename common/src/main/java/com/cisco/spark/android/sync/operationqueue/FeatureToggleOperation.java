package com.cisco.spark.android.sync.operationqueue;

import android.support.annotation.NonNull;

import com.cisco.spark.android.authenticator.ApiTokenProvider;
import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.model.KeyManager;
import com.cisco.spark.android.sync.ActorRecordProvider;
import com.cisco.spark.android.sync.operationqueue.core.Operation;
import com.cisco.spark.android.sync.operationqueue.core.RetryPolicy;
import com.cisco.spark.android.util.LoggingUtils;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.cisco.spark.android.wdm.FeatureToggle;
import com.cisco.spark.android.wdm.Features;
import com.github.benoitdion.ln.Ln;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import retrofit2.Response;

import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.OperationType;
import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncState;
import static com.cisco.spark.android.wdm.Features.featureTypeToFeatureToggleType;

public class FeatureToggleOperation extends Operation {
    private Features.FeatureType featureType;
    private Set<String> emails;
    private List<FeatureToggle> newToggles;
    private Date lastModified;

    @Inject
    transient DeviceRegistration deviceRegistration;

    @Inject
    transient ApiClientProvider apiClientProvider;

    @Inject
    transient ApiTokenProvider apiTokenProvider;

    @Inject
    transient ActorRecordProvider actorRecordProvider;

    @Inject
    transient KeyManager keyManager;

    public FeatureToggleOperation(Injector injector, @NonNull Map<String, String> features, @NonNull Features.FeatureType featureType, @NonNull Set<String> emails) {
        super(injector);
        this.featureType = featureType;
        this.emails = emails;
        newToggles = new ArrayList<>(features.size());
        for (String s : features.keySet()) {
            FeatureToggle featureToggle = new FeatureToggle(s, features.get(s), true);
            featureToggle.setType(featureTypeToFeatureToggleType(featureType));
            newToggles.add(featureToggle);
        }
        lastModified = new Date();
    }

    @NonNull
    @Override
    public OperationType getOperationType() {
        return OperationType.FEATURE_TOGGLE;
    }

    @NonNull
    @Override
    protected SyncState onEnqueue() {
        for (FeatureToggle newToggle : newToggles) {
            Ln.v("onEnqueue() setFeature %s type: %s", newToggle, featureType);
            if (featureType == Features.FeatureType.DEVELOPER) {
                deviceRegistration.getFeatures().setDeveloperFeature(newToggle);
            } else {
                deviceRegistration.getFeatures().setUserFeature(newToggle);
            }
        }
        return SyncState.READY;
    }

    @NonNull
    @Override
    protected SyncState doWork() {
        Set<String> userIds = actorRecordProvider.emailsToUserIds(apiClientProvider, apiTokenProvider, emails);
        if (userIds == null) {
            ln.w("Failed getting user id's for feature toggle");
            return SyncState.READY;
        }
        userIds.add(apiTokenProvider.getAuthenticatedUser().getKey().getUuid());
        for (String userId : userIds) {
            // Make a copy of the feature toggles to set and check to make sure none of them have been set more recently on
            // the server for the currently logged in user (to avoid overwriting a more recent change)
            List<FeatureToggle> featureTogglesToSet = newToggles;
            if (userId.equals(apiTokenProvider.getAuthenticatedUser().getKey().getUuid())) {
                for (FeatureToggle featureToggle : newToggles) {
                    FeatureToggle serverToggle = deviceRegistration.getFeatures().getFeature(featureType, featureToggle.getKey());
                    if (serverToggle != null && serverToggle.getLastModified() != null && serverToggle.getLastModified().compareTo(lastModified) > 0) {
                        featureTogglesToSet.remove(featureToggle);
                    }
                }
            }
            try {
                if (newToggles.size() == 1) {
                    FeatureToggle newToggle = newToggles.get(0);
                    Response<FeatureToggle> response;
                    Ln.v("toggleFeature %s featureType: %s", newToggle, featureType);
                    if (featureType == Features.FeatureType.DEVELOPER) {
                        response = apiClientProvider.getFeatureClient().toggleDeveloperFeature(userId, newToggle).execute();
                    } else if (featureType == Features.FeatureType.USER) {
                        response = apiClientProvider.getFeatureClient().toggleUserFeature(userId, newToggle).execute();
                    } else {
                        response = apiClientProvider.getFeatureClient().toggleFeature(userId, newToggle).execute();
                    }
                    if (response.isSuccessful()) {
                        FeatureToggle result = response.body();
                        Ln.v("toggleFeature result: %s", result);
                        if (userId.equals(apiTokenProvider.getAuthenticatedUser().getKey().getUuid())) {
                            if (featureType == Features.FeatureType.DEVELOPER) {
                                deviceRegistration.getFeatures().setDeveloperFeature(result);
                            } else {
                                deviceRegistration.getFeatures().setUserFeature(result);
                            }
                        }
                    } else {
                        Ln.w("Failed setting feature toggle for " + userId + ": " + LoggingUtils.toString(response));
                    }
                } else {
                    Response<Void> response;
                    response = apiClientProvider.getFeatureClient().toggleFeatures(userId, newToggles).execute();
                    if (!response.isSuccessful()) {
                        Ln.w("Failed setting feature toggles for " + userId + ": " + LoggingUtils.toString(response));
                    }
                }
            } catch (IOException e) {
                Ln.e(e, "Failed setting feature toggle");
                return SyncState.READY;
            }
        }

        return SyncState.SUCCEEDED;
    }

    @Override
    protected void onNewOperationEnqueued(Operation newOperation) {
        Ln.v("onNewOperationEnqueued() newOperation: %s", newOperation);
        if (newOperation instanceof FeatureToggleOperation) {
            FeatureToggleOperation newToggleOperation = (FeatureToggleOperation) newOperation;
            // a new operation on the same feature supersedes this one.
            if (newToggles.equals(newToggleOperation.newToggles) && featureType.equals(((FeatureToggleOperation) newOperation).featureType)) {
                Ln.w("onNewOperationEnqueued cancel this operation: %s toggle: %s", newToggleOperation, Arrays.toString(newToggles.toArray()));
                cancel();
            }
        }
    }

    @Override
    protected SyncState checkProgress() {
        return getState();
    }

    @NonNull
    @Override
    public RetryPolicy buildRetryPolicy() {
        return RetryPolicy.newLimitAttemptsPolicy();
    }
}
