package com.cisco.spark.android.mercury.events;

import android.net.Uri;
import android.support.annotation.NonNull;

import com.cisco.spark.android.authenticator.ApiTokenProvider;
import com.cisco.spark.android.client.TrackingIdGenerator;
import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.Settings;
import com.cisco.spark.android.locus.events.ResetEvent;
import com.cisco.spark.android.mercury.MercuryClient;
import com.cisco.spark.android.processing.ActivityListener;
import com.cisco.spark.android.util.UserAgentProvider;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.cisco.spark.android.whiteboard.WhiteboardService;
import com.github.benoitdion.ln.Ln;
import com.google.gson.Gson;

import java.util.HashMap;

import de.greenrobot.event.EventBus;

public class WhiteboardMercuryClient extends MercuryClient {

    public WhiteboardMercuryClient(ApiClientProvider apiClientProvider, ApiTokenProvider apiTokenProvider, Gson gson,
                                   EventBus bus, DeviceRegistration deviceRegistration, Settings settings,
                                   UserAgentProvider userAgentProvider, TrackingIdGenerator trackingIdGenerator,
                                   ActivityListener activityListener, Ln.Context lnContext,
                                   WhiteboardService whiteboardService) {

        super(false, apiClientProvider, apiTokenProvider, gson, bus, deviceRegistration, settings, userAgentProvider,
              trackingIdGenerator, activityListener, lnContext, whiteboardService);
    }

    @NonNull
    @Override
    protected MercuryWebSocketClient buildMercuryWebSocketClient(Uri webSocketUri, HashMap<String, String> headers) {
        return new WhiteboardMercuryWebSocketClient(webSocketUri, headers);
    }

    @NonNull
    @Override
    protected String getName() {
        return "WhiteboardMercury";
    }

    protected class WhiteboardMercuryWebSocketClient extends MercuryClient.MercuryWebSocketClient {
        public WhiteboardMercuryWebSocketClient(Uri serverURI, HashMap<String, String> headers) {
            super(serverURI, headers);
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {

            ln.w("Mercury connection closed [%d (%s)] \"%s\" : %s", code, WebSocketStatusCodes.valueForCode(code).name(),
                 reason, remote ? " closed by remote" : "closed locally");

            if (!forcedStop) {
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
                        Ln.e("WhiteboardService: WhiteboardMercuryClient: onClose()");
                        whiteboardService.mercuryErrorEvent(new ResetEvent(WebSocketStatusCodes.valueForCode(code)));
                        break;
                }
            }
        }

        @Override
        public void onError(Exception ex) {
            ln.e("Secondary mercury connection error");
            ln.e(ex);
            //do nothing
        }
    }
}
