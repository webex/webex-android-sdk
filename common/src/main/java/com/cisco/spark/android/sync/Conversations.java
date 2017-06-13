package com.cisco.spark.android.sync;

import android.os.Bundle;

public final class Conversations {

    private Conversations() {
    }

    public static String getTitle(Bundle bundle) {
        return bundle.getString(ConversationContract.ConversationEntry.TITLE.name());
    }

    public static String getConversationId(Bundle bundle) {
        return bundle.getString(ConversationContract.ConversationEntry.CONVERSATION_ID.name());
    }

    public static int getTeamColor(Bundle bundle) {
        return bundle.getInt(ConversationContract.TeamEntry.COLOR.name());
    }

}
