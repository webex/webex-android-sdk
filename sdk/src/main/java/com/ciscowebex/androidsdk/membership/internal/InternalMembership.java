/*
 * Copyright 2016-2021 Cisco Systems Inc
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

public class InternalMembership extends Membership {

    public static class InternalMembershipCreated extends MembershipObserver.MembershipCreated {
        public InternalMembershipCreated(Membership membership, ActivityModel activity) {
            super(membership, activity);
        }
    }

    public static class InternalMembershipDeleted extends MembershipObserver.MembershipDeleted {
        public InternalMembershipDeleted(Membership membership, ActivityModel activity) {
            super(membership, activity);
        }
    }

    public static class InternalMembershipUpdated extends MembershipObserver.MembershipUpdated {
        public InternalMembershipUpdated(Membership membership, ActivityModel activity) {
            super(membership, activity);
        }
    }

    public static class InternalMembershipMessageSeen extends MembershipObserver.MembershipMessageSeen {
        public InternalMembershipMessageSeen(Membership membership, ActivityModel activity, String messageId) {
            super(membership, activity, messageId);
        }
    }

    public InternalMembership(ActivityModel activity, String clusterId) {
        super(activity, clusterId);
    }
}
