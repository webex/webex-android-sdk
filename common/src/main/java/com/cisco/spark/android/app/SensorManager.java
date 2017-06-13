package com.cisco.spark.android.app;

import android.hardware.Sensor;
import android.hardware.SensorEventListener;

public interface SensorManager {
    Sensor getDefaultSensor(int type);
    boolean registerListener(SensorEventListener listener, Sensor sensor, int rate);
    void unregisterListener(SensorEventListener listener);
}
