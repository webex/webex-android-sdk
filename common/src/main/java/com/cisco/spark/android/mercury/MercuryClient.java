package com.cisco.spark.android.mercury;

import android.net.Uri;
import android.os.SystemClock;
import android.support.annotation.NonNull;

import com.cisco.spark.android.authenticator.ApiTokenProvider;
import com.cisco.spark.android.authenticator.NotAuthenticatedException;
import com.cisco.spark.android.client.TrackingIdGenerator;
import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.ApplicationController;
import com.cisco.spark.android.core.Component;
import com.cisco.spark.android.core.Settings;
import com.cisco.spark.android.locus.events.ResetEvent;
import com.cisco.spark.android.locus.model.LocusSelfRepresentation;
import com.cisco.spark.android.mercury.events.ConversationActivityEvent;
import com.cisco.spark.android.mercury.events.LocusChangedEvent;
import com.cisco.spark.android.mercury.events.WhiteboardActivityEvent;
import com.cisco.spark.android.processing.ActivityListener;
import com.cisco.spark.android.util.DiagnosticModeChangedEvent;
import com.cisco.spark.android.util.Sanitize;
import com.cisco.spark.android.util.Strings;
import com.cisco.spark.android.util.UserAgentProvider;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.cisco.spark.android.whiteboard.WhiteboardService;
import com.github.benoitdion.ln.Ln;
import com.github.benoitdion.ln.NaturalLog;
import com.google.gson.Gson;

import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_17;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.HashMap;

import de.greenrobot.event.EventBus;

public class MercuryClient implements Component {
    private static final int SOCKET_TIMEOUT_MS = 15000; // 15s, same as server

    private final ApiClientProvider apiClientProvider;
    private final ApiTokenProvider apiTokenProvider;
    private final Gson gson;
    private final EventBus bus;
    private final DeviceRegistration deviceRegistration;
    private final Settings settings;
    private final UserAgentProvider userAgentProvider;
    private final TrackingIdGenerator trackingIdGenerator;
    private final ActivityListener activityListener;
    protected final NaturalLog ln;
    protected final WhiteboardService whiteboardService;

    protected MercuryWebSocketClient webSocketClient;
    private final Object syncLock = new Object();
    protected ApplicationController applicationController;
    private long lastPing;
    protected boolean forcedStop;
    private String verboseLoggingToken;
    private Uri uriOverride;
    private Uri mercuryConnectionServiceClusterUrl;

    private final boolean isPrimary;

