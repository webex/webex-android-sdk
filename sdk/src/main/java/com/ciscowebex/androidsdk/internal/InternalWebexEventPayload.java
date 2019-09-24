package com.ciscowebex.androidsdk.internal;

import com.cisco.spark.android.model.AuthenticatedUser;
import com.cisco.spark.android.model.conversation.Activity;
import com.ciscowebex.androidsdk.WebexEvent;

public class InternalWebexEventPayload extends WebexEvent.Payload {

    public InternalWebexEventPayload(Activity activity, AuthenticatedUser user, WebexEvent.Data data) {
        super(activity, user, data);
    }
}
