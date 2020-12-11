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

import java.util.Date;

public class LocusStateModel {

    public enum State {
        TERMINATING,
        INACTIVE,
        INITIALIZING,
        ACTIVE
    }

    public enum Type {
        CALL,
        MEETING,
        SIP_BRIDGE,
        IVR
    }

    private boolean active;
    private int count;
    private Date lastActive;
    private LocusStateModel.State state;
    private boolean startingSoon;
    private LocusStateModel.Type type;

    public LocusStateModel(boolean active, int count, LocusStateModel.State state) {
        this.active = active;
        this.count = count;
        this.state = state;
    }

    public LocusStateModel(boolean active, int count, LocusStateModel.State state, LocusStateModel.Type type) {
        this(active, count, state);
        this.type = type;
    }

    /**
     * This is set to true if the locus is active
     */
    public boolean isActive() {
        return active || (state != null && state.equals(LocusStateModel.State.INITIALIZING));
    }


    public LocusStateModel.State getState() {
        return state;
    }

    public int getCount() {
        return count;
    }

    public Date getLastActive() {
        return lastActive;
    }

    public void setLastActive(Date lastActive) {
        this.lastActive = lastActive;
    }

    public LocusStateModel.Type getType() {
        return type;
    }

    public boolean isStartingSoon() {
        return startingSoon;
    }

}
