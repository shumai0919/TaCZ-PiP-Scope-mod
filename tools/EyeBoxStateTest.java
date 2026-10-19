import com.shumai.taczpip.client.EyeBoxState;
import com.shumai.taczpip.client.LensBounds;

public final class EyeBoxStateTest {
    private static LensBounds lens(float x, float facing) {
        return new LensBounds(x,.5f,.15f,.2f,new float[0],false,facing);
    }
    private static EyeBoxState state(float x,float facing,float ads) {
        return EyeBoxState.measure(lens(x,facing),ads,1920,1080);
    }
    public static void main(String[] args) {
        if(state(.5f,1,1).visibility()!=1) throw new AssertionError("aligned visibility");
        if(!state(.505f,1,1).limited() || state(.505f,1,1).visibility()>=1) throw new AssertionError("sway suppressed");
        if(!state(.65f,1,1).limited()) throw new AssertionError("ADS=1 bypasses displacement");
        if(!state(.5f,.8f,1).limited()) throw new AssertionError("ADS=1 bypasses angle");
        if(state(.5f,1,0).radius()!=0) throw new AssertionError("hip fire");
        if(EyeBoxState.measure(LensBounds.INVALID,1,1920,1080).radius()!=0)
            throw new AssertionError("invalid geometry exposed");
        if(state(.5f,1,Float.NaN).radius()!=0) throw new AssertionError("invalid ADS");
        float previous=1;
        for(int i=0;i<=100;i++) {
            var s=state(.5f+i*.003f,1,1);
            if(s.visibility()>previous+.00001f || !Float.isFinite(s.radius()))
                throw new AssertionError("displacement response");
            previous=s.visibility();
        }
        var a=EyeBoxState.measure(lens(.65f,1),1,1920,1080);
        var b=EyeBoxState.measure(lens(.65f,1),1,960,540);
        if(a.visibility()!=b.visibility() || Math.abs(a.radius()-2*b.radius())>2.01f)
            throw new AssertionError("resolution response");
        if(state(.5f,1,1).visibility()!=1) throw new AssertionError("state leaked between lenses");
        System.out.println("EyeBoxState: displacement/angle at full ADS, small sway, invalid input, resolution and isolation passed");
    }
}
