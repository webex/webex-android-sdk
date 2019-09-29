/*
 * Copyright 2016-2019 Cisco Systems Inc
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

package com.ciscowebex.androidsdk.membership;

import com.cisco.spark.android.model.conversation.Activity;
import com.ciscowebex.androidsdk.WebexEvent;

/**
 * Callback to receive the events from a {@link MembershipClient}.
 *
 * @since 2.3.0
 */
public interface MembershipObserver {

    /**
     * Mark interface for all membership events.
     */
    interface MembershipEvent extends WebexEvent {
    }

    /**
     * The event when a user is added to a space.
     */
    class MembershipCreated extends WebexEvent.Base implements MembershipEvent {

        private Membership membership;

        protected MembershipCreated(Membership membership, Activity activity) {
            super(activity);
            this.membership = membership;
        }

        /**
         * Returns the membership being added.
         */
        public Membership getMembership() {
            return membership;
        }
    }

    /**
     * The event when a user is removed from a space.
     */
    class MembershipDeleted extends WebexEvent.Base implements MembershipEvent {

        private Membership membership;

        protected MembershipDeleted(Membership membership, Activity activity) {
            super(activity);
            this.membership = membership;
        }

        /**
         * Returns the membership being removed.
         */
        public Membership getMembership() {
            return membership;
        }
    }

    /**
     * The event when a membership's properties changed.
     */
    class MembershipUpdated extends WebexEvent.Base implements MembershipEvent {

        private Membership membership;

        protected MembershipUpdated(Membership membership, Activity activity) {
            super(activity);
            this.membership = membership;
        }

        /**
         * Returns the membership whose properties have been modified.
         */
        public Membership getMembership() {
            return membership;
        }
    }

    /**
     * The event when a membership has sent a read receipt
     */
    class MembershipMessageSeen extends WebexEvent.Base implements MembershipEvent {

        private Membership membership;
        private String lastSeenMessageId;

        protected MembershipMessageSeen(Membership membership, Activity activity, String lastSeenMessageId) {
            super(activity);
            this.membership = membership;
            this.lastSeenMessageId = lastSeenMessageId;
        }

        /**
         * Returns the membership whose sent the read receipt.
         */
        public Membership getMembership() {
            return membership;
        }

        /**
         * Returns the id of last message read by the membership.
         */
        public String getLastSeenMessageId() {
            return lastSeenMessageId;
        }
    }

    /**
     * Invoked when there is a new {@link MembershipEvent}.
     *
     * @param event Membership event
     */
    void onEvent(MembershipEvent event);
}
