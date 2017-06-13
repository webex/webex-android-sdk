package com.cisco.spark.android.core;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.ConditionVariable;

import com.cisco.spark.android.BuildConfig;
import com.cisco.spark.android.app.NotificationManager;
import com.cisco.spark.android.authenticator.ApiTokenProvider;
import com.cisco.spark.android.authenticator.AuthenticatedEvent;
import com.cisco.spark.android.authenticator.AuthenticatedUserProvider;
import com.cisco.spark.android.authenticator.LogoutEvent;
import com.cisco.spark.android.authenticator.NotAuthenticatedException;
import com.cisco.spark.android.authenticator.OAuth2;
import com.cisco.spark.android.callcontrol.CallHistoryService;
import com.cisco.spark.android.client.UrlProvider;
import com.cisco.spark.android.events.DeviceRegistrationChangedEvent;
import com.cisco.spark.android.events.OAuth2ErrorResponseEvent;
import com.cisco.spark.android.events.UIBackgroundTransition;
import com.cisco.spark.android.events.UIForegroundTransition;
import com.cisco.spark.android.locus.events.ResetEvent;
import com.cisco.spark.android.locus.service.LocusService;
import com.cisco.spark.android.log.Lns;
import com.cisco.spark.android.log.LogFilePrint;
import com.cisco.spark.android.lyra.LyraService;
import com.cisco.spark.android.media.MediaEngine;
import com.cisco.spark.android.mercury.MercuryClient;
import com.cisco.spark.android.metrics.MetricsReporter;
import com.cisco.spark.android.model.KeyManager;
import com.cisco.spark.android.reachability.NetworkReachabilityChangedEvent;
import com.cisco.spark.android.reachability.UIServiceAvailability;
import com.cisco.spark.android.sdk.SdkClient;
import com.cisco.spark.android.sync.ActorRecordProvider;
import com.cisco.spark.android.sync.ConversationSyncQueueAdapter;
import com.cisco.spark.android.sync.SearchManager;
import com.cisco.spark.android.sync.operationqueue.core.OperationQueue;
import com.cisco.spark.android.sync.queue.ConversationSyncQueue;
import com.cisco.spark.android.ui.call.VideoMultitaskComponent;
import com.cisco.spark.android.util.Action;
import com.cisco.spark.android.util.CpuLogger;
import com.cisco.spark.android.util.LinusReachabilityService;
import com.cisco.spark.android.util.LocationManager;
import com.cisco.spark.android.util.LoggingLock;
import com.cisco.spark.android.util.SafeAsyncTask;
import com.cisco.spark.android.util.UIUtils;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.cisco.spark.android.wdm.RegisterDeviceOperation;
import com.cisco.spark.android.wdm.WdmClient;
import com.cisco.spark.android.whiteboard.WhiteboardCache;
import com.cisco.spark.android.whiteboard.WhiteboardService;
import com.github.benoitdion.ln.Ln;
import com.github.benoitdion.ln.NaturalLog;
import com.webex.wme.MediaSessionAPI;

import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import de.greenrobot.event.EventBus;
import retrofit.RetrofitError;

/**
 * The ApplicationController handles the mgrState of the application.
 */
public class ApplicationController {
    private static final String STATE_TAG = "ApplicationController - state";

    private final ConditionVariable unregisterCondition = new ConditionVariable();
    private final static int NON_INTRUSIVE_UI_TIMEOUT = 1000;

    private final Context context;
    private NaturalLog ln;
    private final ApiClientProvider clientProvider;
    private final ApiTokenProvider tokenProvider;
    private final AuthenticatedUserProvider userProvider;
    private final DeviceRegistration deviceRegistration;
    private final BackgroundCheck backgroundCheck;
    private final Settings settings;
    private final MediaEngine mediaEngine;
    private final NotificationManager notificationManager;
    private final AccessManager accessManager;
    private final UIServiceAvailability uiServiceAvailability;
    private final LocationManager locationManager;
    private final EventBus bus;
    private final SearchManager searchManager;
    private final OperationQueue operationQueue;
    private final Injector injector;
    private final AccountUi accountUi;
    private final SdkClient sdkClient;
    private PermissionsHelper permissionsHelper;
    private final LogFilePrint log;
    private final UrlProvider urlProvider;
    private final WhiteboardCache whiteboardCache;

    private LoggingLock syncLock;

    private Map<Class, WeakReference<Component>> components = new LinkedHashMap<>();

