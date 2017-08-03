package com.cisco.spark.android.whiteboard.persistence.model;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;

import com.cisco.spark.android.util.BaseObservable;
import com.cisco.spark.android.whiteboard.view.model.Stroke;
import com.github.benoitdion.ln.Ln;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Whiteboard {

    private final String id;
    @NonNull private final List<Stroke> strokes;

    private Content backgroundContent;
    private Bitmap backgroundBitmap;
    private Bitmap cachedSnapshot;
    private boolean stale;

    private BaseObservable<Observer> observable;

    public Whiteboard(String id, List<Stroke> strokes) {
        this(id, strokes, null, null);
    }

    public Whiteboard(String id, List<Stroke> strokes, Content backgroundContent, Bitmap backgroundBitmap) {

        this.id = id;

        observable = new BaseObservable<>();

        if (strokes != null) {
            this.strokes = strokes;
        } else {
            this.strokes = new ArrayList<>();
        }

        this.backgroundContent = backgroundContent;
        this.backgroundBitmap = backgroundBitmap;
    }

    public String getId() {
        return id;
    }

    public void addStroke(Stroke stroke) {
        synchronized (strokes) {
            strokes.add(stroke);
        }
    }

    public void removeStroke(UUID removeStrokeId) {
        synchronized (strokes) {
            for (int i = 0; i < strokes.size(); i++) {
                if (strokes.get(i).hasSameId(removeStrokeId)) {
                    strokes.remove(i);
                    break;
                }
            }
        }
    }

    public List<Stroke> getStrokes() {
        synchronized (strokes) {
            // Always return a copy
            return new ArrayList<>(strokes);
        }
    }

    public Content getBackgroundContent() {
        return backgroundContent;
    }

    public void setBackgroundContent(Content backgroundContent) {
        this.backgroundContent = backgroundContent;
    }

    public Bitmap getBackgroundBitmap() {
        return backgroundBitmap;
    }

    public void addObserver(Whiteboard.Observer observer) {
        observable.addObserver(observer);
    }

    public void removeObserver(Whiteboard.Observer observer) {
        observable.removeObserver(observer);
    }

    public void setBackgroundBitmap(Bitmap backgroundBitmap) {

        if (this.backgroundBitmap == backgroundBitmap) {
            Ln.w("Refusing to set background bitmap to the same object");
            return;
        }

        this.backgroundBitmap = backgroundBitmap;
        observable.notify(o -> o.onBackgroundUpdated(this));
    }

    public void setStale(boolean stale) {
        this.stale = stale;
    }

    public boolean isStale() {
        return stale;
    }

    public void clear() {
        synchronized (strokes) {
            strokes.clear();
        }
    }

    public Bitmap getCachedSnapshot() {
        return cachedSnapshot;
    }

    public void setCachedSnapshot(Bitmap cachedSnapshot) {
        this.cachedSnapshot = cachedSnapshot;
    }

    @Override
    public String toString() {
        return "Whiteboard(" + id + ")";
    }

    public interface Observer {
        void onBackgroundUpdated(Whiteboard whiteboard);
    }

}
