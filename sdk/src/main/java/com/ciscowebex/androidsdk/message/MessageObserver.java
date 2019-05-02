package com.ciscowebex.androidsdk.message;

/**
 * The struct of a message event
 * @since 1.4.0
 */
public interface MessageObserver {

    /**
     *
     */
    interface MessageEvent {
    }

    /**
     * The struct of a new message received event
     * @since 1.4.0
     */
    class MessageArrived implements MessageEvent {
        private Message message;

        public MessageArrived(Message message) {
            this.message = message;
        }
        public Message getMessage() {
            return message;
        }

        public void setMessage(Message message) {
            this.message = message;
        }
    }

    /**
     * The struct of a message delete event
     * @since 1.4.0
     */
    class MessageDeleted implements MessageEvent {
        private String messageId;

        public MessageDeleted(String messageId) {
            this.messageId = messageId;
        }

        public String getMessageId() {
            return messageId;
        }
    }

    /**
     * The struct of a message read event
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

        public String getSpaceId() {
            return spaceId;
        }

        public String getMessageId() {
            return messageId;
        }

        public String getPersonId() {
            return personId;
        }
    }

    /**
     * Call back when message arrived.
     * @param event     Message event
     */
    void onEvent(MessageEvent event);
}
