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

package com.ciscowebex.androidsdk.phone;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.ciscowebex.androidsdk.internal.model.LocusScheduledMeetingModel;
import com.ciscowebex.androidsdk.internal.model.LocusStateModel;
import com.ciscowebex.androidsdk.utils.DateUtils;

import java.util.Date;
import java.util.Objects;

/**
 * A CallSchedule represents the schedule of a scheduled call
 *
 * @see Call
 * @since 2.6.0
 */
public class CallSchedule implements Comparable<CallSchedule> {

    private Date start;
    private Date end;

    /**
     * Start time of the call is scheduled.
     *
     * @since 2.6.0
     */
    public Date getStart() {
        return start;
    }

    /**
     * End time of the call is scheduled.
     *
     * @since 2.6.0
     */
    public Date getEnd() {
        return end;
    }

    protected CallSchedule(@NonNull LocusScheduledMeetingModel meeting, @Nullable LocusStateModel state) {
        this.start = meeting.getStartTime();
        if (this.start != null) {
            this.end = DateUtils.addMinutesToDate(meeting.getDurationMinutes(), this.start);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CallSchedule that = (CallSchedule) o;
        return Objects.equals(start, that.start) &&
                Objects.equals(end, that.end);
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end);
    }

    @Override
    public String toString() {
        return "CallSchedule{" +
                "start=" + start +
                ", end=" + end +
                '}';
    }

    @Override
    public int compareTo(@NonNull CallSchedule o) {
        if (this.start != null && o.start != null) {
            return this.start.compareTo(o.start);
        }
        if (this.end != null && o.end != null) {
            return this.end.compareTo(o.end);
        }
        return 0;
    }
}
