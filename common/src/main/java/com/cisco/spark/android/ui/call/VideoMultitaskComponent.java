package com.cisco.spark.android.ui.call;

import com.cisco.spark.android.core.ApplicationController;
import com.cisco.spark.android.core.Component;

public interface VideoMultitaskComponent extends Component, VideoOverlay {

    void setApplicationController(ApplicationController controller);

    void transitionToFullscreen();

}
