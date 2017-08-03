package com.cisco.spark.android.room;

public interface LyraSpaceCallback {

    void newLyraSpaceStatus(LyraSpaceResponse lyraSpace);

    void onError(LyraSpaceResponse lyraSpace);
}
