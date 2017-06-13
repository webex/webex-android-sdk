package com.cisco.spark.android.util;

import com.cisco.spark.android.core.ApplicationController;
import com.cisco.spark.android.core.Component;

public interface LocationManager extends Component {
    String getCoarseLocationName();
    String getCoarseLocationISO6709Position();
    boolean isEnabled();
    void setApplicationController(ApplicationController applicationController);
    void clearLocationCache();
}