    public enum State {
        NONE,
        REGISTERING,
        REGISTERED,
        UNREGISTERED,
        STARTING,
        STARTED,
        STOPPING,
        STOPPED,
        BAD
    }

    //$REVIEW is this boolean needed?
    private boolean registered;
    private boolean running;
    private State state = State.NONE;
    private Runnable outstandingHomeActivityRunnable;

    private ConversationSyncQueueAdapter conversationSyncQueueAdapter;
    private MercuryClient mercuryClient;

    public ApplicationController(final Context context, final ApiClientProvider clientProvider, final ApiTokenProvider tokenProvider,
                                 final AuthenticatedUserProvider userProvider, final EventBus bus, final DeviceRegistration deviceRegistration,
                                 final BackgroundCheck backgroundCheck, final Settings settings, final MediaEngine mediaEngine,
                                 final ActorRecordProvider actorRecordProvider, final MetricsReporter metricsReporter,
                                 final StatusManager statusManager, final LocationManager locationManager, MercuryClient mercuryClient,
                                 final SearchManager searchManager, final LocusService locusService, final CallHistoryService callHistoryService,
                                 final CpuLogger cpuLogger, final ConversationSyncQueue conversationSyncQueue, NotificationManager notificationManager, AccessManager accessManager,
                                 final KeyManager keyManager, UIServiceAvailability uiServiceAvailability, OperationQueue operationQueue, Injector injector,
                                 final VideoMultitaskComponent videoMultitaskComponent, final Ln.Context lnContext, final AccountUi accountUi, final LogFilePrint log,
                                 final LinusReachabilityService linusReachabilityService, final WhiteboardService whiteboardService,
                                 final LyraService lyraService, final UrlProvider urlProvider, final SdkClient sdkClient, final WhiteboardCache whiteboardCache) {
        this.context = context;
        this.clientProvider = clientProvider;
        this.tokenProvider = tokenProvider;
        this.userProvider = userProvider;
        this.deviceRegistration = deviceRegistration;
        this.backgroundCheck = backgroundCheck;
        this.settings = settings;
        this.mediaEngine = mediaEngine;
        this.locationManager = locationManager;
        this.bus = bus;
        this.searchManager = searchManager;
        this.notificationManager = notificationManager;
        this.accessManager = accessManager;
        this.uiServiceAvailability = uiServiceAvailability;
        this.operationQueue = operationQueue;
        this.injector = injector;
        this.accountUi = accountUi;
        this.sdkClient = sdkClient;
        this.syncLock = new LoggingLock(BuildConfig.DEBUG, "ApplicationControllerLock");
        this.permissionsHelper = new PermissionsHelper(context);
        this.ln = Ln.get(lnContext);
        this.log = log;
        this.urlProvider = urlProvider;
        this.whiteboardCache = whiteboardCache;

        registered = false;

        bus.register(this);

        conversationSyncQueueAdapter = new ConversationSyncQueueAdapter(context, settings, operationQueue, bus, keyManager, injector, deviceRegistration);

        this.mercuryClient = mercuryClient;

        metricsReporter.setApplicationController(this);
        statusManager.setApplicationController(this);
        locationManager.setApplicationController(this);
        mercuryClient.setApplicationController(this);
        locusService.setApplicationController(this);
        callHistoryService.setApplicationController(this);
        cpuLogger.setApplicationController(this);
        videoMultitaskComponent.setApplicationController(this);
        linusReachabilityService.setApplicationController(this);
        whiteboardService.setApplicationController(this);
        lyraService.setApplicationController(this);
        conversationSyncQueue.setApplicationController(this);
        whiteboardCache.setApplicationController(this);
    }

    public boolean isStarted() {
        boolean result = registered && state == State.STARTED;
        ln.v("ApplicationController reporting isStarted: %b", result);
        return result;
    }

    public MercuryClient getMercuryClient() {
        return mercuryClient;
    }

    public boolean isStarting() {
        return state == State.STARTING;
    }

    public boolean isStopped() {
        return state == State.STOPPED;
    }

    public boolean isRegistered() {
        return registered && (state == State.REGISTERED || state == State.STARTING || state == State.STARTED);
    }

