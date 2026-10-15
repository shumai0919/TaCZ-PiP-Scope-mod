package com.shumai.taczpip.client;

/** Exterior black separation plus its connected one-texel staircase band. Interior strokes are retained. */
public final class ReticleMaskRules {
    private ReticleMaskRules() {}
    private static boolean black(int rgba) {
        // The exterior's antialiased fringe is still mask, even when not opaque.
        return (rgba>>>24)>0 && (rgba&255)<=24 && ((rgba>>>8)&255)<=24 && ((rgba>>>16)&255)<=24;
    }
    public static boolean[] exterior(int[] rgba,int w,int h) {
        boolean[] remove=new boolean[rgba.length];
        if(w<16 || h<16 || rgba.length!=w*h) return remove;
        int border=0,dark=0,clear=0;
        var points=new java.util.ArrayList<double[]>();
        for(int y=0;y<h;y++) for(int x=0;x<w;x++) {
            int p=rgba[y*w+x];
            if(x==0||y==0||x==w-1||y==h-1) { border++; if(black(p)) dark++; }
            if((p>>>24)<26) {
                clear++;
                // Row-extreme transparent pixel centers suffice for the convex hull.
                if(x==0 || x==w-1 || (rgba[y*w+x-1]>>>24)>=26 || (rgba[y*w+x+1]>>>24)>=26)
                    points.add(new double[]{x+.5,y+.5});
            }
        }
        if(dark<border*.9 || clear<w*h*.25 || clear>w*h*.9) return remove;
        // This hull has no shader edge-count cap.
        points.sort(java.util.Comparator.<double[]>comparingDouble(p->p[0]).thenComparingDouble(p->p[1]));
        var hull=new java.util.ArrayList<double[]>();
        for(var p:points) { while(hull.size()>1 && cross(hull.get(hull.size()-2),hull.get(hull.size()-1),p)<=0) hull.remove(hull.size()-1); hull.add(p); }
        int lower=hull.size();
        for(int i=points.size()-2;i>=0;i--) { var p=points.get(i); while(hull.size()>lower && cross(hull.get(hull.size()-2),hull.get(hull.size()-1),p)<=0) hull.remove(hull.size()-1); hull.add(p); }
        if(hull.size()<4) return remove;
        hull.remove(hull.size()-1);
        boolean[] edge=new boolean[rgba.length];
        for(int y=0;y<h;y++) for(int x=0;x<w;x++) {
            if(!black(rgba[y*w+x])) continue;
            for(int e=0;e<hull.size();e++) {
                var a=hull.get(e); var b=hull.get((e+1)%hull.size());
                // Remove the exterior up to the clear-view hull; retaining a one-pixel ring
                // here would leave the same enlarged stair-step boundary.
                if((b[0]-a[0])*(y+.5-a[1])-(b[1]-a[1])*(x+.5-a[0])
                        <= Math.hypot(b[0]-a[0],b[1]-a[1])) {
                    edge[y*w+x]=true; break;
                }
            }
        }
        // Pixel centers inside the hull can still belong to its staircase boundary.
        // Remove the one-texel boundary band ONLY when connected to exterior black.
        // Internal reticle strokes cannot be traversed once they leave this band.
        var queue=new java.util.ArrayDeque<Integer>();
        for(int y=0;y<h;y++)for(int x=0;x<w;x++)
            if((x==0||y==0||x==w-1||y==h-1)&&edge[y*w+x]){
                remove[y*w+x]=true;queue.add(y*w+x);
            }
        while(!queue.isEmpty()){
            int i=queue.removeFirst(),x=i%w,y=i/w;
            // Diagonal staircase pixels can touch the exterior only at a corner.
            // Connectivity is expanded, but traversal still cannot leave the edge band.
            for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++) {
                int nx=x+dx,ny=y+dy;
                if(nx<0||ny<0||nx>=w||ny>=h)continue;
                int k=ny*w+nx;
                if(edge[k]&&!remove[k]){remove[k]=true;queue.add(k);}
            }
        }
        return remove;
    }
    private static double cross(double[] a,double[] b,double[] p) {
        return (b[0]-a[0])*(p[1]-a[1])-(b[1]-a[1])*(p[0]-a[0]);
    }
}
