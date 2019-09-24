package com.ciscowebex.androidsdk.membership.internal;

import com.ciscowebex.androidsdk.membership.Membership;

import java.util.Date;

class MembershipInternal extends Membership {
    public MembershipInternal(String id, String personId, String personEmail, String personDisplayName, String personOrgId, String spaceId, boolean isModerator, boolean isMonitor, Date created) {
        _id = id;
        _personId = personId;
        _personEmail = personEmail;
        _personDisplayName = personDisplayName;
        _personOrgId = personOrgId;
        _spaceId = spaceId;
        _isModerator = isModerator;
        _isMonitor = isMonitor;
        _created = created;
    }
}
