package com.cisco.spark.android.presence;

import com.cisco.spark.android.authenticator.AuthenticatedUserProvider;
import com.cisco.spark.android.sync.ActorRecord;
import com.cisco.spark.android.sync.ActorRecordProvider;
import com.cisco.spark.android.sync.Batch;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.cisco.spark.android.wdm.FeatureToggle;
import com.cisco.spark.android.wdm.Features;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Provider;

import de.greenrobot.event.EventBus;

public class PresenceStatusListener {
    private EventBus bus;
    private ActorRecordProvider actorRecordProvider;
    private AuthenticatedUserProvider authenticatedUserProvider;
    private Provider<Batch> batchProvider;
    private DeviceRegistration deviceRegistration;

    private Map<String, SubscriptionEntry> subscriptionEntries;

    public PresenceStatusListener(EventBus eventBus, ActorRecordProvider actorRecordProvider,
                                  AuthenticatedUserProvider authenticatedUserProvider, Provider<Batch> batchProvider,
                                  DeviceRegistration deviceRegistration) {
        this.bus = eventBus;
        this.actorRecordProvider = actorRecordProvider;
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.batchProvider = batchProvider;
        this.deviceRegistration = deviceRegistration;
        this.subscriptionEntries = new HashMap<>();

        bus.register(this);
    }

    public void addSubscription(String subject, int ttlInSeconds) {
        SubscriptionEntry subscriptionEntry = subscriptionEntries.get(subject);

        if (subscriptionEntry == null) {
            subscriptionEntry = new SubscriptionEntry();
            subscriptionEntries.put(subject, subscriptionEntry);
        }

        subscriptionEntry.subscriptionExpiration = PresenceUtils.getExpireTime(ttlInSeconds);
    }

    public void removeSubscription(String subject) {
        subscriptionEntries.remove(subject);
    }

    public boolean isSubscribed(String subject) {
        if (subscriptionEntries.containsKey(subject) && subscriptionEntries.get(subject).subscriptionExpiration != null) {
            Date expireDate = subscriptionEntries.get(subject).subscriptionExpiration;
            Date currentDate = new Date();

            return currentDate.getTime() <= expireDate.getTime();
        }

        return false;
    }

    @SuppressWarnings("UnusedMethod") // Called from the event bus
    public void onEvent(PresenceStatusResponse response) {
        Batch batch = batchProvider.get();

        addStatusUpdate(batch, response);

        batch.apply();
    }

    @SuppressWarnings("UnusedMethod") // Called from the event bus
    public void onEvent(PresenceStatusList statusList) {
        Batch batch = batchProvider.get();

        for (PresenceStatusResponse response : statusList.getStatusList()) {
            addStatusUpdate(batch, response);
            bus.post(response);
        }

        batch.apply();
    }

    private void addStatusUpdate(Batch batch, PresenceStatusResponse response) {
        ActorRecord actorRecord = actorRecordProvider.get(response.getSubject());

        if (actorRecord != null) {
            actorRecord.setPresenceStatus(response.getStatus());
            actorRecord.setPresenceLastActive(response.getLastActive());
            Date expirationDate = null;

            if (response.getExpiresTTL() <= 0) {
                if (response.getStatus() != PresenceStatus.PRESENCE_STATUS_OOO) {
                    expirationDate = PresenceUtils.getExpireTime(30);
                }
            } else {
                expirationDate = PresenceUtils.getExpireTime(response.getExpiresTTL());
            }

            actorRecord.setPresenceExpiration(expirationDate);

            actorRecord.addInsertUpdateContentProviderOperation(batch);

            if (actorRecord.isAuthenticatedUser(authenticatedUserProvider.getAuthenticatedUser())) {
                if (response.getStatus() == PresenceStatus.PRESENCE_STATUS_DO_NOT_DISTURB) {
                    bus.post(new QuietTimeStartedEvent(response.getExpiresTTL()));
                    deviceRegistration.getFeatures().setUserFeature(new FeatureToggle(Features.USER_PRESENCE_ENABLED, "true", true));
                } else if (response.getStatus() == PresenceStatus.PRESENCE_STATUS_UNKNOWN) {
                    deviceRegistration.getFeatures().setUserFeature(new FeatureToggle(Features.USER_PRESENCE_ENABLED, "false", true));
                } else {
                    deviceRegistration.getFeatures().setUserFeature(new FeatureToggle(Features.USER_PRESENCE_ENABLED, "true", true));
                }
            }
        }
    }

    private static class SubscriptionEntry {
        Date subscriptionExpiration;
    }
}
