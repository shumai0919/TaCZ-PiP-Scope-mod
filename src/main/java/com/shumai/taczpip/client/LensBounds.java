package com.shumai.taczpip.client;

/** Projected ocular bounds in bottom-left-origin texture coordinates, never a replacement mask. */
public record LensBounds(float x, float y, float rx, float ry, float[] outline, boolean alphaShaped, float facing,
                         float tiltX,float tiltY) {
    public LensBounds(float x,float y,float rx,float ry,float[] outline,boolean alphaShaped,float facing) {
        this(x,y,rx,ry,outline,alphaShaped,facing,0,0);
    }
    public LensBounds(float x,float y,float rx,float ry,float[] outline,boolean alphaShaped) {
        this(x,y,rx,ry,outline,alphaShaped,1f);
    }
    public LensBounds(float x,float y,float rx,float ry,float[] outline) { this(x,y,rx,ry,outline,false); }
    public LensBounds(float x, float y, float rx, float ry) { this(x,y,rx,ry,new float[0]); }
    public static final LensBounds INVALID = new LensBounds(0, 0, 0, 0);
    public boolean valid() {
        return Float.isFinite(x) && Float.isFinite(y) && Float.isFinite(rx) && Float.isFinite(ry) && Float.isFinite(facing)
                && Float.isFinite(tiltX) && Float.isFinite(tiltY) && rx > 0 && ry > 0;
    }
    public float pupilRadius(float progress, int width, int height) {
        if (!valid()) return 0;
        return (float) EyeBoxRules.radius(progress, (x - .5) * width, (y - .5) * height,
                rx * width, ry * height);
    }
}
