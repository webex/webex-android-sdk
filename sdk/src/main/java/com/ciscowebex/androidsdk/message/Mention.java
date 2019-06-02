package com.ciscowebex.androidsdk.message;

/**
 * A data type represents mention.
 * <p>
 * Mention is the way to send specific notification to the people mentioned.
 *
 * @since 1.4.0
 */
public interface Mention {



    /**
     * Mention one particular person by person Id.
     *
     * @since 2.1.0
     */
    class Person implements Mention {
        private String personId;

        public Person(String personId) {
            this.personId = personId;
        }

        public String getPersonId() {
            return personId;
        }
    }

    /**
     * Mention all people in a space.
     *
     * @since 2.1.0
     */
    class All implements Mention {

    }

    /**
     * Mention one particular person by person Id.
     *
     * @since 1.4.0
     * @Deprecated
     */
    @Deprecated
    class MentionPerson implements Mention {
        private String personId;

        public MentionPerson(String personId) {
            this.personId = personId;
        }

        public String getPersonId() {
            return personId;
        }
    }

    /**
     * Mention all people in a space.
     *
     * @since 1.4.0
     * @Deprecated
     */
    @Deprecated
    class MentionAll implements Mention {

    }
}
