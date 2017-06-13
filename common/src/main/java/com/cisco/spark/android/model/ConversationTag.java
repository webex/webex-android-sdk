package com.cisco.spark.android.model;

public enum ConversationTag {
    MUTED,
    ONE_ON_ONE,
    FAVORITE,
    HIDDEN,
    LOCKED,
    TEAM,   // If a conversation has the TEAM tag it is the primary conversation for a team. This
            //   means that the participant list is the list of members of the team, and the
            //   conversation is the 'general' team conversation
    OPEN,   // If a conversation has the OPEN tag it is a conversation that is open to all members
            //   of a team that it is associated with, but is not the primary conversation
    NOT_JOINED,  // This means the conversation is a team room that the self user hasn't joined
    ARCHIVED,
    MESSAGE_NOTIFICATIONS_ON,
    MESSAGE_NOTIFICATIONS_OFF,
    MENTION_NOTIFICATIONS_ON,
    MENTION_NOTIFICATIONS_OFF
}
