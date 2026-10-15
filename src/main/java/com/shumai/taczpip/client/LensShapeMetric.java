package com.shumai.taczpip.client;

/** Shading-only radial coordinate: zero at optical center, one on the convex aperture. */
public final class LensShapeMetric {
    private LensShapeMetric() {}
    public static float[] coefficients(LensBounds b) {
        if(b==null||!b.valid()||b.outline().length<6||b.outline().length>32)return new float[0];
        float[] v=b.outline(),result=new float[v.length];
        for(int i=0;i<v.length;i+=2) {
            int next=(i+2)%v.length;
            double nx=-(v[next+1]-v[i+1]),ny=v[next]-v[i];
            double distance=nx*(b.x()-v[i])+ny*(b.y()-v[i+1]);
            // Preserve the optical center. Invalid/off-center envelopes use the old metric.
            if(!Double.isFinite(distance)||distance<=1e-10)return new float[0];
            result[i]=(float)(-nx/distance);result[i+1]=(float)(-ny/distance);
        }
        return result;
    }
}
