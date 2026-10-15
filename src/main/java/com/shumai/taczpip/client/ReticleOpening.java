package com.shumai.taczpip.client;

/** Shape of an already recognized exterior mask, independent of interior reticle strokes. */
public final class ReticleOpening {
    private ReticleOpening() {}
    public static float[] trace(int[] pixels,int width,int height) {
        if(width<2||height<2||pixels.length!=width*height) return new float[0];
        var points=new java.util.ArrayList<double[]>();
        for(int y=0;y<height;y++) {
            int left=-1,right=-1;
            for(int x=0;x<width;x++) if((pixels[y*width+x]>>>24)<26) {
                if(left<0) left=x;
                right=x;
            }
            if(left>=0) {
                points.add(new double[]{(left+.5)/width,(y+.5)/height});
                points.add(new double[]{(right+.5)/width,(y+.5)/height});
            }
        }
        return PupilShape.hull(points);
    }
}
