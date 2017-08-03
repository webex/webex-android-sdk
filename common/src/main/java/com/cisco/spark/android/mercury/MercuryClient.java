package com.cisco.spark.android.mercury;

import android.net.Uri;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.ApplicationController;
import com.cisco.spark.android.core.Component;
import com.cisco.spark.android.core.ServiceHosts;
import com.cisco.spark.android.locus.events.ResetEvent;
import com.cisco.spark.android.mercury.events.ConversationActivityEvent;
import com.cisco.spark.android.mercury.events.WhiteboardActivityEvent;
import com.cisco.spark.android.processing.ActivityListener;
import com.cisco.spark.android.sync.operationqueue.core.OperationQueue;
import com.cisco.spark.android.util.Sanitizer;
import com.cisco.spark.android.util.Strings;
import com.cisco.spark.android.util.TestUtils;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.cisco.spark.android.whiteboard.WhiteboardService;
import com.github.benoitdion.ln.Ln;
import com.github.benoitdion.ln.NaturalLog;
import com.google.gson.Gson;

import java.util.ArrayList;

import de.greenrobot.event.EventBus;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class MercuryClient implements Component {
    private final ApiClientProvider apiClientProvider;
    private final Gson gson;
    private final EventBus bus;
    private final DeviceRegistration deviceRegistration;
    private final ActivityListener activityListener;
    protected final NaturalLog ln;
    protected final WhiteboardService whiteboardService;
    protected final Sanitizer sanitizer;

    private final OperationQueue operationQueue;

    protected MercuryWebsocketListener websocketListener;
    protected okhttp3.WebSocket webSocket;
    private final Object syncLock = new Object();
    protected ApplicationController applicationController;
    private long lastPing;
    protected boolean forcedStop;
    private Uri uriOverride;
    private Uri mercuryConnectionServiceClusterUrl;

    private ServiceHosts serviceHosts;

    private final boolean isPrimary;
    private boolean isConnected;

    public MercuryClient(boolean primary, ApiClientProvider apiClientProvider, Gson gson, EventBus bus,
                         DeviceRegistration deviceRegistration, ActivityListener activityListener, Ln.Context lnContext,
                         WhiteboardService whiteboardService, OperationQueue operationQueue, Sanitizer sanitizer) {

        this.apiClientProvider = apiClientProvider;
        this.gson = gson;
        this.bus = bus;
        this.deviceRegistration = deviceRegistration;
        this.activityListener = activityListener;
        this.ln = Ln.get(lnContext, getName());
        this.whiteboardService = whiteboardService;
        this.operationQueue = operationQueue;
        this.sanitizer = sanitizer;

        this.isPrimary = primary;

        if (this.deviceRegistration.getServiceHostMap() != null) {
            Uri mercuryLink = deviceRegistration.getServiceHostMap().getServiceLink("mercuryConnection");

            this.serviceHosts = new ServiceHosts(deviceRegistration.getServiceHostMap().getServiceHost(mercuryLink.getHost()));
        } else {
            this.serviceHosts = new ServiceHosts(new ArrayList<>());
        }
    }

    @NonNull
    protected String getName() {
        return "Mercury";
    }

    public Uri getMercuryConnectionServiceClusterUrl() {
        return mercuryConnectionServiceClusterUrl;
    }

    public void setMercuryConnectionServiceClusterUrl(Uri mercuryConnectionServiceClusterUrl) {
        this.mercuryConnectionServiceClusterUrl = mercuryConnectionServiceClusterUrl;
    }

    public Uri getPrimaryMercuryWebSocketUrl() {
        return deviceRegistration.getWebSocketUrl();
    }

    @Override
    public boolean shouldStart() {
        return true;
    }

    @Override
    public void start() {
        ln.i("Starting " + getName());

        synchronized (syncLock) {
            forcedStop = false;

            if (isRunning()) {
                Ln.w(new RuntimeException("Mercury is already running"));
                return;
            }

            Uri webSocketUri;
            Request request;

            if (uriOverride != null) {
                webSocketUri = uriOverride;
                ln.d("webSocketUri (override): %s", webSocketUri);
            } else {
                webSocketUri = deviceRegistration.getWebSocketUrl();
                ln.d("webSocketUri: %s", webSocketUri);
            }

            request = buildMercuryWebSocketRequest(webSocketUri);

            websocketListener = buildMercuryWebSocketListener();
            OkHttpClient client = apiClientProvider.buildOkHttpClient(null, deviceRegistration, null);
            webSocket = client.newWebSocket(request, websocketListener);
            client.dispatcher().executorService().shutdown();
        }
    }

    @NonNull
    protected Request buildMercuryWebSocketRequest(Uri webSocketUri) {
        Uri updatedWebSocketUri = webSocketUri;

        if (uriOverride == null && serviceHosts != null && serviceHosts.getHost() != null) {
            String originalWebSocketUri = webSocketUri.toString();
            String newHost = serviceHosts.getHost();
            String newWebSocketUri = originalWebSocketUri.replace(webSocketUri.getHost(), newHost);

            updatedWebSocketUri = Uri.parse(newWebSocketUri);
        }

        if (deviceRegistration.getFeatures().isBufferedMercuryEnabled())
            updatedWebSocketUri = updatedWebSocketUri.buildUpon()
                    .appendQueryParameter("mercuryRegistrationStatus", "true")
                    .appendQueryParameter("isAckSupported", "true")
                    .build();

        return new Request.Builder().url(updatedWebSocketUri.toString()).build();
    }

    @NonNull
    protected MercuryWebsocketListener buildMercuryWebSocketListener() {
        return new MercuryWebsocketListener();
    }

    private boolean isSecuredWebSocketUri(Uri uri) {
        return uri.getScheme().startsWith("wss");
    }

    @Override
    public void stop() {
        synchronized (syncLock) {
            forcedStop = true;
            if (webSocket != null) {
                ln.i("Stopping %s client.", getName());
                webSocket.close(1000, "Component Stopping");
                webSocket = null;
            }
        }
    }

    public boolean isRunning() {
        synchronized (syncLock) {
            return webSocket != null && isConnected;
        }
    }

    public void setApplicationController(ApplicationController applicationController) {
        applicationController.register(this);
        this.applicationController = applicationController;
    }

    public void logState() {
        ln.v("WebSocket is null: %b", webSocket == null);
        ln.v("WebSocket is connected: %b", isConnected);
        ln.v("WebSocket uri: %s", deviceRegistration.getWebSocketUrl());
    }

    public void send(String message) {
        if (webSocket != null) {
            Ln.d("Send " + message);
            webSocket.send(message);
        }
    }

    public void setUriOverride(Uri uriOverride) {
        this.uriOverride = uriOverride;
    }

    public long getLastPing() {
        return lastPing;
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    protected class MercuryWebsocketListener extends WebSocketListener {
        private String trackingId;

        @Override
        public void onOpen(okhttp3.WebSocket webSocket, Response response) {
            trackingId = response.header("TrackingID");
            ln.i("Mercury connection opened. handshake: %s - %s TrackingId: %s", response.code(), response.message(), trackingId);
            bus.post(new MercuryClient.MercuryConnectedEvent());
            isConnected = true;
        }

        @Override
        public void onMessage(okhttp3.WebSocket webSocket, String message) {
            ln.d("Received message: %s", sanitizer.sanitize(message));
            lastPing = SystemClock.elapsedRealtime();
            MercuryEnvelope envelope = gson.fromJson(message, MercuryEnvelope.class);
            if (envelope != null) {
                if (envelope.getData() != null && !Strings.isEmpty(envelope.getData().getEventType().toString())) {
                    if (envelope.getData() instanceof ConversationActivityEvent) {
                        handleConversationActivityEvent(envelope);
                    } else if (envelope.getData() instanceof WhiteboardActivityEvent) {
                        WhiteboardActivityEvent whiteboardActivityEvent = getWhiteboardActivityEnvent(envelope);
                        whiteboardService.mercuryEvent(whiteboardActivityEvent);
                    } else if (envelope.getData() instanceof MercuryRegistration) {
                        handleRegistrationEvent(envelope);
                    } else {
                        bus.post(envelope.getData());
                    }
                } else {
                    ln.w("Invalid message envelope.");
                }
                webSocket.send(gson.toJson(new AckMessage(envelope.getId())));
            }
        }

        @Override
        public void onMessage(okhttp3.WebSocket webSocket, ByteString bytes) {
            onMessage(webSocket, new String(bytes.toByteArray()));
        }

        @Override
        public void onClosing(okhttp3.WebSocket webSocket, int code, String reason) {
            ln.i("Connection closing. Reason: \"%s\", code: %d (%s), last ping: %d TrackingId: %s", reason, code,
                    MercuryClient.WebSocketStatusCodes.valueForCode(code).name(), lastPing, trackingId);
            webSocket.close(code, reason);
        }

        @Override
        public void onClosed(okhttp3.WebSocket webSocket, int code, String reason) {
            super.onClosed(webSocket, code, reason);

            ln.i("Connection closed. Reason: \"%s\", code: %d (%s), last ping: %d TrackingId: %s", reason, code,
                    MercuryClient.WebSocketStatusCodes.valueForCode(code).name(), lastPing, trackingId);

            isConnected = false;

            if (forcedStop)
                return;

            if (shouldConsiderRetry()) {
                // Status Code Definitions: https://tools.ietf.org/html/rfc6455#section-7.4.1
                switch (MercuryClient.WebSocketStatusCodes.valueForCode(code)) {
                    case CLOSE_REPLACED:
                        ln.w("Connection was replaced, safe to ignore");
                        break;
                    case CLOSE_UNKNOWN:
                        ln.w("Connection closed with unknown code %d", code);
                    case CLOSE_NORMAL:
                    case CLOSE_LOCAL_ERROR:
                    case CLOSE_GOING_AWAY:
                    case CLOSE_PROTOCOL_ERROR:
                    case CLOSE_UNSUPORTED:
                    case CLOSE_NO_STATUS:
                    case CLOSE_ABNORMAL:
                    case CLOSE_INCONSISTANT_DATA:
                    case CLOSE_POLICY_VIOLATED:
                    case CLOSE_TO_LARGE:
                    case CLOSE_EXTENSION_NEGOTIATION:
                    case CLOSE_REQUEST_UNFULLABLE:
                        serviceHosts.markHostFailed(webSocket.request().url().host());
                        Ln.i("Connection closed, reset to try again.");
                        bus.post(new ResetEvent(MercuryClient.WebSocketStatusCodes.valueForCode(code)));
                        break;
                }
            }
        }

        @Override
        public void onFailure(okhttp3.WebSocket webSocket, Throwable t, Response response) {
            ln.d(t);
            if (!forcedStop) {

                if (response != null && response.request() != null && response.request().url() != null) {
                    serviceHosts.markHostFailed(response.request().url().host());
                }

                bus.post(new ResetEvent(MercuryClient.WebSocketStatusCodes.CLOSE_LOCAL_ERROR));
            }

            isConnected = false;
        }

        private void handleRegistrationEvent(MercuryEnvelope envelope) {
            MercuryRegistration event = (MercuryRegistration) envelope.getData();
            MercuryClient.this.mercuryConnectionServiceClusterUrl = event
                    .getLocalClusterServiceUrls()
                    .getMercuryConnectionServiceClusterUrl();

            if (deviceRegistration.getFeatures().isBufferedMercuryEnabled() && !event.getBufferState().isConversationBuffered()) {
                operationQueue.catchUpSync();
            }
        }

        private void handleConversationActivityEvent(MercuryEnvelope envelope) {
            ConversationActivityEvent activityEvent = (ConversationActivityEvent) envelope.getData();
            activityEvent.patch(envelope.getHeaders());
            activityListener.setActivityMetadata(activityEvent.getActivity().getId(), activityEvent.getActivity().getClientTempId(), envelope.getAlertType(), envelope.isDeliveryEscalation());
            bus.post(activityEvent);
        }
    }

    private WhiteboardActivityEvent getWhiteboardActivityEnvent(MercuryEnvelope envelope) {
        MercuryEnvelope.Headers headers = envelope.getHeaders();
        WhiteboardActivityEvent whiteboardActivityEvent = (WhiteboardActivityEvent) envelope.getData();
        if (null != headers && !TextUtils.isEmpty(headers.getChannelId())) {
            whiteboardActivityEvent.setChannelId(headers.getChannelId());
        }
        return whiteboardActivityEvent;
    }

    private boolean shouldConsiderRetry() {
        if (TestUtils.isInstrumentation()) {
            boolean retryCondition = applicationController.isRegistered();
            ln.i("shouldRetry(testing) startInProgress ? %s state = %s", retryCondition, applicationController.getState());
            // To address the issue with quick local handshake failures in integration (-intb)
            // We need to be able to consider a retry while we are in the process of starting
            // the applicationController.  isRegistered, considers STARTING and STARTED and REGISTERING
            // as valid states to retry.
            return retryCondition;
        } else {
            // This is the current check, this will fail if application controller is not starting or started
            // We should consider using isRegistered() and look at how we retry, do we need a
            // backoff algorithm, or a maximum number of retries.
            // This only considers STARTED or STARTING as a valid state for resetting the process.
            boolean retryCondition = applicationController.isStarted() || applicationController.isStarting();
            ln.i("shouldRetry ? %s state = %s ", retryCondition, applicationController.getState());
            return retryCondition;
        }
    }

    public enum WebSocketStatusCodes {
        CLOSE_NORMAL(1000),
        CLOSE_GOING_AWAY(1001),
        CLOSE_PROTOCOL_ERROR(1002),
        CLOSE_UNSUPORTED(1003),
        CLOSE_NO_STATUS(1005),
        CLOSE_ABNORMAL(1006),
        CLOSE_INCONSISTANT_DATA(1007),
        CLOSE_POLICY_VIOLATED(1008),
        CLOSE_TO_LARGE(1009),
        CLOSE_EXTENSION_NEGOTIATION(1010),
        CLOSE_REQUEST_UNFULLABLE(1011),
        CLOSE_UNKNOWN(0),
        CLOSE_LOCAL_ERROR(-1),
        CLOSE_REPLACED(4000);

        private int code;

        WebSocketStatusCodes(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        public static WebSocketStatusCodes valueForCode(int code) {
            for (WebSocketStatusCodes value : WebSocketStatusCodes.values()) {
                if (value.getCode() == code) {
                    return value;
                }
            }
            return CLOSE_UNKNOWN;
        }
    }

    public static class MercuryConnectedEvent {
    }
}
