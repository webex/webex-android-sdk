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

package com.ciscowebex.androidsdk.phone.internal;

import android.util.Size;
import android.view.View;

import com.ciscowebex.androidsdk.phone.CallMembership;
import com.ciscowebex.androidsdk.phone.AuxStream;

public class AuxStreamImpl implements AuxStream {

    private CallImpl call;

    private int vid;

    private View view;

    private CallMembership person;

    AuxStreamImpl(CallImpl call, int vid, View view)  {
        this.call = call;
        this.vid = vid;
        this.view = view;
    }

    public int getVid() {
        return vid;
    }

    public void setVid(int vid) {
        this.vid = vid;
    }

    @Override
    public View getRenderView() {
        return view;
    }

    @Override
    @Deprecated
    public void refresh() {
    }

    @Override
    public boolean isSendingVideo() {
        return call.getMedia() != null && !call.getMedia().isAuxVideoRemoteMuted(vid);
    }

    @Override
    public Size getSize() {
        return call.getMedia() == null ? new Size(0, 0) : call.getMedia().getAuxVideoViewSize(vid);
    }

    @Override
    public void close() {
        call.closeAuxStream(view);
    }

    @Override
    public CallMembership getPerson() {
        return person;
    }

    public void setPerson(CallMembership person) {
        this.person = person;
    }
}