package com.ciscowebex.androidsdk.phone;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.ciscowebex.androidsdk.internal.model.LocusScheduledMeetingModel;
import com.ciscowebex.androidsdk.internal.model.LocusStateModel;
import com.ciscowebex.androidsdk.utils.DateUtils;
import me.helloworld.utils.annotation.StringPart;

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
    private int count;
    private boolean removed;

    /**
     * Start time.
     *
     * @since 2.6.0
     */
    public Date getStart() {
        return start;
    }

    /**
     * End time.
     *
     * @since 2.6.0
     */
    public Date getEnd() {
        return end;
    }

    protected CallSchedule(@NonNull LocusScheduledMeetingModel meeting, @Nullable LocusStateModel state) {
        this.count = state == null ? 0 : state.getCount();
        this.removed = meeting.isRemoved();
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
        return Objects.hash(start, end, count, removed);
    }

    @Override
    public String toString() {
        return "CallSchedule{" +
                "start=" + start +
                ", end=" + end +
                ", count=" + count +
                ", removed=" + removed +
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
