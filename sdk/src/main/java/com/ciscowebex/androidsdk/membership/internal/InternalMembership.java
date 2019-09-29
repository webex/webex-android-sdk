package com.ciscowebex.androidsdk.membership.internal;

import com.cisco.spark.android.model.conversation.Activity;
import com.ciscowebex.androidsdk.membership.Membership;
import com.ciscowebex.androidsdk.membership.MembershipObserver;

class InternalMembership extends Membership {

    static class InternalMembershipCreated extends MembershipObserver.MembershipCreated {
        InternalMembershipCreated(Membership membership, Activity activity) {
            super(membership, activity);
        }
    }

    static class InternalMembershipDeleted extends MembershipObserver.MembershipDeleted {
        InternalMembershipDeleted(Membership membership, Activity activity) {
            super(membership, activity);
        }
    }

    static class InternalMembershipUpdated extends MembershipObserver.MembershipUpdated {
        InternalMembershipUpdated(Membership membership, Activity activity) {
            super(membership, activity);
        }
    }

    static class InternalMembershipMessageSeen extends MembershipObserver.MembershipMessageSeen {
        InternalMembershipMessageSeen(Membership membership, Activity activity, String messageId) {
            super(membership, activity, messageId);
        }
    }

    InternalMembership(Activity activity) {
        super(activity);
    }
}
