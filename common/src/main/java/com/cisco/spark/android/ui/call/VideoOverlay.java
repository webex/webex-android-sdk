package com.cisco.spark.android.ui.call;

import com.cisco.spark.android.locus.model.LocusKey;

public interface VideoOverlay {

    void startOverlay();

    void show(VideoMode videoMode, LocusKey locusKey);

    void hide();

    void endCall(LocusKey locusKey);

    void setMultitaskingMode(boolean multitasking);

    boolean isVisible();

    void bringOverlayToFront();

    void subdueOverlay();

    enum VideoMode {
        SHOW_PREVIEW_VIDEO,
        SHOW_REMOTE_VIDEO,
        SHOW_WAITING_VIDEO
    }

}
