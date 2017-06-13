package com.cisco.spark.android.room.audiopairing;

import java.nio.FloatBuffer;

public interface AudioDataListener {

    void audioDataAvailable(FloatBuffer samples);
    void onFailure();

}
