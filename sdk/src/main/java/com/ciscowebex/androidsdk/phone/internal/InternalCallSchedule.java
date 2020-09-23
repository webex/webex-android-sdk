package com.ciscowebex.androidsdk.phone.internal;

import android.support.annotation.NonNull;
import com.ciscowebex.androidsdk.internal.model.LocusScheduledMeetingModel;
import com.ciscowebex.androidsdk.internal.model.LocusStateModel;
import com.ciscowebex.androidsdk.phone.CallSchedule;

public class InternalCallSchedule extends CallSchedule {

    public InternalCallSchedule(@NonNull LocusScheduledMeetingModel meeting, @NonNull LocusStateModel state) {
        super(meeting, state);
    }

}
