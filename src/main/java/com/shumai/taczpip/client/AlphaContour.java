package com.shumai.taczpip.client;

/** Conservative radial approximation for simple centered transparent apertures only. */
public final class AlphaContour {
    private AlphaContour() {}
    public static float[] trace(boolean[] mask,int w,int h) {
        if(w<2 || h<2 || mask.length!=w*h) return new float[0];
        int filled=0; for(boolean p:mask) if(p) filled++;
        if(filled==w*h || filled< w*h*.4 || !mask[(h/2)*w+w/2]) return new float[0];
        // Reject asymmetry, holes and disconnected radial islands. Keep original stencil in those cases.
        for(int y=0;y<h;y++) for(int x=0;x<w;x++)
            if(mask[y*w+x]!=mask[y*w+w-1-x] || mask[y*w+x]!=mask[(h-1-y)*w+x]) return new float[0];
        float[] result=new float[32];
        for(int i=0;i<16;i++) {
            double angle=2*Math.PI*i/16, dx=Math.cos(angle),dy=Math.sin(angle);
            double radius=0; boolean outside=false;
            for(int j=0;j<=512;j++) {
                double r=j/512.0*.72;
                int x=(int)Math.floor((.5+dx*r)*w), y=(int)Math.floor((.5+dy*r)*h);
                boolean inside=x>=0&&x<w&&y>=0&&y<h&&mask[y*w+x];
                if(!inside) outside=true;
                else { if(outside) return new float[0]; radius=r; }
            }
            // Inset by half a source texel to hide rather than enlarge coarse alpha steps.
            radius=Math.max(0,radius-.5/Math.min(w,h));
            result[i*2]=(float)(.5+dx*radius); result[i*2+1]=(float)(.5+dy*radius);
        }
        // Validate connecting segments stay within the original alpha (do not bridge concave cuts).
        for(int i=0;i<16;i++) for(int j=0;j<=32;j++) {
            int next=(i+1)%16; double t=j/32.0;
            int x=(int)((result[i*2]*(1-t)+result[next*2]*t)*w);
            int y=(int)((result[i*2+1]*(1-t)+result[next*2+1]*t)*h);
            if(x<0||x>=w||y<0||y>=h||!mask[y*w+x]) return new float[0];
        }
        return result;
    }
}
