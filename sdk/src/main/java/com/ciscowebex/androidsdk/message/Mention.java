/*
 * Copyright 2016-2021 Cisco Systems Inc
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

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
     * @deprecated
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
     * @deprecated
     */
    @Deprecated
    class MentionAll implements Mention {

    }
}
