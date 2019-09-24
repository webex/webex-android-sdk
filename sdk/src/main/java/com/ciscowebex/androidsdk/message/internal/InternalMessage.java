package com.ciscowebex.androidsdk.message.internal;

import com.cisco.spark.android.model.AuthenticatedUser;
import com.cisco.spark.android.model.conversation.Activity;
import com.ciscowebex.androidsdk.message.Message;

class InternalMessage extends Message {

    InternalMessage(Activity activity, AuthenticatedUser user, boolean received) {
        super(activity, user, received);
    }
}