    public void start() {
        syncLock.lock();
        try {
            running = true;

            ln.i("ApplicationController - start(%s)", state.name());

            if (state == State.REGISTERING) {
                ln.i("ApplicationController - in %s state on start; ignoring start request", state.name());
                return;
            }
            if (state != State.UNREGISTERED && state != State.NONE && state != State.STARTED) {
                ln.w("ApplicationController - trying to start but not in expected state '%s'", state.name());
                setState(State.BAD);
                return;
            }

            backgroundCheck.start();
            registered = false;

            if (permissionsHelper.hasCameraPermission() && permissionsHelper.hasMicrophonePermission()) {
                initializeWme();
            }

            try {
                searchManager.checkForUpdate();
            } catch (Throwable t) {
                Ln.e("Failed checking search manager");
            }

            try {
                uiServiceAvailability.update();
            } catch (Throwable t) {
                Ln.e("Failed updating uiServiceAvailability");
            }

            if (!backgroundCheck.isInBackground()) {
                Ln.d("Submitting register device operation");
                operationQueue.submit(new RegisterDeviceOperation(injector));
                setState(State.REGISTERING);
            } else {
                Ln.d("App is in the background, skipping registration");
            }
        } finally {
            syncLock.unlock();
        }
    }

    public void stop() {
        syncLock.lock();
        try {
            running = false;
            if (state == State.STOPPING || state == State.STOPPED || state == State.UNREGISTERED) {
                ln.d("ApplicationController - faithfully refusing to perform a stop when one is in progress");
                return;
            }
            stopComponents();
            backgroundCheck.stop();
            deviceRegistration.getFeatures().clear();
        } finally {
            syncLock.unlock();
        }
    }

    public void register(Component component) {
        syncLock.lock();
        try {
            if (!components.containsKey(component.getClass())) {
                ln.i("ApplicationController: %s registered", component.getClass());
                components.put(component.getClass(), new WeakReference<>(component));
                if (state == State.STARTED && component.shouldStart()) {
                    component.start();
                }
            }
        } finally {
            syncLock.unlock();
        }
    }

    public void unregister(Component component) {
        syncLock.lock();
        try {
            if (components.containsKey(component.getClass())) {
                ln.i("ApplicationController - %s unregistered", component.getClass());
                components.remove(component.getClass());
                if (state == State.STARTED) {
                    component.stop();
                }
            } else {
                ln.v("ApplicationController - %s unregistered with when not registered", component.getClass());
            }
        } finally {
            syncLock.unlock();
        }
    }

    public void initializeWme() {
        // Initialize the ME for a 'signed in user'. This was previously done in SquaredApp::create(),
        // but created problems during testing, where multiple instances (processes) of the app were
        // created.  This caused bad synchronization issues!  So be very careful, if moving this call!
        try {
            mediaEngine.initialize();
            mediaEngine.setLoggingLevel(MediaSessionAPI.TraceLevelMask.TRACE_LEVEL_MASK_INFO);
        } catch (Throwable t) {
            Ln.e(t, "Failed initializing WME");
        }
    }

    private void loopComponents(Action<Component> action) {
        Set<Class> cleanupSet = new HashSet<>();
        for (Class key : components.keySet()) {
            WeakReference<Component> componentReference = components.get(key);
            Component component = componentReference.get();
            if (component == null) {
                cleanupSet.add(key);
            } else {
                try {
                    action.call(component);
                } catch (NotAuthenticatedException e) {
                    ln.i(e);
                } catch (Exception e) {
                    ln.e(e);
                }
            }
        }
        // Clean up those instances that went away.
        if (cleanupSet.size() != 0) {
            for (Class key : cleanupSet) {
                components.remove(key);
            }
        }
    }

    private void startComponents() {

        if (sdkClient.conversationCachingEnabled()) {
            conversationSyncQueueAdapter.register();
        }

        syncLock.lock();
        try {
            if (state == State.STARTING || state == State.STARTED) {
                ln.i("ApplicationController - Ignoring redundant call to startComponents");
                return;
            }
            setState(State.STARTING);
            loopComponents(new Action<Component>() {
                @Override
                public void call(Component component) {
                    ln.d("ApplicationController - Starting %s", component.getClass());
                    if (component.shouldStart()) {
                        component.start();
                    }
                }
            });

            setState(State.STARTED);
        } finally {
            syncLock.unlock();
        }
    }

