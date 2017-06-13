package com.cisco.spark.android.room;

/**
 * Various stages of the pairing flows
 * Will be extended with more stages as we add more flows
 */
public enum AltoFlowEvent {
    app_to_foreground,
    started_listening,
    decoded_token,
    announced_token,
    room_updated_event,
    got_room_state,
    enter_ultrasonic_range,
    left_ultrasonic_range
}
