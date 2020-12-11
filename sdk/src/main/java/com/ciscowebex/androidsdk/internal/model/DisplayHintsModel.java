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

package com.ciscowebex.androidsdk.internal.model;

import java.util.ArrayList;
import java.util.List;

public class DisplayHintsModel {

    public enum LocalRecordingStatus {
        LOCAL_RECORDING_STATUS_STOPPED,
        LOCAL_RECORDING_STATUS_PAUSED,
        LOCAL_RECORDING_STATUS_STARTED
    }

    public enum JoinedDisplayHint {
        WAITING_FOR_OTHERS,
        RECORDING_STATUS_STARTED,
        RECORDING_STATUS_PAUSED,
        RECORDING_STATUS_STOPPED,
        LOCK_STATUS_LOCKED,
        LOCK_STATUS_UNLOCKED;
    }

    public enum LobbyDisplayHint {
        WAIT_FOR_HOST_TO_JOIN,
        HOST_HAS_BEEN_NOTIFIED,
        OTHERS_HAVE_BEEN_NOTIFIED;

        static public LobbyDisplayHint fromString(String value) {
            try {
                value = value.toUpperCase().trim();
                return LobbyDisplayHint.valueOf(value);
            } catch (IllegalArgumentException e) {
                // unknown enum
                return null;
            }
        }
    }

    private List<String> joined = new ArrayList<>();
    private List<String> lobby = new ArrayList<>();
    private List<String> moderator = new ArrayList<>();

    public List<String> getJoinedHints() {
        return joined;
    }

    public List<String> getLobbyHints() {
        return lobby;
    }

    public List<String> getModeratorHints() {
        return moderator;
    }
}
