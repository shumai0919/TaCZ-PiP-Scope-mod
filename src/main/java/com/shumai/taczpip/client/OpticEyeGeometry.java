package com.shumai.taczpip.client;

/** Camera at view-space origin. A normalized virtual eye box, not a lens ray tracer. */
public final class OpticEyeGeometry {
    private OpticEyeGeometry() {}
    public record Pupil(double x,double y,double z,boolean valid) {}
    public static Pupil pupil(double cx,double cy,double cz,double nx,double ny,double nz,double radius) {
        double length=Math.sqrt(nx*nx+ny*ny+nz*nz);
        if(!Double.isFinite(cx+cy+cz+length+radius)||length<1e-9||radius<=0)
            return new Pupil(0,0,0,false);
        nx/=length;ny/=length;nz/=length;
        // The eye's axial and transverse coordinates in the optical frame.
        double axial=-(cx*nx+cy*ny+cz*nz);
        if(axial<=radius*.01) return new Pupil(0,0,0,false);
        double ex=-cx-axial*nx,ey=-cy-axial*ny,ez=-cz-axial*nz;
        // Four aperture radii is a shared virtual relief, independent of pack scale
        // and of the arbitrary distance between ocular and division model faces.
        double scale=4*radius/axial;
        return new Pupil(cx+ex*scale,cy+ey*scale,cz+ez*scale,true);
    }
}
