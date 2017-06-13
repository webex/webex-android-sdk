package com.cisco.spark.android.room;

import android.os.SystemClock;
import android.support.annotation.Nullable;

import com.cisco.spark.android.metrics.MetricsReporter;
import com.cisco.spark.android.metrics.model.MetricsReportRequest;
import com.cisco.spark.android.room.model.RoomState;
import com.github.benoitdion.ln.Ln;

import static com.cisco.spark.android.room.AltoFlowEvent.announced_token;
import static com.cisco.spark.android.room.AltoFlowEvent.decoded_token;
import static com.cisco.spark.android.room.AltoFlowEvent.enter_ultrasonic_range;
import static com.cisco.spark.android.room.AltoFlowEvent.got_room_state;
import static com.cisco.spark.android.room.AltoFlowEvent.room_updated_event;

/**
 * This flow tries to capture timestamps at these stages
 * 1. Moved into range of an ultrasonic sound source
 * 2. Successfully found a new token and decoded it
 * 3. Successfully announced the token to RoomService (announce-proximity)
 * 4. Receives the Room updated event from Mercury
 * 5. Successfully receives and set the RoomState after requesting proximity-status
 *
 * The room updated event sometimes arrives before the announce returns, which complicates the
 * state handling
 */
public class UltrasoundRangeToFoundTokenFlow extends BaseAltoMetricFlow {

    private final MetricsReporter metricsReporter;
    private final RoomService roomService;

    public UltrasoundRangeToFoundTokenFlow(MetricsReporter metricsReporter, RoomService roomService) {
        this.metricsReporter = metricsReporter;
        this.roomService = roomService;
        clear();
    }

    @Override
    public void event(AltoFlowEvent event, @Nullable RoomState roomState) {
        switch (event) {

            case started_listening: {
                clear();
                break;
            }

            case left_ultrasonic_range: {
                clear();
                break;
            }

            case enter_ultrasonic_range: {
                clear();
                if (roomState == null) {
                    timestamps.put(event, SystemClock.uptimeMillis());
                    lastEvent = event;
                }
                break;
            }

            case decoded_token: {

                if (lastEvent == enter_ultrasonic_range && roomState == null) {
                    timestamps.put(event, SystemClock.uptimeMillis());
                    lastEvent = event;
                } else {
                    clear();
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
                .addUltrasonicRangeToPairedFlow(
                        millisBetween(enter_ultrasonic_range, decoded_token),
                        millisBetween(decoded_token, announced_token),
                        millisBetween(announced_token, room_updated_event),
                        millisBetween(room_updated_event, got_room_state),
                        millisBetween(enter_ultrasonic_range, got_room_state),
                        roomState.getRoomIdentity().toString(),
                        roomState.getRoomUrl().toString())
                .build();

        metricsReporter.enqueueMetricsReport(request);

    }

}
