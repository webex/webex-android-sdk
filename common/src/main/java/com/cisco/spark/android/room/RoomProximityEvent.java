package com.cisco.spark.android.room;

public class RoomProximityEvent {

    public enum ProximityState {
        FOUND_TOKEN,
        ANNOUNCING_TOKEN,
        REQUESTING_STATUS
    }

    private final ProximityState state;

    public RoomProximityEvent(ProximityState state) {
        this.state = state;
    }

    public ProximityState getState() {
        return state;
    }

    @Override
    public String toString() {
        return "RoomProximityEvent{" +
                "state=" + state +
                '}';
    }
}
