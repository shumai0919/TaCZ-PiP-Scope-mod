package com.shumai.taczpip.client;

/** Removes texture padding/offset, retaining the contour for fitting onto the ocular. */
public final class NormalizedOpening {
    private NormalizedOpening() {}
    public static float[] fit(float[] points) {
        if(points.length<6 || points.length%2!=0) return new float[0];
        float minX=Float.POSITIVE_INFINITY,minY=minX,maxX=Float.NEGATIVE_INFINITY,maxY=maxX;
        for(int i=0;i<points.length;i+=2) {
            if(!Float.isFinite(points[i])||!Float.isFinite(points[i+1])) return new float[0];
            minX=Math.min(minX,points[i]);maxX=Math.max(maxX,points[i]);
            minY=Math.min(minY,points[i+1]);maxY=Math.max(maxY,points[i+1]);
        }
        if(maxX-minX<1e-6f||maxY-minY<1e-6f) return new float[0];
        float[] result=new float[points.length];
        for(int i=0;i<points.length;i+=2) {
            result[i]=(points[i]-minX)/(maxX-minX);
            result[i+1]=(points[i+1]-minY)/(maxY-minY);
        }
        return result;
    }
}
