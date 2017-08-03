package com.cisco.spark.android.mercury.events;

import android.support.annotation.NonNull;

import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.locus.events.ResetEvent;
import com.cisco.spark.android.mercury.MercuryClient;
import com.cisco.spark.android.processing.ActivityListener;
import com.cisco.spark.android.sync.operationqueue.core.OperationQueue;
import com.cisco.spark.android.util.Sanitizer;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.cisco.spark.android.whiteboard.WhiteboardService;
import com.github.benoitdion.ln.Ln;
import com.google.gson.Gson;

import de.greenrobot.event.EventBus;
import okhttp3.Response;
import okhttp3.WebSocket;

public class WhiteboardMercuryClient extends MercuryClient {

    public WhiteboardMercuryClient(ApiClientProvider apiClientProvider, Gson gson,
                                   EventBus bus, DeviceRegistration deviceRegistration,
                                   ActivityListener activityListener, Ln.Context lnContext,
                                   WhiteboardService whiteboardService, OperationQueue operationQueue, Sanitizer sanitizer) {

        super(false, apiClientProvider, gson, bus, deviceRegistration, activityListener, lnContext, whiteboardService, operationQueue, sanitizer);
    }

    @NonNull
    @Override
    protected String getName() {
        return "WhiteboardMercury";
    }

    @NonNull
    @Override
    protected MercuryWebsocketListener buildMercuryWebSocketListener() {
        return new WhiteboardMercuryWebSocketListener();
    }

    protected class WhiteboardMercuryWebSocketListener extends MercuryClient.MercuryWebsocketListener {

        @Override
        public void onClosed(WebSocket webSocket, int code, String reason) {

            ln.w("Mercury connection closed [%d (%s)] \"%s\" : %s", code, WebSocketStatusCodes.valueForCode(code).name(),
                 reason);

            if (!forcedStop) {
                // Status Code Definitions: https://tools.ietf.org/html/rfc6455#section-7.4.1
                switch (WebSocketStatusCodes.valueForCode(code)) {
                    case CLOSE_REPLACED:
                        ln.w("Connection was replaced, safe to ignore");
                        break;
                    case CLOSE_NORMAL:
                        ln.d("Normal close");
                        break;
                    case CLOSE_UNKNOWN:
                        ln.w("Connection closed with unknown code %d", code);
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
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            ln.e("Secondary mercury connection error");
            ln.e(t);
            //do nothing
        }
    }
}
