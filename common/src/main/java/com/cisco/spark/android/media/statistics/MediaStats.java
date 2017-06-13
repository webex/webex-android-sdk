package com.cisco.spark.android.media.statistics;


public class MediaStats {
    private VideoStats video;
    private AudioStats audio;

    public MediaStats() {
        video = new VideoStats();
        audio = new AudioStats();
    }

    public VideoStats getVideo() {
        return video;
    }

    public void setVideo(VideoStats video) {
        this.video = video;
    }

    public AudioStats getAudio() {
        return audio;
    }

    public void setAudio(AudioStats audio) {
        this.audio = audio;
    }
}
