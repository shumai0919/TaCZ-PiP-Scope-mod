package com.shumai.taczpip.client;

import java.util.*;

/** Convex projected aperture envelope. Exact concavity/holes remain in the original stencil. */
public final class PupilShape {
    public static final int LIMIT = 16;
    private PupilShape() {}
    public static float[] hull(List<double[]> points) {
        var sorted = new ArrayList<double[]>();
        for (var p : points) {
            if (!Double.isFinite(p[0]) || !Double.isFinite(p[1])) return new float[0];
            sorted.add(p);
        }
        sorted.sort(Comparator.<double[]>comparingDouble(p -> p[0]).thenComparingDouble(p -> p[1]));
        var unique = new ArrayList<double[]>();
        for (var p : sorted) if (unique.isEmpty() || !Arrays.equals(p, unique.get(unique.size()-1))) unique.add(p);
        if (unique.size() < 3) return new float[0];
        var h = new ArrayList<double[]>();
        for (var p : unique) {
            while (h.size() >= 2 && cross(h.get(h.size()-2), h.get(h.size()-1), p) <= 0) h.remove(h.size()-1);
            h.add(p);
        }
        int lower = h.size();
        for (int i=unique.size()-2; i>=0; i--) {
            var p = unique.get(i);
            while (h.size() > lower && cross(h.get(h.size()-2), h.get(h.size()-1), p) <= 0) h.remove(h.size()-1);
            h.add(p);
        }
        h.remove(h.size()-1);
        if (h.size() < 3) return new float[0];
        // Keep a polygon for detailed apertures instead of switching to a circle as
        // projection/animation changes the number of extreme vertices. Removing a
        // convex vertex yields an inscribed polygon: never expands beyond the opening.
        while(h.size()>LIMIT) {
            int remove=0; double smallest=Double.POSITIVE_INFINITY;
            for(int i=0;i<h.size();i++) {
                double area=cross(h.get((i+h.size()-1)%h.size()),h.get(i),h.get((i+1)%h.size()));
                if(area<smallest){smallest=area;remove=i;}
            }
            h.remove(remove);
        }
        float[] result = new float[h.size()*2];
        for (int i=0; i<h.size(); i++) { result[i*2]=(float)h.get(i)[0]; result[i*2+1]=(float)h.get(i)[1]; }
        return result;
    }
    private static double cross(double[] a, double[] b, double[] c) {
        return (b[0]-a[0])*(c[1]-a[1])-(b[1]-a[1])*(c[0]-a[0]);
    }
    public static float[] aperturePlanes(LensBounds b,int width,int height) {
        if(b==null || !b.valid() || !b.alphaShaped() || b.outline().length<6) return new float[0];
        float[] v=b.outline(), result=new float[v.length/2*4];
        for(int i=0;i<v.length/2;i++) {
            int next=(i+1)%(v.length/2);
            double dx=(v[next*2]-v[i*2])*width, dy=(v[next*2+1]-v[i*2+1])*height;
            double len=Math.hypot(dx,dy);
            if(len<.0001) return new float[0];
            double nx=-dy/len,ny=dx/len;
            result[i*4]=(float)nx; result[i*4+1]=(float)ny;
            result[i*4+2]=(float)(nx*(v[i*2]-.5)*width+ny*(v[i*2+1]-.5)*height);
        }
        return result;
    }
    /** Pixel-space inward planes. Expand far enough to contain every aperture corner at settled ADS. */
    public static float[] planes(LensBounds b, float progress, int width, int height) {
        return planes(b,progress,width,height,0,0);
    }
    public static float[] planes(LensBounds b,float progress,int width,int height,float shiftX,float shiftY) {
        if (b == null || !b.valid() || b.outline().length < 6 || !Float.isFinite(progress)) return new float[0];
        float[] vertices = b.outline(), planes = new float[vertices.length/2*4];
        // The bounding-box center can lie outside a perspective-skewed aperture.
        // A convex vertex average is always inside, so valid polygons do not turn
        // into the circular fallback merely because the gun tilts during reload.
        double centerX=0,centerY=0;
        for(int i=0;i<vertices.length;i+=2){centerX+=vertices[i];centerY+=vertices[i+1];}
        centerX/=vertices.length/2;centerY/=vertices.length/2;
        double p = Math.max(0, Math.min(1, progress));
        double t = Math.max(0, Math.min(1, (p-.85)/.13));
        t = t*t*(3-2*t);
        double offsetX=(b.x()-.5)*width-shiftX, offsetY=(b.y()-.5)*height-shiftY;
        for (int i=0; i<vertices.length/2; i++) {
            int next=(i+1)%(vertices.length/2);
            double x=(vertices[i*2]-centerX)*width, y=(vertices[i*2+1]-centerY)*height;
            double dx=(vertices[next*2]-vertices[i*2])*width, dy=(vertices[next*2+1]-vertices[i*2+1])*height;
            double length=Math.hypot(dx,dy);
            if (length < .0001) return new float[0];
            double nx=-dy/length, ny=dx/length;
            double c=nx*x+ny*y;
            if (c >= -.0001) return new float[0]; // bounds center must be strictly inside
            // Include the original bounds, not only the simplified outline, before
            // dropping the ADS gate. Otherwise removed hull vertices can pop in.
            double full=Math.max(1,(Math.abs(nx*offsetX+ny*offsetY)
                    +Math.abs(nx)*b.rx()*width+Math.abs(ny)*b.ry()*height+2)/(-c));
            double scale=p+(full-p)*t;
            planes[i*4]=(float)nx; planes[i*4+1]=(float)ny;
            planes[i*4+2]=(float)(c*scale+nx*shiftX+ny*shiftY);
        }
        return planes;
    }
}
