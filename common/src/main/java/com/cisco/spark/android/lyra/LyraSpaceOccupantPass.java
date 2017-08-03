package com.cisco.spark.android.lyra;

public class LyraSpaceOccupantPass {
    public enum Type {
        MANUAL,
        ULTRASOUND,
        VERIFICATION,
        PROOF
    }

    private Type type;
    private String data;

    public LyraSpaceOccupantPass(Type type, String data) {
        this.type = type;
        this.data = data;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
