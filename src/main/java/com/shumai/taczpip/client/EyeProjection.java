package com.shumai.taczpip.client;

/** Screen projection of the optical center and its eye-relative pupil center. */
public record EyeProjection(float centerX,float centerY,float pupilX,float pupilY,boolean visible) {
    public static final EyeProjection CLOSED=new EyeProjection(0,0,0,0,false);
    public boolean finite() {
        return Float.isFinite(centerX)&&Float.isFinite(centerY)&&Float.isFinite(pupilX)&&Float.isFinite(pupilY);
    }
}
