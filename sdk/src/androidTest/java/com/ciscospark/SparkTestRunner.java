package com.ciscospark;


import android.app.Application;
import android.content.Context;
import android.support.test.runner.AndroidJUnitRunner;

import com.ciscospark.core.SparkApplication;


public class SparkTestRunner extends AndroidJUnitRunner {
    @Override
    public Application newApplication(ClassLoader cl, String className, Context context) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return super.newApplication(cl, SparkApplication.class.getName(), context);
    }
}
