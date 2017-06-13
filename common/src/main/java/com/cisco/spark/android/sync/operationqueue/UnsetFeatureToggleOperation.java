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
import java.util.Date;
import java.util.Set;

import javax.inject.Inject;

import retrofit2.Response;

import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.OperationType;
import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncState;

/**
 * TODO:
 *
 * Only supports User features at the moment
 */
public class UnsetFeatureToggleOperation extends Operation {

    @NonNull
    private final String key;
    private Features.FeatureType featureType;
    private Set<String> emails;
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

    public UnsetFeatureToggleOperation(Injector injector, @NonNull String key, Features.FeatureType featureType, @NonNull Set<String> emails) {
        super(injector);
        this.key = key;
        this.featureType = featureType;
        this.emails = emails;
        lastModified = new Date();
    }

    @NonNull
    @Override
    public OperationType getOperationType() {
        return OperationType.UNSET_FEATURE_TOGGLE;
    }

    @NonNull
    @Override
    protected SyncState onEnqueue() {
        Ln.i("onEnqueue() unsetUserFeatureToggle: key: %s", key);
        deviceRegistration.getFeatures().unsetUserFeature(key);
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
            if (userId.equals(apiTokenProvider.getAuthenticatedUser().getKey().getUuid())) {
                FeatureToggle serverToggle = deviceRegistration.getFeatures().getFeature(featureType, key);
                if (serverToggle != null) {
                    Ln.v("doWork() server toggle key: %s value: %s lastMod: %s", serverToggle.getKey(), serverToggle.getVal(), serverToggle.getLastModified());
                }
                if (serverToggle != null && serverToggle.getLastModified() != null && serverToggle.getLastModified().compareTo(lastModified) > 0) {
                    Ln.v("doWork() ignoring as lastmodified local: %s lastmodified server: %s", lastModified, serverToggle.getLastModified());
                    continue;
                }
            }

            try {
                Response response = apiClientProvider.getFeatureClient().unsetUserFeature(userId, key).execute();
                if (response.isSuccessful()) {
                    if (userId.equals(apiTokenProvider.getAuthenticatedUser().getKey().getUuid())) {
                        Ln.v("doWork() unset userfeature: %s", key);
                        deviceRegistration.getFeatures().unsetUserFeature(key);
                    }
                } else {
                    Ln.i("Failed unsetting feature toggle: " + LoggingUtils.toString(response));
                }
            } catch (IOException e) {
                Ln.w(e, "Failed unsetting feature toggle");
            }
        }

        return SyncState.SUCCEEDED;
    }

    @Override
    protected void onNewOperationEnqueued(Operation newOperation) {
        if (newOperation instanceof UnsetFeatureToggleOperation) {
            UnsetFeatureToggleOperation newToggleOperation = (UnsetFeatureToggleOperation) newOperation;
            // a new operation on the same feature supersedes this one.
            if (key.equals(newToggleOperation.key) && featureType.equals(((UnsetFeatureToggleOperation) newOperation).featureType)) {
                Ln.v("onNewOperationEnqueued() A new operation on the same feature supersedes this one, cancel this one for key: %s", key);
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
