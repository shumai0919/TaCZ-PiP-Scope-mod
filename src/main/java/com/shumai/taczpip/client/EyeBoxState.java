package com.shumai.taczpip.client;

/** Per-frame/per-ocular visibility, independent of the aperture silhouette and reticle. */
public record EyeBoxState(boolean limited,float radius,float visibility,float shiftX,float shiftY) {
    public EyeBoxState(boolean limited,float radius,float visibility) {this(limited,radius,visibility,0,0);}
    public static EyeBoxState measure(LensBounds b,float ads,int width,int height) {
        if(b==null||!b.valid()||!Float.isFinite(ads)||width<=0||height<=0)
            return new EyeBoxState(true,0,0); // no speculative unmasked view
        double offset=Math.hypot((b.x()-.5)/b.rx(),(b.y()-.5)/b.ry());
        double position=1-smooth(.12,1.5,offset);
        double angle=1-smooth(.02,.38,1-b.facing());
        float visibility=(float)Math.min(Math.max(0,Math.min(1,ads)),Math.min(position,angle));
        return new EyeBoxState(visibility<.98f,b.pupilRadius(visibility,width,height),visibility,
                -b.tiltX()*b.rx()*width,-b.tiltY()*b.ry()*height);
    }
    private static double smooth(double a,double b,double v){
        double t=Math.max(0,Math.min(1,(v-a)/(b-a)));return t*t*(3-2*t);
    }
}
