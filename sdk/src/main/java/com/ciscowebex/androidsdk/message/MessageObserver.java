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

import com.ciscowebex.androidsdk.WebexEvent;
import com.ciscowebex.androidsdk.internal.model.ActivityModel;

import java.util.List;

/**
 * Callback to receive the events from a {@link MessageClient}.
 *
 * @since 1.4.0
 */
public interface MessageObserver {

    /**
     * Mark interface for all message events.
     */
    interface MessageEvent extends WebexEvent {
    }

    /**
     * The event when a new message has arrived.
     *
     * @since 1.4.0
     * @deprecated
     */
    @Deprecated
    class MessageArrived extends WebexEvent.Base implements MessageEvent {
        private Message message;

        protected MessageArrived(Message message, ActivityModel activity) {
            super(activity);
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
    class MessageReceived extends WebexEvent.Base implements MessageEvent {

        private Message message;

        protected MessageReceived(Message message, ActivityModel activity) {
            super(activity);
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
    class MessageDeleted extends WebexEvent.Base implements MessageEvent {

        private String messageId;

        protected MessageDeleted(String messageId, ActivityModel activity) {
            super(activity);
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
     * The event when an existing message has been updated.
     *
     * @since 2.6.0
     */
    abstract class MessageUpdated extends WebexEvent.Base implements MessageEvent {
        private String messageId;

        protected MessageUpdated(String messageId, ActivityModel activity) {
            super(activity);
            this.messageId = messageId;
        }

        /**
         * Return id of the updated message.
         */
        public String getMessageId() {
            return messageId;
        }
    }

    /**
     * The thumbnails of the attached files in message has been updated.
     *
     * @since 2.6.0
     */
    class MessageFileThumbnailsUpdated extends MessageUpdated {
        private List<RemoteFile> files;

        protected MessageFileThumbnailsUpdated(String messageId, ActivityModel activity, List<RemoteFile> files) {
            super(messageId, activity);
            this.files = files;
        }

        /**
         * Return the updated file(s) list.
         *
         * @return The updated file(s) list.
         */
        public List<RemoteFile> getFiles() {
            return files;
        }
    }

    /**
     * Invoked when there is a new {@link MessageEvent}.
     *
     * @param event Message event
     */
    void onEvent(MessageEvent event);
}
