package com.shumai.taczpip.client;

import java.util.List;

/** Aggregate ocular-local frame. Independent of screen area, UVs and face ordering. */
public final class ApertureFrame {
    private ApertureFrame() {}
    public static double[][] fit(List<double[]> vertices,float[] shape) {
        if(vertices.isEmpty()||shape.length<6||shape.length%2!=0) return new double[0][];
        double[] min={Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY};
        double[] max={Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY};
        for(var v:vertices) for(int a=0;a<3;a++) {
            if(!Double.isFinite(v[a]))return new double[0][];
            min[a]=Math.min(min[a],v[a]);max[a]=Math.max(max[a],v[a]);
        }
        int normal=2;
        for(int a=0;a<2;a++)if(max[a]-min[a]<max[normal]-min[normal])normal=a;
        int u=normal==0?1:0,v=normal==2?1:2;
        if(max[u]-min[u]<1e-7||max[v]-min[v]<1e-7)return new double[0][];
        // Only planar oculars have a meaningful aggregate plane; do not guess for volumes.
        if(max[normal]-min[normal]>.05*Math.min(max[u]-min[u],max[v]-min[v]))return new double[0][];
        double[][] result=new double[shape.length/2][3];
        for(int i=0;i<result.length;i++) {
            if(!Float.isFinite(shape[i*2])||!Float.isFinite(shape[i*2+1]))return new double[0][];
            result[i][normal]=(min[normal]+max[normal])*.5;
            result[i][u]=min[u]+shape[i*2]*(max[u]-min[u]);
            result[i][v]=max[v]-shape[i*2+1]*(max[v]-min[v]);
        }
        return result;
    }
}
