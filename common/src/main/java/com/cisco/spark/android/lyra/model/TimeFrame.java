package com.cisco.spark.android.lyra.model;

public class TimeFrame {
    private String start;
    private String end;
    private long millisUntilStart;
    private long millisUntilEnd;

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    public long getMillisUntilStart() {
        return millisUntilStart;
    }

    public void setMillisUntilStart(long millisUntilStart) {
        this.millisUntilStart = millisUntilStart;
    }

    public long getMillisUntilEnd() {
        return millisUntilEnd;
    }

    public void setMillisUntilEnd(long millisUntilEnd) {
        this.millisUntilEnd = millisUntilEnd;
    }
}
