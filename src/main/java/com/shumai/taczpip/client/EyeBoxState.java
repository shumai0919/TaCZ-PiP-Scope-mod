package com.shumai.taczpip.client;

/** Per-frame/per-ocular visibility, independent of the aperture silhouette and reticle. */
public record EyeBoxState(boolean limited,float radius,float visibility,float shiftX,float shiftY) {
    public EyeBoxState(boolean limited,float radius,float visibility) {this(limited,radius,visibility,0,0);}
    public static EyeBoxState measure(LensBounds b,float ads,int width,int height) {
        if(b==null||!b.valid()||!Float.isFinite(ads)||width<=0||height<=0)
            return new EyeBoxState(true,0,0); // no speculative unmasked view
        if(b.eye()!=null) {
            var eye=b.eye();
            if(!eye.visible()||!eye.finite()) return new EyeBoxState(true,0,0);
            // Misalignment is represented only by pupil/aperture overlap, not a
            // second, competing position/angle shrink. ADS controls entry alone.
            float opening=Math.max(0,Math.min(1,ads));
            float radius=(Math.max(b.rx()*width,b.ry()*height)+2)*opening;
            return new EyeBoxState(true,radius,opening,
                    (eye.pupilX()-.5f)*width,(eye.pupilY()-.5f)*height);
        }
        double offset=Math.hypot((b.x()-.5)/b.rx(),(b.y()-.5)/b.ry());
        double position=1-smooth(0,1.5,offset);
        double angle=1-smooth(0,.38,1-b.facing());
        float visibility=(float)Math.min(Math.max(0,Math.min(1,ads)),Math.min(position,angle));
        // Keep the gate active: switching it off near full ADS hides small sway.
        // ADS opens the pupil; it must not expand it to cancel eye misalignment.
        float radius=visibility<=0 ? 0 : Math.max(b.rx()*width,b.ry()*height)*visibility+2*visibility;
        return new EyeBoxState(true,radius,visibility,
                -b.tiltX()*b.rx()*width,-b.tiltY()*b.ry()*height);
    }
    private static double smooth(double a,double b,double v){
        double t=Math.max(0,Math.min(1,(v-a)/(b-a)));return t*t*(3-2*t);
    }
}
