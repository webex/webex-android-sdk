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

package com.ciscowebex.androidsdk.internal.mercury;

import com.ciscowebex.androidsdk.internal.model.ActivityModel;
import com.ciscowebex.androidsdk.internal.model.ConversationModel;

public class MercuryActivityEvent extends MercuryEvent {

    private ActivityModel activity;

    public ActivityModel getActivity() {
        return this.activity;
    }

    public String toString() {
        return "Event: " + this.activity.toString();
    }

    public void patch(MercuryEnvelope.Headers headers) {
        if (this.activity.getTarget() != null && this.activity.getTarget() instanceof ConversationModel) {
            ConversationModel target = (ConversationModel) this.activity.getTarget();
            if (headers != null) {
                target.setLastReadableActivityDate(headers.getLastReadableActivityDate());
                target.setLastRelevantActivityDate(headers.getLastRelevantActivityDate());
                target.setLastSeenActivityDate(headers.getLastSeenActivityDate());
            } else {
                target.setLastReadableActivityDate(null);
                target.setLastRelevantActivityDate(null);
                target.setLastSeenActivityDate(null);
            }

        }
    }
}

