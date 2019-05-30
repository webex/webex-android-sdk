package com.ciscowebex.androidsdk.message;

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
     * @since 1.4.0
     */
    class MessageArrived implements MessageEvent {
        private Message message;

        public MessageArrived(Message message) {
            this.message = message;
        }

        /**
         * Return the message arrived.
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
     * The event when a message has been deleted.
     * @since 1.4.0
     */
    class MessageDeleted implements MessageEvent {
        private String messageId;

        public MessageDeleted(String messageId) {
            this.messageId = messageId;
        }

        /**
         * Return the id of the deleted message
         * @return The id of the deleted message
         */
        public String getMessageId() {
            return messageId;
        }
    }

    /**
     * The event when a message has been read.
     * @since 2.1.0
     */
    class MessageRead implements MessageEvent {

        private String spaceId;
        private String messageId;
        private String personId;

        public MessageRead(String spaceId, String messageId, String personId) {
            this.spaceId = spaceId;
            this.messageId = messageId;
            this.personId = personId;
        }

        /**
         * Return the id of the space where the message is read.
         * @return The id of the space where the message is read.
         */
        public String getSpaceId() {
            return spaceId;
        }

        /**
         * Return the id of the message that has been read.
         * @return The id of the message that has been read.
         */
        public String getMessageId() {
            return messageId;
        }

        /**
         * Return the id the person who read the message
         * @return The id the person who read the message.
         */
        public String getPersonId() {
            return personId;
        }
    }

    /**
     * Invoked when there is a new {@link MessageEvent}.
     * @param event     Message event
     */
    void onEvent(MessageEvent event);
}
