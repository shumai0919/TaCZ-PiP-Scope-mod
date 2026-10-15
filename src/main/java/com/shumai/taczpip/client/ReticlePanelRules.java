package com.shumai.taczpip.client;

/** Recognize broad coplanar panels entirely outside a verified reticle-mask rectangle. */
public final class ReticlePanelRules {
    private ReticlePanelRules() {}
    public static boolean outside(double[] anchor,double[] panel) {
        if(anchor.length!=12||panel.length!=12) return false;
        double[] u=delta(anchor,3,0),v=delta(anchor,9,0);
        double uu=dot(u,u),vv=dot(v,v),uv=dot(u,v),det=uu*vv-uv*uv;
        if(!Double.isFinite(det)||det<1e-12) return false;
        double[] normal={u[1]*v[2]-u[2]*v[1],u[2]*v[0]-u[0]*v[2],u[0]*v[1]-u[1]*v[0]};
        double minU=Double.POSITIVE_INFINITY,minV=minU,maxU=-minU,maxV=-minU;
        for(int i=0;i<12;i+=3) {
            double[] d={panel[i]-anchor[0],panel[i+1]-anchor[1],panel[i+2]-anchor[2]};
            if(!Double.isFinite(dot(d,d)) || Math.abs(dot(d,normal))>Math.sqrt(det)*Math.sqrt(Math.max(uu,vv))*1e-4) return false;
            double du=dot(d,u),dv=dot(d,v);
            double a=(du*vv-dv*uv)/det,b=(dv*uu-du*uv)/det;
            minU=Math.min(minU,a);maxU=Math.max(maxU,a);minV=Math.min(minV,b);maxV=Math.max(maxV,b);
        }
        // Broad outside covers commonly overlap the mask by a seam-hiding margin.
        // Permit at most 2% of the anchor extent, never interior/half-overlapping marks.
        return maxU-minU>=.25 && maxV-minV>=.25 &&
                (maxU<=.02 || minU>=.98 || maxV<=.02 || minV>=.98);
    }
    private static double[] delta(double[] p,int a,int b){return new double[]{p[a]-p[b],p[a+1]-p[b+1],p[a+2]-p[b+2]};}
    private static double dot(double[] a,double[] b){return a[0]*b[0]+a[1]*b[1]+a[2]*b[2];}
}
