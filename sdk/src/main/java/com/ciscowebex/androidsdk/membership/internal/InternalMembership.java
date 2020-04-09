/*
 * Copyright 2016-2020 Cisco Systems Inc
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.ciscowebex.androidsdk.membership.internal;

import com.ciscowebex.androidsdk.internal.model.ActivityModel;
import com.ciscowebex.androidsdk.membership.Membership;
import com.ciscowebex.androidsdk.membership.MembershipObserver;

class InternalMembership extends Membership {

    static class InternalMembershipCreated extends MembershipObserver.MembershipCreated {
        InternalMembershipCreated(Membership membership, ActivityModel activity) {
            super(membership, activity);
        }
    }

    static class InternalMembershipDeleted extends MembershipObserver.MembershipDeleted {
        InternalMembershipDeleted(Membership membership, ActivityModel activity) {
            super(membership, activity);
        }
    }

    static class InternalMembershipUpdated extends MembershipObserver.MembershipUpdated {
        InternalMembershipUpdated(Membership membership, ActivityModel activity) {
            super(membership, activity);
        }
    }

    static class InternalMembershipMessageSeen extends MembershipObserver.MembershipMessageSeen {
        InternalMembershipMessageSeen(Membership membership, ActivityModel activity, String messageId) {
            super(membership, activity, messageId);
        }
    }

    InternalMembership(ActivityModel activity) {
        super(activity);
    }
}
