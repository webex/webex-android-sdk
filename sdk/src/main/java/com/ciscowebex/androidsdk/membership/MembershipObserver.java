package com.ciscowebex.androidsdk.membership;

import com.ciscowebex.androidsdk.WebexEventPayload;
import com.ciscowebex.androidsdk.internal.WebexEvent;
import com.ciscowebex.androidsdk.internal.WebexEventImpl;

/**
 * Callback to receive the events from a {@link MembershipClient}.
 *
 * @since 2.2.0
 */
public interface MembershipObserver {

    /**
     * Mark interface for all membership events.
     */
    interface MembershipEvent extends WebexEvent{
    }

    /**
     * The event when a new membership has added to a space.
     *
     * @since 2.2.0
     */
    class MembershipCreated extends WebexEventImpl implements MembershipEvent {
        private Membership membership;
        public MembershipCreated(WebexEventPayload eventPayload, Membership membership) {
            super(eventPayload);
            this.membership = membership;
        }

        /**
         * Returns the membership has added.
         *
         * @return the membership has added.
         */
        public Membership getMembership() {
            return membership;
        }
    }

    /**
     * The event when a membership has removed from a space.
     *
     * @since 2.2.0
     */
    class MembershipDeleted extends WebexEventImpl implements MembershipEvent {
        private Membership membership;
        public MembershipDeleted(WebexEventPayload eventPayload, Membership membership) {
            super(eventPayload);
            this.membership = membership;
        }

        /**
         * Returns the membership has added.
         *
         * @return the membership has added.
         */
        public Membership getMembership() {
            return membership;
        }
    }

    /**
     * The event when a membership moderator status changed
     *
     * @since 2.2.0
     */
    class MembershipUpdated extends WebexEventImpl implements MembershipEvent{
        private Membership membership;
        public MembershipUpdated(WebexEventPayload eventPayload, Membership membership) {
            super(eventPayload);
            this.membership = membership;
        }

        /**
         * Returns the membership has added.
         *
         * @return the membership has added.
         */
        public Membership getMembership() {
            return membership;
        }
    }

    /**
     * The event when a user has sent a read receipt
     *
     * @since 2.2.0
     */
    class MembershipSeen extends WebexEventImpl implements MembershipEvent{
        private Membership membership;
        private String lastSeenMessageId;
        public MembershipSeen(WebexEventPayload eventPayload, Membership membership, String lastSeenMessageId) {
            super(eventPayload);
            this.membership = membership;
            this.lastSeenMessageId = lastSeenMessageId;
        }

        /**
         * Returns the membership has added.
         *
         * @return the membership has added.
         */
        public Membership getMembership() {
            return membership;
        }

        /**
         * Returns the id of last seen message in space.
         *
         * @return the id of last seen message in space.
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
