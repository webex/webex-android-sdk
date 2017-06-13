package com.cisco.spark.android.processing;

import com.cisco.spark.android.model.Activity;

public interface ActivityProcessor {
    ActivityProcessorCommand processActivity(Activity activity);
}
