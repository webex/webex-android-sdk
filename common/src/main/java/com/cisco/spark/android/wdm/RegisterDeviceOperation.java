package com.cisco.spark.android.wdm;

import android.os.Build;
import android.support.annotation.NonNull;

import com.cisco.spark.android.authenticator.ApiTokenProvider;
import com.cisco.spark.android.authenticator.OAuth2;
import com.cisco.spark.android.authenticator.OAuth2Tokens;
import com.cisco.spark.android.core.AccessManager;
import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.AuthenticatedUser;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.core.SecureDevice;
import com.cisco.spark.android.core.Settings;
import com.cisco.spark.android.events.DeviceRegistrationChangedEvent;
import com.cisco.spark.android.events.RegisterDeviceOperationFailedEvent;
import com.cisco.spark.android.features.CoreFeatures;
import com.cisco.spark.android.metrics.SegmentService;
import com.cisco.spark.android.metrics.model.GenericMetric;
import com.cisco.spark.android.metrics.value.ClientMetricField;
import com.cisco.spark.android.metrics.value.ClientMetricNames;
import com.cisco.spark.android.metrics.value.ClientMetricTag;
import com.cisco.spark.android.model.User;
import com.cisco.spark.android.sync.operationqueue.AbstractOAuth2Operation;
import com.cisco.spark.android.sync.operationqueue.core.Operation;
import com.cisco.spark.android.sync.operationqueue.core.RetryPolicy;
import com.cisco.spark.android.util.Clock;
import com.cisco.spark.android.util.NetworkUtils;
import com.cisco.spark.android.util.Strings;
import com.cisco.spark.android.util.SystemUtils;
import com.cisco.spark.android.util.TestUtils;
import com.cisco.spark.android.util.UIUtils;
import com.github.benoitdion.ln.Ln;
import com.google.gson.Gson;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.UUID;

import javax.inject.Inject;

import dagger.Lazy;
import de.greenrobot.event.EventBus;
import okhttp3.ResponseBody;
import retrofit2.Response;

import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.OperationType;
import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.OperationType.REGISTER_DEVICE;
import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncState;

public class RegisterDeviceOperation extends AbstractOAuth2Operation {

    private final boolean gcmRegistration;
    private final boolean allowMoreRetries;

    @Inject
    transient ApiTokenProvider apiTokenProvider;

    @Inject
    transient ApiClientProvider apiClientProvider;

    @Inject
    transient DeviceRegistration deviceRegistration;

    @Inject
    transient AccessManager accessManager;

    @Inject
    transient CoreFeatures coreFeatures;

    @Inject
    transient SegmentService segmentService;

    @Inject
    transient Settings settings;

    @Inject
    transient EventBus bus;

    @Inject
    transient Gson gson;

    @Inject
    transient Lazy<DeviceInfo> deviceInfoProvider;

    @Inject
    transient Clock clock;

    @Inject
    transient SecureDevice secureDevice;

    private OAuth2Tokens tokens;
    private String clientCompatibilityHint;

    public RegisterDeviceOperation(Injector injector) {
        this(injector, false, false);

    }
    public RegisterDeviceOperation(Injector injector, boolean allowMoreRetries) {
        this(injector, allowMoreRetries, false);
    }

    public RegisterDeviceOperation(Injector injector, OAuth2Tokens tokens) {
        this(injector, false, false);
        this.tokens = tokens;
    }

    public RegisterDeviceOperation(Injector injector, boolean allowMoreRetries, boolean gcmRegistration) {
        super(injector);
        this.allowMoreRetries = allowMoreRetries;
        this.gcmRegistration = gcmRegistration;
    }

    @NonNull
    @Override
    public OperationType getOperationType() {
        return REGISTER_DEVICE;
    }

