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

import android.support.annotation.NonNull;

/**
 * Marker interface. Use either {@link Before.Message} or {@link Before.Date}.
 * @since 2.1.0
 */
public interface Before {

    /**
     * Before one particular message by message Id.
     * @since 2.1.0
     */
    class Message implements Before {
        private String id;

        /**
         * Create a {@link Before.Message} with a message id.
         * @param id the identifier of the message.
         */
        public Message(@NonNull String id) {
            this.id = id;
        }

        /**
         * Return the identifier of the message.
         * @return The identifier of the message.
         */
        public String getMessage() {
            return id;
        }
    }

    /**
     * Before a particular time point by date.
     * @since 2.1.0
     */
    class Date implements Before {
        private java.util.Date date;

        /**
         * Create a {@link Before.Date} with a {@link java.util.Date}.
         * @param date the date.
         */
        public Date(@NonNull java.util.Date date) {
            this.date = date;
        }

        /**
         * Return the date.
         * @return the date.
         */
        public java.util.Date getDate() {
            return date;
        }
    }
}
