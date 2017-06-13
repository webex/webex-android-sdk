package com.cisco.spark.android.whiteboard.persistence.model;

import com.cisco.spark.android.whiteboard.view.model.Stroke;

import java.util.List;

public class Whiteboard {

    public final String id;
    public final List<Stroke> strokes;
    private boolean stale;

    public Whiteboard(String id, List<Stroke> strokes) {
        this.id = id;
        this.strokes = strokes;
    }

    public String getId() {
        return id;
    }

    public void addStroke(Stroke stroke) {
        strokes.add(stroke);
    }

    public List<Stroke> getStrokes() {
        return strokes;
    }

    public void setStale(boolean stale) {
        this.stale = stale;
    }

    public boolean isStale() {
        return stale;
    }

    public void clear() {
        strokes.clear();
    }

    @Override
    public String toString() {
        return "Whiteboard(" + id + ")";
    }

}
