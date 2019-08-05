package com.ciscowebex.androidsdk.message;

import com.ciscowebex.androidsdk.people.Person;

/**
 * Callback to receive the events from a {@link MessageClient}.
 *
 * @since 1.4.0
 */
public interface MessageObserver {

    /**
     * Mark interface for all message events.
     */
    interface MessageEvent {
    }

    /**
     * The event when a new message has arrived.
     *
     * @deprecated
     * @since 1.4.0
     */
    @Deprecated
    class MessageArrived implements MessageEvent {
        private Message message;

        public MessageArrived(Message message) {
            this.message = message;
        }

        /**
         * Returns the message arrived.
         *
         * @return The message arrived.
         */
        public Message getMessage() {
            return message;
        }

        public void setMessage(Message message) {
            this.message = message;
        }
    }

    /**
     * The event when a new message has arrived.
     *
     * @since 2.1.0
     */
    class MessageReceived implements MessageEvent {
        private Message message;

        public MessageReceived(Message message) {
            this.message = message;
        }

        /**
         * Returns the message arrived.
         *
         * @return The message arrived.
         */
        public Message getMessage() {
            return message;
        }

    }

    /**
     * The event when a message has been deleted.
     *
     * @since 1.4.0
     */
    class MessageDeleted implements MessageEvent {
        private String messageId;

        public MessageDeleted(String messageId) {
            this.messageId = messageId;
        }

        /**
         * Returns the id of the deleted message
         *
         * @return The id of the deleted message
         */
        public String getMessageId() {
            return messageId;
        }
    }

    /**
     * Added by Orel Abutbul
     *
     */
    class MembershipsDeleted implements MessageEvent {

        private Person person;
        private String spaceId;

        public MembershipsDeleted(Person person, String spaceId) {
            this.spaceId = spaceId;
            this.person = person;
        }

        public String getSpaceId() {
            return spaceId;
        }

        public Person getMembership() { return person; }
    }

    /**
     * Added by Orel Abutbul
     *
     */
    class MembershipsAdded implements MessageEvent {

        private Person person;
        private String spaceId;

        public MembershipsAdded(Person person, String spaceId) {
            this.spaceId = spaceId;
            this.person = person;
        }

        public Person getMembership() {
            return this.person;
        }

        public String getSpaceId() {
            return spaceId;
        }
    }

    /**
     * Added by Orel Abutbul
     *
     */
    class MessageRead implements MessageEvent {

        private String messageId;
        private String spaceId;
        private Person person;

        public MessageRead(String spaceId, String messageId, Person person) {

            this.spaceId = spaceId;
            this.messageId = messageId;
            this.person = person;
        }

        public String  getMessage() {
            return this.messageId;
        }

        public String getSpaceId() {
            return this.spaceId;
        }

        public Person getPerson() {
            return this.person;
        }
    }
    /**
     * Invoked when there is a new {@link MessageEvent}.
     *
     * @param event Message event
     */
    void onEvent(MessageEvent event);
}
