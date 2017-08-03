package com.cisco.spark.android.whiteboard.renderer;

import com.wacom.ink.rasterization.InkCanvas;
import com.wacom.ink.rasterization.Layer;

import java.util.ArrayList;
import java.util.List;

public class LayerPool {

    List<ReusableLayer> reusableLayers = new ArrayList<>();

    ReusableLayer obtainLayer(InkCanvas inkCanvas, int width, int height) {
        synchronized (reusableLayers) {
            for (ReusableLayer reusableLayer : reusableLayers) {
                if (reusableLayer.canBeTaken(width, height, inkCanvas)) {
                    reusableLayer.take();
                    return reusableLayer;
                }
            }
            ReusableLayer layer = new ReusableLayer(inkCanvas, width, height);
            reusableLayers.add(layer);
            return layer;
        }
    }

    public static class ReusableLayer {
        private boolean inUse;
        private InkCanvas inkCanvas;
        private int width;
        private int height;
        private Layer layer;

        public ReusableLayer(InkCanvas inkCanvas, int width, int height) {
            this.inkCanvas = inkCanvas;
            this.width = width;
            this.height = height;
            this.inUse = true;
            this.layer = inkCanvas.createLayer(width, height);
        }

        public Layer getLayer() {
            return layer;
        }

        public void take() {
            inUse = true;
            inkCanvas.clearLayer(layer);
        }

        //We need pass the currently available inkCanvas to make sure the it's the same one with the
        // inkCanvas instance maintained in the ReusableLayer
        public boolean canBeTaken(int width, int height, InkCanvas inkCanvas) {
            return !inUse && this.width == width && this.height == height && this.inkCanvas.equals(inkCanvas);
        }

        public void dispose() {
            if (!inUse) {
                return;
            }
            inUse = false;
        }
    }
}
