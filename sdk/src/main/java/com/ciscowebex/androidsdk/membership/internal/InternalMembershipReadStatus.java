package com.ciscowebex.androidsdk.membership.internal;

import com.cisco.spark.android.model.Person;
import com.cisco.spark.android.model.conversation.Conversation;
import com.ciscowebex.androidsdk.membership.MembershipReadStatus;

class InternalMembershipReadStatus extends MembershipReadStatus {

    InternalMembershipReadStatus(Conversation conversation, Person person) {
        super(conversation, person);
    }
}
