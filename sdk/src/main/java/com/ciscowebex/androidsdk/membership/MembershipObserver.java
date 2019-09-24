package com.ciscowebex.androidsdk.membership;

import com.ciscowebex.androidsdk.WebexEvent;

/**
 * Callback to receive the events from a {@link MembershipClient}.
 *
 * @since 2.3.0
 */
public interface MembershipObserver {

    /**
     * Mark interface for all message events.
     */
    interface MembershipEvent extends WebexEvent {
    }

    /**
     * The event when a new membership has added to a space.
     *
     * @since 2.3.0
     */
    class MembershipCreated extends WebexEvent.Base implements MembershipEvent {

        private Membership membership;

        public MembershipCreated(Membership membership, WebexEvent.Payload payload) {
            super(payload);
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
     * @since 2.3.0
     */
    class MembershipDeleted extends WebexEvent.Base implements MembershipEvent {

        private Membership membership;

        public MembershipDeleted(Membership membership, Payload payload) {
            super(payload);
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
     * @since 2.3.0
     */
    class MembershipUpdated extends WebexEvent.Base implements MembershipEvent {

        private Membership membership;

        public MembershipUpdated(Membership membership, Payload payload) {
            super(payload);
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
    class MembershipSeen extends WebexEvent.Base implements MembershipEvent {

        private Membership membership;
        private String lastSeenMessageId;

        public MembershipSeen(Membership membership, WebexEvent.Payload payload, String lastSeenMessageId) {
            super(payload);
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
