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
     * @Deprecated
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
     * Invoked when there is a new {@link MessageEvent}.
     *
     * @param event Message event
     */
    void onEvent(MessageEvent event);
}
