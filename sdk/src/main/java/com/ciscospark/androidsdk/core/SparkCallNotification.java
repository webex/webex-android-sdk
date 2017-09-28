package com.ciscospark.androidsdk.core;

import com.cisco.spark.android.callcontrol.CallNotification;
import com.cisco.spark.android.callcontrol.NotificationActions;
import com.cisco.spark.android.locus.model.LocusKey;

/**
 * Created with IntelliJ IDEA.
 * User: zhiyuliu
 * Date: 27/09/2017
 * Time: 8:41 PM
 */

public class SparkCallNotification implements CallNotification {


	@Override
	public void notify(LocusKey locusKey, NotificationActions notificationActions) {
		
	}

	@Override
	public void dismiss(LocusKey locusKey) {

	}

	@Override
	public int getTimeout() {
		return 0;
	}
}