    private void stopComponents() {
        syncLock.lock();
        try {
            if (state == State.NONE || state == State.STOPPING || state == State.STOPPED) {
                ln.w("ApplicationController - already stopping. Refusing.");
                return;
            }

            if (state != State.STARTED) {
                ln.w("ApplicationController - not in proper state to stop components, but doing so anyway. state=%s", state.name());
                // Something wants them stopped; we'll make them stopped.
            }
            setState(State.STOPPING);
            loopComponents(new Action<Component>() {
                @Override
                public void call(Component component) {
                    ln.d("ApplicationController - Stopping %s", component.getClass());
                    component.stop();
                }
            });
            setState(State.STOPPED);
        } finally {
            syncLock.unlock();
        }
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEventAsync(UIBackgroundTransition event) {
        ln.i("ApplicationController - BackgroundTransition()");
        if (state == State.STARTED || state == State.REGISTERING) {
            stopComponents();
        } else {
            ln.w("ApplicationController - background transition: not in expected state '%s'", state.name());
        }
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEventAsync(UIForegroundTransition event) {
        ln.i("ApplicationController - ForegroundTransition()");
        if (state == State.BAD || state == State.REGISTERING || state == State.STARTED || state == State.STOPPED || state == State.NONE) {
            reset();
        } else {
            ln.w("ApplicationController - foreground transition: not in expected state '%s'", state.name());
        }
    }

    private void clear() {
        settings.clear();
        tokenProvider.clearAccount();
        deviceRegistration.getFeatures().clear();
        conversationSyncQueueAdapter.clear();
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEvent(RequestLogoutEvent event) {
        logout(null, true);
    }

    public void logout(Activity activity, boolean isTeardown) {
        logout(activity, isTeardown, true);
    }

    public void logout(Activity activity, boolean isTeardown, boolean clearAccount) {
        if (state == State.NONE || state == State.UNREGISTERED) {
            ln.w("ApplicationController - None or already unregistered. Ignoring logout request");
            return;
        }

        ln.i("ApplicationController - logout");
        stop();

        accessManager.reset();
        new UnregisterDeviceTask().execute();

        boolean result = unregisterCondition.block(NON_INTRUSIVE_UI_TIMEOUT);
        if (!result) {
            // It is perfectly valid for the condition to not open. We log just for
            // convenience. The logcat should show that the unregister did eventually 'complete'.
            ln.w("ApplicationController - failed receiving response from delete device ID in timeout period");
        }

        if (clearAccount) {
            new SafeAsyncTask<Void>() {
                @Override
                public Void call() throws Exception {
                    clear();
                    return null;
                }
            }.execute();
        }

        deviceRegistration.reset(urlProvider);
        setState(State.UNREGISTERED);

        uiLogout(activity, isTeardown, clearAccount);
    }

    public void sslErrorReLogin(Activity activity, boolean isTeardown) {
        new SafeAsyncTask<Void>() {
            @Override
            public Void call() throws Exception {
                settings.clear();
                return null;
            }
        }.execute();

        uiLogout(activity, isTeardown, false);
    }

    private void uiLogout(Activity activity, boolean isTeardown, boolean clearAccount) {

        // Note: Consumers of this event must guarantee no persistent writes of personal data after
        // returning from onEvent(LogoutEvent).
        bus.post(new LogoutEvent());

        accountUi.logout(context, activity, isTeardown);

        if (clearAccount) {
            // Clearing again here in case anything was in flight during the logout event.
            clear();
        }

        log.clearLogs();
    }

    public void readyForHomeActivity(Runnable runnable) {
        outstandingHomeActivityRunnable = runnable;
        if (registered && (state == State.STARTED)) {
            startHomeActivity();
        }
    }

    public void readyForHomeActivity(final boolean isAdded, final Activity activity) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (isAdded) {
                    bus.post(new AuthenticatedEvent());
                    accountUi.showHome(activity);
                }
                UIUtils.removeTransition(activity);
            }
        };
        readyForHomeActivity(runnable);
    }

