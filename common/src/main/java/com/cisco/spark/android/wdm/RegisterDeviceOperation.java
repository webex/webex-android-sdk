package com.cisco.spark.android.wdm;

import android.support.annotation.NonNull;

import com.cisco.spark.android.authenticator.ApiTokenProvider;
import com.cisco.spark.android.authenticator.OAuth2;
import com.cisco.spark.android.authenticator.OAuth2Tokens;
import com.cisco.spark.android.core.AccessManager;
import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.AuthenticatedUser;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.core.Settings;
import com.cisco.spark.android.events.DeviceRegistrationChangedEvent;
import com.cisco.spark.android.model.User;
import com.cisco.spark.android.sync.operationqueue.AbstractOAuth2Operation;
import com.cisco.spark.android.sync.operationqueue.core.Operation;
import com.cisco.spark.android.sync.operationqueue.core.RetryPolicy;
import com.github.benoitdion.ln.Ln;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.UUID;

import javax.inject.Inject;

import dagger.Lazy;
import de.greenrobot.event.EventBus;
import retrofit.RetrofitError;
import retrofit.client.Header;
import retrofit.client.Response;
import retrofit.mime.MimeUtil;
import retrofit.mime.TypedByteArray;
import retrofit.mime.TypedInput;

import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.OperationType;
import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.OperationType.REGISTER_DEVICE;
import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncState;

public class RegisterDeviceOperation extends AbstractOAuth2Operation {

    private final boolean gcmRegistration;

    @Inject
    transient ApiTokenProvider apiTokenProvider;

    @Inject
    transient ApiClientProvider apiClientProvider;

    @Inject
    transient DeviceRegistration deviceRegistration;

    @Inject
    transient AccessManager accessManager;

    @Inject
    transient Settings settings;

    @Inject
    transient EventBus bus;

    @Inject
    transient Gson gson;

    @Inject
    transient Lazy<DeviceInfo> deviceInfoProvider;

    private OAuth2Tokens tokens;
    private String clientCompatibilityHint;

    public RegisterDeviceOperation(Injector injector) {
        this(injector, false);
    }

    public RegisterDeviceOperation(Injector injector, OAuth2Tokens tokens) {
        this(injector, false);
        this.tokens = tokens;
    }

    public RegisterDeviceOperation(Injector injector, boolean gcmRegistration) {
        super(injector);
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

        Response response;

        try {
            if (deviceRegistration.getId() == null) {
                Ln.d("Creating new device");
                // Register the device as a TP_ENDPOINT if the user is a machine to be able to have
                // a test user fake a room
                AuthenticatedUser authenticatedUser = apiTokenProvider.getAuthenticatedUserOrNull();
                if (authenticatedUser != null && authenticatedUser.isMachine()) {
                    if (!deviceInfo.getDeviceType().equals(DeviceInfo.SPARKBOARD_DEVICE_TYPE)) {
                        Ln.i("Override deviceType for machine user '%s', type was %s but will be registered as a TP_ENDPOINT",
                             authenticatedUser.getDisplayName(), deviceInfo.getDeviceType());
                        deviceInfo.setDeviceType(DeviceInfo.TP_DEVICE_TYPE);
                    }
                    deviceInfo.setDeviceManaged(true);
                    deviceInfo.setDeviceID(UUID.randomUUID());
                }
                response = wdmClient.createDevice(getAuthorizationHeader(), deviceInfo);
            } else {
                Ln.d("Updating device");
                response = wdmClient.updateDevice(getAuthorizationHeader(), deviceRegistration.getId(), deviceInfo);
            }
        } catch (RetrofitError error) {
            Ln.d(error);
            if (error.getKind() != RetrofitError.Kind.HTTP)
                throw error;
            response = error.getResponse();
            if (response == null)
                throw error;
        }

        Ln.d("Got response with status code %d", response.getStatus());
        if (response.getStatus() >= 200 && response.getStatus() < 300) {
            deviceRegistration.populateFrom(bodyToRegistration(response.getBody()));
            settings.setDeviceRegistration(deviceRegistration);

            // Find client-compatibility-hint header and check for required update
            for (Header header : response.getHeaders()) {
                if ("client-compatibility-hint".equalsIgnoreCase(header.getName())) {
                    clientCompatibilityHint = header.getValue();
                    break;
                }
            }

            if (!apiTokenProvider.isAuthenticated()) {
                apiTokenProvider.requestAuthenticatedUser(tokens, apiClientProvider, getAuthorizationHeader());
            } else {
                refreshAuthenticatedUserIfNecessary();
            }

            if (tokens == null)
                tokens = apiTokenProvider.getAuthenticatedUser().getOAuth2Tokens();

            accessManager.grantAccess();

            if (!deviceRegistration.getFeatures().isAnalyticsUserAliased()) {
                operationQueue.postAliasUser(settings.getPreloginUserId());
                operationQueue.setUserFeature(Features.ANALYTICS_USER_ALIASED, "true", apiTokenProvider.getAuthenticatedUser().getUserId());
                deviceRegistration.getFeatures().setUserFeature(new FeatureToggle(Features.ANALYTICS_USER_ALIASED, "true", true));
            }

            if (tokens.getScopes().contains(OAuth2.UBER_SCOPES)) {
                reduceScopeIfNeeded(tokens);
            }

            settings.removePreloginUserId();

            Ln.d("Done with device registration");
            return SyncState.SUCCEEDED;
        } else if (response.getStatus() == 404) {
            deviceRegistration.setId(null);
            settings.setDeviceRegistration(deviceRegistration);
            return SyncState.READY;
        } else if (response.getStatus() == 451) {
            // HTTP 451 Unavailable For Legal Reasons
            // Set the state to registered even though it wasn't a successful registration.
            // We've reached a terminal state with a 451 and there's no point in retrying.
            accessManager.revokeAccess();
            return SyncState.SUCCEEDED;
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

    private DeviceRegistration bodyToRegistration(TypedInput body) {
        if (body == null) {
            return null;
        }
        byte[] bodyBytes = ((TypedByteArray) body).getBytes();
        String bodyMime = body.mimeType();
        String bodyCharset = MimeUtil.parseCharset(bodyMime, "utf-8");
        try {
            String data = new String(bodyBytes, bodyCharset);
            return gson.fromJson(data, DeviceRegistration.class);
        } catch (UnsupportedEncodingException e) {
            Ln.e(e);
        }
        return null;
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
            bus.post(new DeviceRegistrationChangedEvent(deviceRegistration, clientCompatibilityHint, gcmRegistration));
        }
    }

    @Override
    protected void onNewOperationEnqueued(Operation newOperation) {
    }

    @Override
    public boolean isOperationRedundant(Operation newOperation) {
        return newOperation.getOperationType() == REGISTER_DEVICE;
    }

    @Override
    protected SyncState checkProgress() {
        return SyncState.READY;
    }
}
