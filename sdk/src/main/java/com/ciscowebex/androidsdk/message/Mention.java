package com.ciscowebex.androidsdk.message;

/**
 * A data type represents mention.
 * @since 1.4.0
 */
public interface Mention {

    /**
     * Mention one particular person by person Id.
     * @since 1.4.0
     */
      class MentionPerson implements Mention {
        private String personId;

        public MentionPerson(String personId) {
            this.personId = personId;
        }

        public String getPersonId() {
            return personId;
        }

        public void setPersonId(String personId) {
            this.personId = personId;
        }
    }

    /**
     * Mention all people in a space.
     * @since 1.4.0
     */
     class MentionAll implements Mention {

    }
}
