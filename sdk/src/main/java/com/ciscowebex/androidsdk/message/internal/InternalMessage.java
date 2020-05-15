/*
 * Copyright 2016-2020 Cisco Systems Inc
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

package com.ciscowebex.androidsdk.message.internal;

import com.ciscowebex.androidsdk.internal.Credentials;
import com.ciscowebex.androidsdk.internal.model.ActivityModel;
import com.ciscowebex.androidsdk.message.Message;
import com.ciscowebex.androidsdk.message.MessageObserver;
import com.ciscowebex.androidsdk.message.RemoteFile;

import java.util.List;

class InternalMessage extends Message {

    static class InternalMessageArrived extends MessageObserver.MessageArrived {
        InternalMessageArrived(Message message, ActivityModel activity) {
            super(message, activity);
        }
    }

    static class InternalMessageReceived extends MessageObserver.MessageReceived {
        InternalMessageReceived(Message message, ActivityModel activity) {
            super(message, activity);
        }
    }

    static class InternalMessageDeleted extends MessageObserver.MessageDeleted {
        InternalMessageDeleted(String message, ActivityModel activity) {
            super(message, activity);
        }
    }

    static class InternalMessageFileThumbnailsUpdated extends MessageObserver.MessageFileThumbnailsUpdated{
        InternalMessageFileThumbnailsUpdated(ActivityModel activity, String messageId, List<RemoteFile> files){
            super(activity, messageId, files);
        }
    }

    InternalMessage(ActivityModel activity, Credentials user, boolean received) {
        super(activity, user, received);
    }
}
