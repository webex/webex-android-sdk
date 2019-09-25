package com.ciscowebex.androidsdk.message.internal;

import com.cisco.spark.android.model.AuthenticatedUser;
import com.cisco.spark.android.model.conversation.Activity;
import com.ciscowebex.androidsdk.message.Message;
import com.ciscowebex.androidsdk.message.MessageObserver;
import com.ciscowebex.androidsdk.space.Space;
import com.ciscowebex.androidsdk.space.SpaceObserver;

class InternalMessage extends Message {

    static class InternalMessageArrived extends MessageObserver.MessageArrived {
        InternalMessageArrived(Message message, Activity activity) {
            super(message, activity);
        }
    }

    static class InternalMessageReceived extends MessageObserver.MessageReceived {
        InternalMessageReceived(Message message, Activity activity) {
            super(message, activity);
        }
    }

    static class InternalMessageDeleted extends MessageObserver.MessageDeleted {
        InternalMessageDeleted(String message, Activity activity) {
            super(message, activity);
        }
    }

    InternalMessage(Activity activity, AuthenticatedUser user, boolean received) {
        super(activity, user, received);
    }
}
