import com.shumai.taczpip.client.*;
public class SwayOcclusionTest {
    static LensBounds lens(int sides,float dx,float dy,float tx,float ty) {
        float[] vertices=new float[sides*2];
        for(int i=0;i<sides;i++) {
            double a=2*Math.PI*i/sides;
            vertices[2*i]=.5f+dx+.2f*(float)Math.cos(a);
            vertices[2*i+1]=.5f+dy+.2f*(float)Math.sin(a);
        }
        return new LensBounds(.5f+dx,.5f+dy,.2f,.2f,vertices,true,1,tx,ty);
    }
    static double margin(LensBounds b,int vertex) {
        var s=EyeBoxState.measure(b,1,1000,1000);
        var planes=PupilShape.eyePlanes(b,s.visibility(),1000,1000,s.shiftX(),s.shiftY());
        double result=Double.POSITIVE_INFINITY;
        for(int e=0;e<planes.length;e+=4)
            result=Math.min(result,planes[e]*(b.outline()[vertex*2]-.5)*1000+
                    planes[e+1]*(b.outline()[vertex*2+1]-.5)*1000-planes[e+2]);
        return result;
    }
    public static void main(String[] args) {
        for(int sides:new int[]{4,8,16}) {
            var aligned=lens(sides,0,0,0,0);
            for(int v=0;v<sides;v++) if(margin(aligned,v)<0) throw new AssertionError("aligned corner clipped");
            if(margin(lens(sides,.01f,0,0,0),0)>=0) throw new AssertionError("horizontal sway hidden");
            if(margin(lens(sides,0,.01f,0,0),sides/4)>=0) throw new AssertionError("vertical sway hidden");
            if(margin(lens(sides,0,0,.05f,0),0)>=0) throw new AssertionError("yaw hidden");
            if(margin(lens(sides,0,0,0,.05f),sides/4)>=0) throw new AssertionError("pitch hidden");
            double previous=margin(aligned,0);
            for(int step=1;step<=100;step++) {
                double m=margin(lens(sides,step*.0002f,0,0,0),0);
                if(m>previous+.001 || previous-m>1) throw new AssertionError("discontinuous sway");
                previous=m;
            }
        }
        System.out.println("SwayOcclusion: aligned corners, horizontal/vertical sway, pitch/yaw and continuity passed");
    }
}
