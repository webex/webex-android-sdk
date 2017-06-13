package com.cisco.spark.android.mercury.events;

import com.cisco.spark.android.mercury.MercuryData;
import com.cisco.spark.android.mercury.MercuryEnvelope;
import com.cisco.spark.android.mercury.MercuryEventType;
import com.cisco.spark.android.model.Activity;
import com.cisco.spark.android.model.Conversation;

public class ConversationActivityEvent extends MercuryData {
    private Activity activity;

    public ConversationActivityEvent() {
        super();
    }

    public ConversationActivityEvent(Activity activity) {
        super(MercuryEventType.CONVERSATION_ACTIVITY);
        this.activity = activity;
    }

    public Activity getActivity() {
        return activity;
    }

    @Override
    public String toString() {
        return "Event: " + activity.toString();
    }

    public void patch(MercuryEnvelope.Headers headers) {
        if (activity.getTarget() == null || !(activity.getTarget() instanceof Conversation)) {
            return;
        }

        Conversation target = ((Conversation) activity.getTarget());

        if (headers != null) {
            target.setLastReadableActivityDate(headers.getLastReadableActivityDate());
            target.setLastRelevantActivityDate(headers.getLastRelevantActivityDate());
            target.setLastSeenActivityDate(headers.getLastSeenActivityDate());
        } else {
            // Clear these, they are sometimes included accidentally.
            target.setLastReadableActivityDate(null);
            target.setLastRelevantActivityDate(null);
            target.setLastSeenActivityDate(null);
        }
    }
}