    @NonNull
    @Override
    protected SyncState doWork() throws IOException {
        WdmClient wdmClient = apiClientProvider.getWdmClient();

        Ln.d("Getting deviceInfo");
        DeviceInfo deviceInfo = deviceInfoProvider.get();

        Response<ResponseBody> response;

        try {
            if (deviceRegistration.getId() == null) {
                Ln.d("Creating new device");
                // Register the device as a TP_ENDPOINT if the user is a machine to be able to have
                // a test user fake a room
                if (!deviceInfo.getDeviceType().equals(DeviceInfo.SPARKBOARD_DEVICE_TYPE)) {
                    AuthenticatedUser authenticatedUser = apiTokenProvider.getAuthenticatedUserOrNull();
                    if (authenticatedUser != null && authenticatedUser.isMachine()) {
                        Ln.i("Override deviceType for machine user '%s', type was %s but will be registered as a TP_ENDPOINT",
                             authenticatedUser.getDisplayName(), deviceInfo.getDeviceType());
                        deviceInfo.setDeviceType(DeviceInfo.TP_DEVICE_TYPE);
                    }
                    deviceInfo.setDeviceManaged(true);
                    deviceInfo.setDeviceID(UUID.randomUUID());
                }
                response = wdmClient.createDevice(getAuthorizationHeader(), deviceInfo).execute();
            } else {
                Ln.d("Updating device");
                if (deviceRegistration.getDeviceIdentifier() != null) {
                    deviceInfo.setDeviceID(UUID.fromString(deviceRegistration.getDeviceIdentifier()));
                }
                response = wdmClient.updateDevice(getAuthorizationHeader(), deviceRegistration.getId(), deviceInfo).execute();
            }
        } catch (IOException error) {
            Ln.d(error);
            return SyncState.READY;
        }

        Ln.d("Got response with status code %d", response.code());
        if (response.isSuccessful()) {
            deviceRegistration.populateFrom(bodyToRegistration(response.body()));
            settings.setDeviceRegistration(deviceRegistration);

            // Find client-compatibility-hint header and check for required update
            clientCompatibilityHint = response.headers().get("client-compatibility-hint");

            if (!apiTokenProvider.isAuthenticated()) {
                apiTokenProvider.requestAuthenticatedUser(tokens, apiClientProvider, getAuthorizationHeader());
            } else {
                refreshAuthenticatedUserIfNecessary();
            }

            if (coreFeatures.isSegmentMetricsEnabled()) {
                segmentService.identify(apiTokenProvider.getAuthenticatedUser().getUserId());
            }

            if (tokens == null)
                tokens = apiTokenProvider.getAuthenticatedUser().getOAuth2Tokens();

            accessManager.grantAccess();

            if (!deviceRegistration.getFeatures().isAnalyticsUserAliased()) {
                operationQueue.setUserFeature(Features.ANALYTICS_USER_ALIASED, "true", apiTokenProvider.getAuthenticatedUser().getUserId());
                deviceRegistration.getFeatures().setUserFeature(new FeatureToggle(Features.ANALYTICS_USER_ALIASED, "true", true));
            }

            if (tokens.getScopes().contains(OAuth2.UBER_SCOPES)) {
                reduceScopeIfNeeded(tokens);
            }

            Ln.d("Done with device registration");
            return SyncState.SUCCEEDED;
        } else if (response.code() == 404) {
            deviceRegistration.setId(null);
            settings.setDeviceRegistration(deviceRegistration);
            return SyncState.READY;
        } else if (response.code() == 451) {
            // HTTP 451 Unavailable For Legal Reasons
            // Set the state to registered even though it wasn't a successful registration.
            // We've reached a terminal state with a 451 and there's no point in retrying.
            accessManager.revokeAccess();
            return SyncState.SUCCEEDED;
        } else if (response.code() == 400) {
            Ln.e(new RegisterDeviceException(NetworkUtils.getTrackingId(response)));
            return SyncState.READY;
        } else if ((response.code() == 500 || response.code() == 503) && allowMoreRetries) {
            Ln.e(new RegisterDeviceException(NetworkUtils.getTrackingId(response)));
            getRetryPolicy().addAttempt();
            return SyncState.READY;
        }
        return SyncState.READY;
    }

    private String getAuthorizationHeader() throws IOException {
        if (tokens != null) {
            return tokens.getAuthorizationHeader();
        } else {
            return apiTokenProvider.getAuthenticatedUser().getConversationAuthorizationHeader();
        }
    }

