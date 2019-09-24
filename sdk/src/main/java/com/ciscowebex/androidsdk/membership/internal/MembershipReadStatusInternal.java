package com.ciscowebex.androidsdk.membership.internal;

import com.ciscowebex.androidsdk.membership.Membership;
import com.ciscowebex.androidsdk.membership.MembershipReadStatus;

import java.util.Date;

public class MembershipReadStatusInternal extends MembershipReadStatus {
    public MembershipReadStatusInternal(Membership membership, String lastSeenId, Date lastSeenDate) {
        _membership = membership;
        _lastSeenId = lastSeenId;
        _lastSeenDate = lastSeenDate;
    }
}
