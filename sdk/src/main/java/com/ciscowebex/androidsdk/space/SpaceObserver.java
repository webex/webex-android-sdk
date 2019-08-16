package com.ciscowebex.androidsdk.space;

import com.ciscowebex.androidsdk.WebexEventPayload;
import com.ciscowebex.androidsdk.internal.WebexEvent;
import com.ciscowebex.androidsdk.internal.WebexEventImpl;
import com.ciscowebex.androidsdk.membership.Membership;

import java.util.Date;

/**
 * Callback to receive the events from a {@link SpaceClient}.
 *
 * @since 2.2.0
 */
public interface SpaceObserver {

    /**
     * Mark interface for all space events.
     */
    interface SpaceEvent extends WebexEvent {
    }

    /**
     * The event when a new space was created
     *
     * @since 2.2.0
     */
    class SpaceCreated extends WebexEventImpl implements SpaceEvent {
        private String spaceId;
        private String creatorId;

        public SpaceCreated(WebexEventPayload eventPayload, String spaceId, String creatorId) {
            super(eventPayload);
            this.spaceId = spaceId;
            this.creatorId = creatorId;
        }

        /**
         * Return the id of space.
         *
         * @return the id of space.
         * @since 2.2.0
         */
        public String getSpaceId() {
            return spaceId;
        }

        /**
         * Return the id of creator.
         *
         * @return the id of creator.
         * @since 2.2.0
         */
        public String getCreatorId() {
            return creatorId;
        }
    }

    /**
     * The event when a space was changed (usually a rename).
     *
     * @since 2.2.0
     */
    class SpaceUpdated extends WebexEventImpl implements SpaceEvent {
        private String spaceId;

        public SpaceUpdated(WebexEventPayload eventPayload, String spaceId) {
            super(eventPayload);
            this.spaceId = spaceId;
        }

        /**
         * Return the id of space.
         *
         * @return the id of space.
         * @since 2.2.0
         */
        public String getSpaceId() {
            return spaceId;
        }
    }

    /**
     * Invoked when there is a new {@link SpaceEvent}.
     *
     * @param event SpaceEvent event
     */
    void onEvent(SpaceEvent event);
}