    /**
     * Request missing property we started collecting after the user authenticated.
     */
    private void refreshAuthenticatedUserIfNecessary() {
        try {
            AuthenticatedUser authenticatedUser = apiTokenProvider.getAuthenticatedUser();
            if (authenticatedUser.getOrgId() == null || authenticatedUser.getKey() == null || authenticatedUser.getCreated() == 0) {
                Ln.d("Requesting user update");
                retrofit2.Response<User> response = apiClientProvider.getUserClient().getUser(authenticatedUser.getConversationAuthorizationHeader()).execute();
                if (response.isSuccessful() && response.body() != null) {
                    User user = response.body();
                    authenticatedUser.setOrgId(user.getOrgId());
                    authenticatedUser.setUserId(user.getActorKey());
                    authenticatedUser.setCreated(user.createdInMillis());
                    apiTokenProvider.setAuthenticatedUser(authenticatedUser);
                }
            }
        } catch (Exception e) {
            Ln.d(e);
        }
    }

    private DeviceRegistration bodyToRegistration(ResponseBody body) throws IOException {
        if (body == null) {
            return null;
        }
        byte[] bodyBytes = body.bytes();

        Charset bodyCharset = body.contentType().charset();
        if (bodyCharset == null)
            bodyCharset = Charset.forName("utf-8");

        String data = new String(bodyBytes, bodyCharset);
        return gson.fromJson(data, DeviceRegistration.class);
    }

    @NonNull
    @Override
    public RetryPolicy buildRetryPolicy() {
        return RetryPolicy.newLimitAttemptsPolicy(10)
                    .withExponentialBackoff();
    }

    @Override
    protected void onStateChanged(SyncState oldState) {
        if (getState() == SyncState.SUCCEEDED) {
            if (!TestUtils.isInstrumentation()) {
                sendDeviceRegisteredMetrics();
            }
            bus.post(new DeviceRegistrationChangedEvent(deviceRegistration, clientCompatibilityHint, gcmRegistration));
        } else if (getState() == SyncState.FAULTED) {
            bus.post(new RegisterDeviceOperationFailedEvent());
        }
    }

    private void sendDeviceRegisteredMetrics() {
        GenericMetric deviceRegisteredEvent = GenericMetric.buildBehavioralAndOperationalMetric(ClientMetricNames.CLIENT_DEVICE_REGISTERED);

        deviceRegisteredEvent.addTag(ClientMetricTag.METRIC_TAG_IS_ROOTED, SystemUtils.isRooted());
        deviceRegisteredEvent.addTag(ClientMetricTag.METRIC_TAG_HAS_ANDROID_LOCKSCREEN, secureDevice.hasLockScreen());
        deviceRegisteredEvent.addTag(ClientMetricTag.METRIC_TAG_CLIENT_SECURITY_POLICY, deviceRegistration.getClientSecurityPolicy());
        deviceRegisteredEvent.addField(ClientMetricField.METRIC_FIELD_ANDROID_HARDWARE_BRAND, Strings.capitalize(Build.BRAND));
        deviceRegisteredEvent.addField(ClientMetricField.METRIC_FIELD_ANDROID_HARDWARE_MANUFACTURER, Strings.capitalize(Build.MANUFACTURER));
        deviceRegisteredEvent.addField(ClientMetricField.METRIC_FIELD_ANDROID_HARDWARE_MODEL, Strings.capitalize(Build.MODEL));
        if (UIUtils.hasMarshmallow()) {
            deviceRegisteredEvent.addField(ClientMetricField.METRIC_FIELD_ANDROID_ISDEVICESECURE, secureDevice.isDeviceSecure());
        }
        deviceRegisteredEvent.addField(ClientMetricField.METRIC_FIELD_ANDROID_ISKEYGUARDSECURE, secureDevice.isKeyguardSecure());

        operationQueue.postGenericMetric(deviceRegisteredEvent);
    }

    @Override
    protected void onNewOperationEnqueued(Operation newOperation) {
    }

    @Override
    public boolean isOperationRedundant(Operation newOperation) {
        return newOperation.getOperationType() == REGISTER_DEVICE;
    }

    @NonNull
    @Override
    protected SyncState checkProgress() {
        return SyncState.READY;
    }

    public static class RegisterDeviceException extends IOException {

        public RegisterDeviceException(String trackingId) {
            super(trackingId);
        }
    }
}
