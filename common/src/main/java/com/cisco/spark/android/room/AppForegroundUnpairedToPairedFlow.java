package com.cisco.spark.android.room;

import android.os.SystemClock;

import com.cisco.spark.android.metrics.MetricsReporter;
import com.cisco.spark.android.metrics.model.MetricsReportRequest;
import com.cisco.spark.android.room.model.RoomState;
import com.github.benoitdion.ln.Ln;

import static com.cisco.spark.android.room.AltoFlowEvent.app_to_foreground;
import static com.cisco.spark.android.room.AltoFlowEvent.started_listening;
import static com.cisco.spark.android.room.AltoFlowEvent.decoded_token;
import static com.cisco.spark.android.room.AltoFlowEvent.announced_token;
import static com.cisco.spark.android.room.AltoFlowEvent.room_updated_event;
import static com.cisco.spark.android.room.AltoFlowEvent.got_room_state;

/**
 * This flow captures timestamps at these stages
 * 1. App comes to foreground and device is not paired to a room
 * 2. App starts to listen for tokens
 * 3. Successfully  found a new token and decoded it
 * 4. Successfully announced the token to RoomService (announce-proximity)
 * 5. Receives the Room updated event from Mercury
 * 6. Successfully receives and set the RoomState after requesting proximity-status
 */
public class AppForegroundUnpairedToPairedFlow extends BaseAltoMetricFlow {

    private final MetricsReporter metricsReporter;
    private final RoomService roomService;

    public AppForegroundUnpairedToPairedFlow(MetricsReporter metricsReporter, RoomService roomService) {
        this.metricsReporter = metricsReporter;
        this.roomService = roomService;
        clear();
    }

    @Override
    public synchronized void event(AltoFlowEvent event, RoomState roomState) {

        switch (event) {

            case app_to_foreground: {

                if (roomService.isInRoom()) {
                    // Flow requires the app to come to foreground unpaired to trigger
                    clear();
                    return;
                }

                clear();

                timestamps.put(app_to_foreground, SystemClock.uptimeMillis());
                lastEvent = event;
                break;
            }

            case started_listening: {

                if (lastEvent == app_to_foreground) {
                    timestamps.put(event, SystemClock.uptimeMillis());
                    lastEvent = event;
                }

                break;
            }

            case decoded_token: {

                if (lastEvent == started_listening) {
                    timestamps.put(event, SystemClock.uptimeMillis());
                    lastEvent = event;
                }

                break;

            }

            case announced_token: {

                if (lastEvent == AltoFlowEvent.decoded_token) {
                    timestamps.put(event, SystemClock.uptimeMillis());
                    lastEvent = event;
                }

                break;
            }

            case room_updated_event: {

                if (lastEvent == AltoFlowEvent.announced_token) {
                    timestamps.put(event, SystemClock.uptimeMillis());
                    lastEvent = event;
                }

                break;
            }

            case got_room_state: {

                if (lastEvent == AltoFlowEvent.room_updated_event || lastEvent == announced_token) {

                    timestamps.put(event, SystemClock.uptimeMillis());

                    if (lastEvent == announced_token) {
                        // we went straight from announce ( happens if the sx10 believes we are paired but we don't know
                        // it yet, or if the event arrives before we are done announcing)
                        // Set the timestamp of room updated event to the same as announced token.
                        timestamps.put(AltoFlowEvent.room_updated_event, timestamps.get(announced_token));
                    }

                    lastEvent = event;
                    publishMetrics();
                }

                break;
            }
        }
    }

    @Override
    public void publishMetrics() {

        RoomState roomState = roomService.getRoomState();
        Ln.i("flow executed publish metrics");
        if (roomState == null) {
            Ln.w("lost room pairing before publishing - ignore");
            return;
        }

        MetricsReportRequest request = metricsReporter.newRoomServiceMetricsBuilder()
                .addAppForegroundToPairedFlow(
                        millisBetween(app_to_foreground, started_listening),
                        millisBetween(started_listening, decoded_token),
                        millisBetween(decoded_token, announced_token),
                        millisBetween(announced_token, room_updated_event),
                        millisBetween(room_updated_event, got_room_state),
                        millisBetween(app_to_foreground, got_room_state),
                        roomState.getRoomIdentity().toString(),
                        roomState.getRoomUrl().toString())
                .build();

        metricsReporter.enqueueMetricsReport(request);
    }

}
