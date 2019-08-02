package com.ciscowebex.androidsdk.internal;

import com.ciscowebex.androidsdk.WebexEventPayload;

public class WebexEventImpl implements WebexEvent {
    private WebexEventPayload _eventPayload;

    public WebexEventImpl(WebexEventPayload eventPayload) {
        _eventPayload = eventPayload;
    }

    @Override
    public WebexEventPayload getEventPayload() {
        return _eventPayload;
    }

}
