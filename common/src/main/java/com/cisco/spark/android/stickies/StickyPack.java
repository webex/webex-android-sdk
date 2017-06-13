package com.cisco.spark.android.stickies;


import java.util.List;

public class StickyPack {

    private List<StickyPad> pads;

    public StickyPack() {
    }

    public List<StickyPad> getPads() {
        return pads;
    }

    public void setPads(List<StickyPad> pads) {
        this.pads = pads;
    }

    @Override
    public String toString() {
        return "StickyPack{" +
                "pads=" + pads +
                '}';
    }

}
