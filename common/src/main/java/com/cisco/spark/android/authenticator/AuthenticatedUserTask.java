package com.cisco.spark.android.authenticator;

import com.cisco.spark.android.core.ApplicationController;
import com.cisco.spark.android.util.SafeAsyncTask;

public class AuthenticatedUserTask extends SafeAsyncTask<Void> {

    private final ApplicationController applicationController;

    public AuthenticatedUserTask(ApplicationController applicationController) {
        this.applicationController = applicationController;
    }

    @Override
    public Void call() throws Exception {
        applicationController.start();
        return null;
    }
}
