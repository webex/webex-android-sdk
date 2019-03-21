package com.ciscowebex.androidsdk.message;

import com.ciscowebex.androidsdk.people.Person;

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

        public String getMessageId() {
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
    class MembershipsUpdated implements MessageEvent {

        private Message message;
        private String spaceId;
        private Person person;


        public MembershipsUpdated(Message message, String spaceId, Person person) {
            this.message = message;
            this.spaceId = spaceId;
            this.person = person;
        }

        public Message getMessage() {
            return message;
        }

        public String getSpaceId() {
            return spaceId;
        }

        public Person getPerson() {
            return person;
        }
    }

    /**
     * Call back when message arrived.
     * @param event     Message event
     */
    void onEvent(MessageEvent event);
}
