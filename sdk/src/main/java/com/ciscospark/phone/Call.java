// Copyright 2016-2017 Cisco Systems Inc
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

package com.ciscospark.phone;

import java.util.List;

/**
 * @author Allen Xiao<xionxiao@cisco.com>
 * @version 0.1
 */
public class Call {
    public enum FacingMode {
        USER, ENVIRONMENT
    }

    public void answer(CallObserver observer) {

    }

    public void reject() {

    }

    public void hangup() {

    }

    public void send(String dtmf) {

    }

    public void acknowledge() {

    }


    public List<CallMembership> getMembership() {
        return null;
    }

    public int getLocalVideoSize() {
        return 0;
    }

    public int getRemoteVideoSize() {
        return 0;
    }

    public boolean isSendingDTMFEnabled() {
        return false;
    }

    public boolean isRemoteSendingVideo() {
        return false;
    }

    public boolean isRemoteSendingAudio() {
        return false;
    }

    public boolean isSendingVideo() {
        return false;
    }

    public boolean isSendingAudio() {
        return false;
    }

    public FacingMode getFacingMode() {
        return null;
    }

}
