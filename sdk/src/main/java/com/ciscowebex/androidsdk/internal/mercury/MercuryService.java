/*
 * Copyright 2016-2021 Cisco Systems Inc
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.ciscowebex.androidsdk.internal.mercury;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.ciscowebex.androidsdk.WebexError;
import com.ciscowebex.androidsdk.auth.Authenticator;
import com.ciscowebex.androidsdk.internal.Closure;
import com.ciscowebex.androidsdk.internal.ServiceReqeust;
import com.ciscowebex.androidsdk.internal.queue.BackgroundQueue;
import com.ciscowebex.androidsdk.internal.queue.Queue;
import com.ciscowebex.androidsdk.utils.Json;
import com.ciscowebex.androidsdk.utils.Utils;
import com.ciscowebex.androidsdk.utils.http.HttpClient;
import com.github.benoitdion.ln.Ln;
import com.google.gson.*;

import me.helloworld.utils.Checker;
import okhttp3.*;
import okio.ByteString;

import java.util.Locale;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class MercuryService {

    public interface MercuryListener {
        void onConnected(@Nullable WebexError error);

        void onDisconnected(@Nullable WebexError error);

        void onEvent(@NonNull MercuryEvent event);
    }

    private static final int[] RETRY_DELAY = new int[]{1, 4, 16, 60, 120};
    private AtomicInteger failureCount = new AtomicInteger(0);

    private Queue queue = new BackgroundQueue();

    private String url;
    private WebSocket websocket;
    private boolean connected = false;

    private Authenticator authenticator;
    private OkHttpClient client = HttpClient.newClient().build();
    private MercuryWebsocketListener websocketListener = new MercuryWebsocketListener();
    private MercuryListener mercuryListener;
    private Closure<WebexError> onConnected;

    private Handler worker = new Handler(Looper.getMainLooper());
    private Runnable reconnectWork = () -> queue.run(new Runnable() {
        @Override
        public void run() {
            Ln.d("Mercury try to reconnect.");
            if (url != null && !connected) {
                authenticator.getToken(token -> {
                    Ln.d("Websocket reconnecting: " + url);
                    Request request = new Request.Builder().url(url).header("Authorization", "Bearer " + token.getData()).build();
                    client.dispatcher().cancelAll();
                    websocket = client.newWebSocket(request, websocketListener);
                });
            }
            else {
                Ln.d("Mercury should not reconnect: " + url + ", connected: " + connected);
            }
        }
    });

    public MercuryService(Authenticator authenticator, MercuryListener listener) {
        this.authenticator = authenticator;
        this.mercuryListener = listener;
    }

    public void connect(String url, Closure<WebexError> closure) {
        queue.run(() -> {
            if (websocket != null && connected) {
                Ln.w("Web socket has already connected");
                closure.invoke(null);
                return;
            }
            websocket = null;
            this.url = url;
            authenticator.getToken(token -> {
                Ln.d("Websocket connecting: " + url);
                Request request = new Request.Builder().url(url).header("Authorization", "Bearer " + token.getData()).build();
                client.dispatcher().cancelAll();
                onConnected = closure;
                websocket = client.newWebSocket(request, websocketListener);
            });
        });
    }

    public void disconnect(boolean clear) {
        queue.run(() -> {
            connected = false;
            resetReconnect();
            if (client != null) {
                client.dispatcher().cancelAll();
            }
            if (websocket != null) {
                if (!websocket.close(WebSocketStatusCodes.CLOSE_NORMAL.getCode(), "Websocket Stopping")) {
                    Ln.d("Stop mecury websocket abnormal");
                }
                websocket = null;
            }
            if (clear) {
                url = null;
            }
        });
    }

    public void tryReconnect() {
        queue.run(reconnectWork);
    }

    private void onMercuryDisconnected(WebexError error) {
        queue.run(() -> {
            connected = false;
            if (onConnected != null) {
                WebexError e = error == null ? WebexError.from("Websocket cannot connect") : error;
                Ln.d("Websocket cannot connect: " + e);
                onConnected.invoke(e);
                if (mercuryListener != null) {
                    mercuryListener.onConnected(e);
                }
                onConnected = null;
            } else if (error != null) {
                Ln.d("Websocket is disconnected: " + error);
                if (websocket == null || error.getErrorCode() == WebSocketStatusCodes.CLOSE_NORMAL.getCode()) {
                    Ln.d("Websocket is disconnected on purpose");
                    if (mercuryListener != null) {
                        mercuryListener.onDisconnected(null);
                    }
                } else {
                    int failureCount = this.failureCount.getAndAdd(1);
                    int retryIndex = failureCount < RETRY_DELAY.length ? failureCount : RETRY_DELAY.length - 1;
                    int delay = RETRY_DELAY[retryIndex] + new Random().nextInt(10);
                    Ln.d("Connection closed, reset to try again in %d seconds", delay);
                    worker.postDelayed(reconnectWork, delay * 1000);
                }
            }
        });
    }

    private void resetReconnect() {
        worker.removeCallbacks(reconnectWork);
        failureCount.set(0);
    }

    private class MercuryWebsocketListener extends WebSocketListener {

        private String trackingId;

        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            trackingId = response.header(ServiceReqeust.HEADER_TRACKING_ID);
            Ln.i("Mercury connection opened. handshake: %s - %s TrackingId: %s", response.code(), response.message(), trackingId);
            queue.run(() -> {
                connected = true;
                if (onConnected != null) {
                    onConnected.invoke(null);
                    onConnected = null;
                }
                if (mercuryListener != null) {
                    mercuryListener.onConnected(null);
                }
                resetReconnect();
            });
        }

        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            Ln.i("Connection closing. Reason: \"%s\", code: %d (%s), TrackingId: %s", reason, code, WebSocketStatusCodes.valueForCode(code).name(), trackingId);
        }

        @Override
        public void onClosed(WebSocket webSocket, int rawCode, String reason) {
            WebSocketStatusCodes code = WebSocketStatusCodes.valueForCode(rawCode);
            Ln.i("Connection closed. Reason: \"%s\", code: %d (%s), TrackingId: %s", reason, rawCode, code.name(), trackingId);
            onMercuryDisconnected(new WebexError(WebexError.ErrorCode.WEBSOCKET_ERROR, rawCode + "/" + reason));
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            Ln.e("Connection failure. Reason: %s, code: %s, Tracking ID: %s", t.getMessage(), response != null ? response.code() : null, trackingId);
            onMercuryDisconnected(WebexError.from(t));
        }

        @Override
        public void onMessage(WebSocket webSocket, ByteString bytes) {
            onMessage(webSocket, new String(bytes.toByteArray()));
        }

        @Override
        public void onMessage(WebSocket webSocket, String message) {
            if (Ln.isDebugEnabled()) {
                if (message != null && message.length() > 4000) {
                    String[] chunks = Utils.chunk(message, 1024);
                    for (String chunk : chunks) {
                        Ln.d("[WS]" + chunk);
                    }
                } else {
                    Ln.d("[WS]" + message);
                }
            }
            failureCount.set(0);
            if (!ackMessage(webSocket, message)) {
                return;
            }
            MercuryEnvelope envelope = Json.fromJson(message, MercuryEnvelope.class);
            if (envelope == null) {
                Ln.w("Message parse error: %s", message);
                return;
            }
            MercuryEvent event = envelope.getData();
            if (event != null && !Checker.isEmpty(event.getEventType().toString())) {
                if (event instanceof MercuryActivityEvent) {
                    ((MercuryActivityEvent) event).patch(envelope.getHeaders());
                }
                if (mercuryListener != null) {
                    mercuryListener.onEvent(event);
                }
            } else {
                Ln.w("Invalid message envelope.");
            }
        }

        private boolean ackMessage(final WebSocket socket, final String message) {
            try {
                final JsonObject envelopeObj = new JsonParser().parse(message).getAsJsonObject();
                final JsonElement idElem = envelopeObj.get("id");
                if (idElem == null) {
                    Ln.w("Incoming Mercury message didn't have id, ignoring");
                    return false;
                }
                final String id = idElem.getAsString();
                socket.send(Json.get().toJson(new AckMessage(id)));
            } catch (final JsonParseException | IllegalStateException | ClassCastException e) {
                Ln.w("Unable to parse id from Mercury message, ignoring: %s", e.getMessage());
                return false;
            }
            return true;
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

    public static class AckMessage {
        private final String type;
        private final String messageId;

        public AckMessage(String messageId) {
            this.type = "ack";
            this.messageId = messageId;
        }

        public String toString() {
            return String.format(Locale.US, "{ \"type\": \"%s\", \"messageId\": \"%s\" }", type, messageId);
        }
    }
}
