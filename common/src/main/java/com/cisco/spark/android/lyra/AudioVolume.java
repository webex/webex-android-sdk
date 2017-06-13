package com.cisco.spark.android.lyra;

public class AudioVolume {
    private int level;
    private int max;
    private int step;
    private int min;

    public int getLevel() {
        return level;
    }

    public int getMax() {
        return max;
    }

    public int getMin() {
        return min;
    }

    public int getStep() {
        return step;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public void setStep(int step) {
        this.step = step;
    }
}
