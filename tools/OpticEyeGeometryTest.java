import com.shumai.taczpip.client.*;
public class OpticEyeGeometryTest {
    static void near(double a,double b) {if(Math.abs(a-b)>1e-6) throw new AssertionError(a+" != "+b);}
    static EyeProjection project(double x,double y,double z,double nx,double ny,double nz) {
        var p=OpticEyeGeometry.pupil(x,y,z,nx,ny,nz,.2);
        if(!p.valid()||p.z()>=0) return EyeProjection.CLOSED;
        return new EyeProjection((float)(.5-x/z*.5),(float)(.5-y/z*.5),
                (float)(.5-p.x()/p.z()*.5),(float)(.5-p.y()/p.z()*.5),true);
    }
    public static void main(String[] args) {
        var straight=OpticEyeGeometry.pupil(0,0,-2,0,0,1,.2);
        near(straight.x(),0);near(straight.y(),0);near(straight.z(),-2);
        // Translation and rotation that keep the eye on the axis must cancel,
        // even when the optic is not at screen center (previous algorithm did not).
        var aligned=OpticEyeGeometry.pupil(.4,.3,-2,-.4,-.3,2,.2);
        near(aligned.x(),.4);near(aligned.y(),.3);near(aligned.z(),-2);
        var side=OpticEyeGeometry.pupil(.1,0,-2,0,0,1,.2);
        var vertical=OpticEyeGeometry.pupil(0,.1,-2,0,0,1,.2);
        near(side.x(),vertical.y());near(side.y(),vertical.x());
        if(side.x()>=.1) throw new AssertionError("pupil must move toward eye");
        // Model scale and the coordinate frame cannot alter the optical result.
        var scaled=OpticEyeGeometry.pupil(1,0,-20,0,0,1,2);
        near(scaled.x()/10,side.x());near(scaled.z()/10,side.z());
        var rotated=OpticEyeGeometry.pupil(0,.1,-2,0,0,1,.2);
        near(rotated.y(),side.x());
        if(OpticEyeGeometry.pupil(0,0,2,0,0,1,.2).valid() ||
                OpticEyeGeometry.pupil(Double.NaN,0,-2,0,0,1,.2).valid()) throw new AssertionError("invalid eye");
        for(int sides:new int[]{4,8,16}) {
            float previousX=0,previousY=0;
            for(int frame=0;frame<=200;frame++) {
                double phase=frame/200.0*Math.PI;
                double x=.12*Math.sin(phase),y=.08*Math.sin(phase);
                double angle=.18*Math.sin(phase);
                var eye=project(x,y,-2,Math.sin(angle),0,Math.cos(angle));
                if(!eye.visible())throw new AssertionError("reload projection invalid");
                float[] outline=new float[2*sides];
                for(int v=0;v<sides;v++) {
                    outline[2*v]=eye.centerX()+.05f*(float)Math.cos(v*2*Math.PI/sides);
                    outline[2*v+1]=eye.centerY()+.05f*(float)Math.sin(v*2*Math.PI/sides);
                }
                var lens=new LensBounds(eye.centerX(),eye.centerY(),.05f,.05f,outline,true,1,0,0,eye);
                var state=EyeBoxState.measure(lens,1,1000,1000);
                near(state.visibility(),1); // no extra contraction due to sideways motion
                var planes=PupilShape.eyePlanes(lens,state.visibility(),1000,1000,state.shiftX(),state.shiftY());
                if(planes.length!=sides*4)throw new AssertionError("shape changed");
                for(float f:planes)if(!Float.isFinite(f))throw new AssertionError("invalid plane");
                if(frame>0&&(Math.abs(state.shiftX()-previousX)>2||Math.abs(state.shiftY()-previousY)>2))
                    throw new AssertionError("reload discontinuity");
                previousX=state.shiftX();previousY=state.shiftY();
            }
        }
        System.out.println("OpticEyeGeometry: optical alignment, translation/rotation cancellation, scale, pitch/yaw, invalid input and reload continuity passed");
    }
}
