/*
 * Copyright 2016-2019 Cisco Systems Inc
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

package com.ciscowebex.androidsdk.space;

import com.ciscowebex.androidsdk.WebexEvent;

/**
 * Callback to receive the events from a {@link SpaceClient}.
 *
 * @since 2.2.0
 */
public interface SpaceObserver {

    /**
     * Mark interface for all space events.
     */
    interface SpaceEvent extends WebexEvent {
    }

    /**
     * The event when a new space was created
     *
     * @since 2.2.0
     */
    class SpaceCreated extends WebexEvent.Base implements SpaceEvent {

        private Space space;

        public SpaceCreated(Space space, WebexEvent.Payload payload) {
            super(payload);
            this.space = space;
        }

        /**
         * Return the id of space.
         *
         * @return the id of space.
         */
        public Space getSpace() {
            return space;
        }

    }

    /**
     * The event when a space was changed (usually a rename).
     *
     * @since 2.2.0
     */
    class SpaceUpdated extends WebexEvent.Base implements SpaceEvent {

        private Space space;

        public SpaceUpdated(Space space, WebexEvent.Payload payload) {
            super(payload);
            this.space = space;
        }

        /**
         * Return the id of space.
         *
         * @return the id of space.
         * @since 2.2.0
         */
        public Space getSpace() {
            return space;
        }
    }

    /**
     * Invoked when there is a new {@link SpaceEvent}.
     *
     * @param event SpaceEvent event
     */
    void onEvent(SpaceEvent event);
}
