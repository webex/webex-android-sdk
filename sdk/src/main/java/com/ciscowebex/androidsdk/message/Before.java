package com.ciscowebex.androidsdk.message;

import android.support.annotation.NonNull;

public interface Before {

    /**
     * Before one particular message by message Id.
     * @since 2.1.0
     */
    class Message implements Before {
        private String id;

        public Message(@NonNull String id) {
            this.id = id;
        }

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

        public Date(@NonNull java.util.Date date) {
            this.date = date;
        }

        public java.util.Date getDate() {
            return date;
        }
    }
}
