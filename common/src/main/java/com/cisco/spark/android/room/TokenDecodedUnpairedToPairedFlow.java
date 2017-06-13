package com.cisco.spark.android.room;

import android.os.SystemClock;

import com.cisco.spark.android.metrics.MetricsReporter;
import com.cisco.spark.android.metrics.model.MetricsReportRequest;
import com.cisco.spark.android.room.model.RoomState;
import com.github.benoitdion.ln.Ln;

import static com.cisco.spark.android.room.AltoFlowEvent.announced_token;
import static com.cisco.spark.android.room.AltoFlowEvent.decoded_token;
import static com.cisco.spark.android.room.AltoFlowEvent.got_room_state;
import static com.cisco.spark.android.room.AltoFlowEvent.room_updated_event;

/**
 * This flow captures timestamps at these stages
 * 1. Successfully  found a new token and decoded it
 * 2. Successfully announced the token to RoomService (announce-proximity)
 * 3. Receives the Room updated event from Mercury
 * 4. Successfully receives and set the RoomState after requesting proximity-status
 *
 * The room updated event sometimes arrives before the announce returns, which complicates the
 * state handling
 */
public class TokenDecodedUnpairedToPairedFlow extends BaseAltoMetricFlow {

    private final MetricsReporter metricsReporter;
    private final RoomService roomService;

    public TokenDecodedUnpairedToPairedFlow(MetricsReporter metricsReporter, RoomService roomService) {
        this.metricsReporter = metricsReporter;
        this.roomService = roomService;
        clear();
    }

    @Override
    public synchronized void event(AltoFlowEvent event, RoomState roomState) {

        switch (event) {

            case app_to_foreground: {
                clear();
                break;
            }

            case started_listening: {
                clear();
                break;
            }

            case decoded_token: {
                clear();
                if (lastEvent == null) {
                    timestamps.put(event, SystemClock.uptimeMillis());
                    lastEvent = event;
                }
                break;

            }

            case announced_token: {

                if ((lastEvent == decoded_token || lastEvent == room_updated_event) && roomState == null) {
                    timestamps.put(event, SystemClock.uptimeMillis());
                    lastEvent = event;
                } else {
                    clear();
                }
                break;
            }

            case room_updated_event: {

                if ((lastEvent == announced_token || lastEvent == decoded_token) && roomState == null) {
                    timestamps.put(event, SystemClock.uptimeMillis());
                    lastEvent = event;
                } else {
                    clear();
                }
                break;
            }

            case got_room_state: {

                if ((lastEvent == room_updated_event || lastEvent == announced_token) && roomState != null) {

                    timestamps.put(event, SystemClock.uptimeMillis());

                    if (lastEvent == announced_token) {
                        // we went straight from announce ( happens if the sx10 believes we are paired but we don't know
                        // it yet, or if the event arrives before we are done announcing)
                        // Set the timestamp of room updated event to the same as announced token.
                        timestamps.put(room_updated_event, timestamps.get(announced_token));
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
                .addTokenDecodedUnpairedToPairedFlow(
                        millisBetween(decoded_token, announced_token),
                        millisBetween(announced_token, room_updated_event),
                        millisBetween(room_updated_event, got_room_state),
                        roomState.getRoomIdentity().toString(),
                        roomState.getRoomUrl().toString())
                .build();

        metricsReporter.enqueueMetricsReport(request);
    }

}
