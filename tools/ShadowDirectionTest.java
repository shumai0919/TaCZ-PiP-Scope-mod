import com.shumai.taczpip.client.*;
public final class ShadowDirectionTest {
    static LensBounds lens(float tx,float ty) {
        return new LensBounds(.5f,.5f,.2f,.2f,new float[]{.3f,.3f,.7f,.3f,.7f,.7f,.3f,.7f},false,.8f,tx,ty);
    }
    public static void main(String[] args) {
        var up=EyeBoxState.measure(lens(0,.5f),1,1920,1080);
        var down=EyeBoxState.measure(lens(0,-.5f),1,1920,1080);
        var side=EyeBoxState.measure(lens(.5f,0),1,1920,1080);
        if(up.shiftY()>=0||down.shiftY()<=0||side.shiftX()>=0||up.shiftX()!=0||side.shiftY()!=0)
            throw new AssertionError("pitch/yaw signed response");
        var a=PupilShape.planes(lens(0,0),.5f,1920,1080);
        var b=PupilShape.planes(lens(0,0),.5f,1920,1080,0,up.shiftY());
        for(int i=0;i<a.length;i+=4)
            if(Math.abs((b[i+2]-a[i+2])-a[i+1]*up.shiftY())>.001)
                throw new AssertionError("common mask translation");
        System.out.println("ShadowDirection: signed vertical/horizontal response and identical plane translation passed");
    }
}