    private void startHomeActivity() {
        if (outstandingHomeActivityRunnable != null && (registered && (state == State.STARTED))) {
            outstandingHomeActivityRunnable.run();
        }
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEventAsync(OAuth2ErrorResponseEvent event) {
        onOAuth2ErrorResponse(event.getUri(), event.getCode());
    }

    private void onOAuth2ErrorResponse(Uri uri, int code) {
        switch (code) {
            case HttpURLConnection.HTTP_BAD_REQUEST: // 400
            case HttpURLConnection.HTTP_UNAUTHORIZED: // 401
            case HttpURLConnection.HTTP_FORBIDDEN: // 403
                if (OAuth2.isOAuth2Uri(uri)) {
                    ln.w("Failed sending auth to host. Logging out");
                    logout(null, false);
                }
            default:
                break;
        }
    }

    public LocationManager getLocationManager() {
        return locationManager;
    }

    private void setState(State state) {
        syncLock.lock();
        try {
            Lns.application().i("%s: %s->%s", STATE_TAG, this.state.name(), state.name());
            this.state = state;
        } finally {
            syncLock.unlock();
        }
    }

    private class UnregisterDeviceTask extends SafeAsyncTask<Object> {
        @Override
        public Object call() throws Exception {
            WdmClient wdmClient = clientProvider.getWdmClient();
            if (wdmClient != null) {
                try {
                    if (deviceRegistration.getId() != null)
                        wdmClient.deleteDevice(tokenProvider.getAuthenticatedUser().getConversationAuthorizationHeader(), deviceRegistration.getId());
                    else
                        ln.i("Ignoring request to unregister deviceRegistration with null id");
                } catch (RetrofitError ex) {
                    // We likely don't want to retry, as unregistering is not something that is required
                    // of a good WDM citizen; log it.
                    ln.i(ex);
                    retrofit.client.Response response = ex.getResponse();
                    if (response == null) {
                        ln.i("ApplicationController - unregister device resulted in: %s", ex.getMessage());
                    } else {
                        ln.i("ApplicationController - unregister device resulted in: %d - %s", response.getStatus(), response.getReason());
                    }
                } catch (NotAuthenticatedException ex) {
                    // Okay, so the user isn't authenticated any more, we just didn't delete the
                    // current session. Is an okay situation; log it.
                    ln.i(ex, "ApplicationController - unregistering device resulted in a NotAuthenticatedException.");
                } catch (Exception ex) {
                    ln.i(ex, "ApplicationController - unregistering device received an exception: %s'", ex.getMessage());
                }
                unregisterCondition.open();
            } else {
                ln.e("ApplicationController - failed getting service client instance. ApplicationController is dead in the water");
                setState(State.BAD);
            }
            // Regardless of outcome we want to open the condition variable.
            ln.d("ApplicationController - Opening unregister condition");
            unregisterCondition.open();
            return null;
        }
    }

    public void reset() {
        if (!running)
            return;

        // Active-ness can be updated by the same EventBus event that brought us here. Allowing a little
        // time for the BackgroundCheck to catch up ensures correct behavior regardless of what order
        // events are delivered in.
        if (!backgroundCheck.waitForActiveState(500)) {
            ln.i("ApplicationController received ResetEvent while in background; ignoring.");
            return;
        }

        if (!userProvider.isAuthenticated()) {
            ln.d("ApplicationController - Attempting to reset the application controller but user is not authenticated.");
            return;
        }

        syncLock.lock();
        try {
            if (state == State.UNREGISTERED || state == State.NONE || state == State.BAD) {
                ln.i("ApplicationController received ResetEvent while in the %s state.", state.name());
            }

            if (state == State.REGISTERED || (state == State.STARTED)) {
                stopComponents();
            }

            registered = false;
            Ln.d("Submitting register device operation");
            operationQueue.submit(new RegisterDeviceOperation(injector));
            setState(State.REGISTERING);
        } finally {
            syncLock.unlock();
        }
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEventAsync(ResetEvent event) {
        reset();
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEventAsync(DeviceRegistrationChangedEvent event) {
        syncLock.lock();
        try {
            if (!running)
                return;
            if (event.wasTriggeredByGcmRegistration()) {
                Ln.i("Gcm registration successful (registered = %s state = %s)", registered, state);
                return;
            }
            Ln.d("Device registration successful");
            registered = true;
            startComponents();
        } finally {
            syncLock.unlock();
        }
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEventAsync(NetworkReachabilityChangedEvent event) {
        ln.i("ApplicationController received NetworkReachabilityChangedEvent event, in background = %s networkIsConnected = %s", backgroundCheck.isInBackground(), event.isConnected());
        // No need to wait; network connectivity doesn't affect active-ness. Just see if we're active.
        if (backgroundCheck.waitForActiveState(0)) {
            ln.i("ApplicationController event.isConnected() = %s", event.isConnected());
            if (event.isConnected()) {
                Ln.i("Network connected. Resetting application controller");
                reset();
            } else {
                ln.i("Connectivity lost. Stop components.");
                stopComponents();
            }
        }
    }

    // For testing
    public State getState() {
        return state;
    }
}
