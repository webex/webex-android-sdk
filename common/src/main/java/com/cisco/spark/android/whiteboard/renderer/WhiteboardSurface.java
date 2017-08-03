package com.cisco.spark.android.whiteboard.renderer;

import android.graphics.SurfaceTexture;
import android.view.SurfaceHolder;

public class WhiteboardSurface {
    private final Object surface;
    private final SurfaceType type;

    public enum SurfaceType {
        SURFACETEXTURE,
        SURFACEHOLDER,
    }

    public WhiteboardSurface(SurfaceTexture surface) {
        this.surface = surface;
        this.type = SurfaceType.SURFACETEXTURE;
    }

    public WhiteboardSurface(SurfaceHolder surface) {
        this.surface = surface;
        this.type = SurfaceType.SURFACEHOLDER;
    }

    public SurfaceType getType() {
        return type;
    }

    public SurfaceTexture getSurfaceTexture() {
        if (type != SurfaceType.SURFACETEXTURE) {
            return null;
        }
        return (SurfaceTexture) surface;
    }

    public SurfaceHolder getSurfaceHolder() {
        if (type != SurfaceType.SURFACEHOLDER) {
            return null;
        }
        return (SurfaceHolder) surface;
    }

    @Override
    public String toString() {
        return surface.toString();
    }

    @Override
    public int hashCode() {
        return surface.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof WhiteboardSurface)) {
            return false;
        }
        if (obj == this) {
            return true;
        }

        WhiteboardSurface other = (WhiteboardSurface) obj;

        return this.surface == other.surface;
    }
}

