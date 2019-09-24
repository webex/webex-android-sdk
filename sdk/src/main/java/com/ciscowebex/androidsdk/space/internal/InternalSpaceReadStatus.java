package com.ciscowebex.androidsdk.space.internal;

import com.cisco.spark.android.model.conversation.Conversation;
import com.ciscowebex.androidsdk.space.SpaceReadStatus;

class InternalSpaceReadStatus extends SpaceReadStatus {

    InternalSpaceReadStatus(Conversation conversation) {
        super(conversation);
    }

}
