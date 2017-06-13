package com.cisco.spark.android.app;

import android.hardware.Sensor;
import android.hardware.SensorEventListener;

public class AndroidSensorManager implements SensorManager {
    private final android.hardware.SensorManager delegate;

    public AndroidSensorManager(android.hardware.SensorManager delegate) {
        this.delegate = delegate;
    }

    @Override
    public Sensor getDefaultSensor(int type) {
        return delegate.getDefaultSensor(type);
    }

    @Override
    public boolean registerListener(SensorEventListener listener, Sensor sensor, int rate) {
        return delegate.registerListener(listener, sensor, rate);
    }

    @Override
    public void unregisterListener(SensorEventListener listener) {
        delegate.unregisterListener(listener);
    }
}
