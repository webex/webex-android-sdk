package com.ciscowebex.androidsdk.space.internal;

import com.cisco.spark.android.model.conversation.Activity;
import com.ciscowebex.androidsdk.space.Space;
import com.ciscowebex.androidsdk.space.SpaceObserver;

class InternalSpace extends Space {

    static class InternalSpaceCeated extends SpaceObserver.SpaceCreated {
        InternalSpaceCeated(Space space, Activity activity) {
            super(space, activity);
        }
    }

    static class InternalSpaceUpdated extends SpaceObserver.SpaceUpdated {
        InternalSpaceUpdated(Space space, Activity activity) {
            super(space, activity);
        }
    }

    InternalSpace(Activity activity) {
        super(activity);
    }

}
