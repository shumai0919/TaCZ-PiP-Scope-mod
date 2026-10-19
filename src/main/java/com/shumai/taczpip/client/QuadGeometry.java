package com.shumai.taczpip.client;

/** TaCZ emits zero-area placeholder quads for omitted cube faces. */
public final class QuadGeometry {
    private QuadGeometry() {}
    public static boolean hasArea(double[] p) {
        if(p.length!=12)return false;
        for(double v:p)if(!Double.isFinite(v))return false;
        for(int i=1;i<3;i++) {
            double ax=p[i*3]-p[0],ay=p[i*3+1]-p[1],az=p[i*3+2]-p[2];
            double bx=p[(i+1)*3]-p[0],by=p[(i+1)*3+1]-p[1],bz=p[(i+1)*3+2]-p[2];
            double x=ay*bz-az*by,y=az*bx-ax*bz,z=ax*by-ay*bx;
            if(x*x+y*y+z*z>1e-24)return true;
        }
        return false;
    }
}