    public MercuryClient(boolean primary, ApiClientProvider apiClientProvider, ApiTokenProvider apiTokenProvider, Gson gson, EventBus bus,
                         DeviceRegistration deviceRegistration, Settings settings, UserAgentProvider userAgentProvider,
                         TrackingIdGenerator trackingIdGenerator, ActivityListener activityListener, Ln.Context lnContext,
                         WhiteboardService whiteboardService) {

        this.apiClientProvider = apiClientProvider;
        this.apiTokenProvider = apiTokenProvider;
        this.gson = gson;
        this.bus = bus;
        this.deviceRegistration = deviceRegistration;
        this.settings = settings;
        this.userAgentProvider = userAgentProvider;
        this.trackingIdGenerator = trackingIdGenerator;
        this.activityListener = activityListener;
        this.ln = Ln.get(lnContext, getName());
        this.whiteboardService = whiteboardService;

        bus.register(this);

        this.isPrimary = primary;
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

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEvent(DiagnosticModeChangedEvent event) {
        verboseLoggingToken = event.getVerboseLoggingToken();
    }

    @Override
    public boolean shouldStart() {
        return true;
    }

    @Override
    public void start() {
        ln.i("Starting mercury");

        try {
            synchronized (syncLock) {
                forcedStop = false;

                if (isRunning()) {
                    Ln.w(new RuntimeException("Mercury is already running"));
                    return;
                }

                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Authorization", apiTokenProvider.getAuthenticatedUser().getConversationAuthorizationHeader());
                headers.put("User-Agent", userAgentProvider.get());
                String trackingID = trackingIdGenerator.nextTrackingId();
                ln.d("Mercury tracking id " + trackingID);

                headers.put("TrackingID", trackingID);
                // Avoid race where the token gets reset to null on the event bus.
                String localToken = verboseLoggingToken;
                if (localToken != null)
                    headers.put("LogLevelToken", localToken);

                Uri webSocketUri;

                if (uriOverride != null) {
                    webSocketUri = uriOverride;
                } else {
                    webSocketUri = deviceRegistration.getWebSocketUrl();
                }

                webSocketClient = buildMercuryWebSocketClient(webSocketUri, headers);
                if (!settings.allowUnsecuredConnection() || isSecuredWebSocketUri(webSocketUri)) {
                    webSocketClient.setSocket(apiClientProvider.buildSSLSocket());
                }
                webSocketClient.connect();
            }
        } catch (NotAuthenticatedException ex) {
            ln.i("No authenticated user. Mercury did not start.");
        } catch (Exception e) {
            ln.e(e);
        }
    }

    @NonNull
    protected MercuryWebSocketClient buildMercuryWebSocketClient(Uri webSocketUri, HashMap<String, String> headers) {
        return new MercuryWebSocketClient(webSocketUri, headers);
    }

    private boolean isSecuredWebSocketUri(Uri uri) {
        return uri.getScheme().startsWith("wss");
    }

    @Override
    public void stop() {
        synchronized (syncLock) {
            forcedStop = true;
            if (webSocketClient != null) {
                ln.i("Stopping Mercury client.");
                try {
                    webSocketClient.close();
                } catch (Exception ex) {
                    ln.e(ex);
                } finally {
                    webSocketClient = null;
                }
            }
        }
    }

    public boolean isRunning() {
        synchronized (syncLock) {
            return webSocketClient != null && webSocketClient.isOpen();
        }
    }

    public void setApplicationController(ApplicationController applicationController) {
        applicationController.register(this);
        this.applicationController = applicationController;
    }

    public void logState() {
        ln.v("WebSocketClient is null: %b", webSocketClient == null);
        if (webSocketClient != null) {
            ln.v("WebSocketClient is connecting: %b", webSocketClient.isConnecting());
            ln.v("WebSocketClient is closed: %b", webSocketClient.isClosed());
            ln.v("WebSocketClient is open: %b", webSocketClient.isOpen());
        }
        ln.v("WebSocket uri: %s", deviceRegistration.getWebSocketUrl());
    }

    public void send(String message) {
        if (webSocketClient != null) {
            Ln.d("Send " + message);
            try {
                webSocketClient.send(message);
            } catch (WebsocketNotConnectedException ex) {
                ln.d(ex);
            } catch (Throwable ex) {
                ln.e(ex, "Failed to parse the message correctly");
            }
        }
    }

    public void setUriOverride(Uri uriOverride) {
        this.uriOverride = uriOverride;
    }

    public long getLastPing() {
        return lastPing;
    }

    protected class MercuryWebSocketClient extends WebSocketClient {
        public MercuryWebSocketClient(Uri serverURI, HashMap<String, String> headers) {
            // Draft_17 implements Hybi 17 / RFC 6455 which corresponds to the server's WebSocket implementation.
            // See https://github.com/TooTallNate/Java-WebSocket/wiki/Drafts for more details.
            super(URI.create(serverURI.toString()), new Draft_17(), headers, SOCKET_TIMEOUT_MS);
        }

        @Override
        public void onOpen(ServerHandshake handshakeData) {
            ln.d("Mercury connection opened.");
            bus.post(new MercuryConnectedEvent());
        }

        @Override
        public void onMessage(String message) {
            ln.d("Received message: %s", Sanitize.sanitize(message));
            try {
                MercuryEnvelope envelope = gson.fromJson(message, MercuryEnvelope.class);
                if (envelope != null) {
                    if (envelope.getData() != null && !Strings.isEmpty(envelope.getData().getEventType().toString())) {
                        if (envelope.getData() instanceof ConversationActivityEvent) {
                            ConversationActivityEvent activityEvent = (ConversationActivityEvent) envelope.getData();
                            activityEvent.patch(envelope.getHeaders());
                            activityListener.setActivityMetadata(activityEvent.getActivity().getId(), envelope.getAlertType(), envelope.isDeliveryEscalation());
                            bus.post(activityEvent);
                        } else if (envelope.getData() instanceof WhiteboardActivityEvent) {
                            whiteboardService.mercuryEvent((WhiteboardActivityEvent) envelope.getData());
                        } else if (envelope.getData() instanceof MercuryRegistration) {
                            MercuryRegistration event = (MercuryRegistration) envelope.getData();
                            MercuryClient.this.mercuryConnectionServiceClusterUrl = event
                                    .getLocalClusterServiceUrls()
                                    .getMercuryConnectionServiceClusterUrl();
                        } else if (envelope.getData() instanceof LocusChangedEvent) {
                            LocusChangedEvent locusChangedEvent = ((LocusChangedEvent) envelope.getData());

                            if (envelope.getAlertType() == AlertType.NONE && locusChangedEvent.getLocus().getSelf() != null && !locusChangedEvent.getLocus().getSelf().getAlertType().getAction().equals(LocusSelfRepresentation.AlertType.ALERT_NONE)) {
                                locusChangedEvent.getLocus().getSelf().getAlertType().setAction(LocusSelfRepresentation.AlertType.ALERT_NONE);
                            }

                            bus.post(locusChangedEvent);
                        } else {
                            bus.post(envelope.getData());
                        }
                    } else {
                        ln.w("Invalid message envelope.");
                    }
                    getConnection().send(gson.toJson(new AckMessage(envelope.getId())));
                }
            } catch (WebsocketNotConnectedException ex) {
                ln.d(ex);
                if (!forcedStop)
                    bus.post(new ResetEvent(WebSocketStatusCodes.CLOSE_LOCAL_ERROR));
            } catch (Throwable ex) {
                ln.e(ex, "Failed to parse the message correctly");
            }
        }

        @Override
        public void onWebsocketPing(WebSocket conn, Framedata f) {
            super.onWebsocketPing(conn, f);
            ln.d("received ping");
            lastPing = SystemClock.elapsedRealtime();
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            ln.i("Connection closed. Reason: %s, code: %d (%s), remote: %b, last ping :%d", reason, code,
                 WebSocketStatusCodes.valueForCode(code).name(), remote, lastPing);

            if (applicationController.isStarted() && !forcedStop) {
                // Status Code Definitions: https://tools.ietf.org/html/rfc6455#section-7.4.1
                switch (WebSocketStatusCodes.valueForCode(code)) {
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
                        if (deviceRegistration.getFeatures().isWhiteboardEnabled()) {
                            if (whiteboardService.usePrimaryMercury()) {
                                Ln.e("WhiteboardService: Mercury: onClose()");
                                whiteboardService.mercuryErrorEvent(new ResetEvent(WebSocketStatusCodes.valueForCode(code)));
                            }
                        }
                        bus.post(new ResetEvent(WebSocketStatusCodes.valueForCode(code)));
                        break;
                }
            }
        }

        @Override
        public void onError(Exception ex) {
            ln.i(ex, "Web socket error. Restarting the Mercury client.");
            bus.post(new ResetEvent(WebSocketStatusCodes.CLOSE_LOCAL_ERROR));
        }

        @Override
        public void onMessage(ByteBuffer bytes) {
            String message = new String(bytes.array());
            onMessage(message);
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

    public class MercuryConnectedEvent {
    }
}
