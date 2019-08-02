package com.ciscowebex.androidsdk.internal;

import com.ciscowebex.androidsdk.WebexEventPayload;

public interface WebexEvent {
    /**
     * Returns the {@link WebexEventPayload}.
     *
     * @return The WebexEventPayload.
     */
    WebexEventPayload getEventPayload();
}
