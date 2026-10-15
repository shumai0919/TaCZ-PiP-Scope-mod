import com.shumai.taczpip.client.LensBounds;
import com.shumai.taczpip.client.EyeBoxRules;

/** CPU reference of shader math; not a replacement for real-driver GLSL / screenshot tests. */
public final class LensOpticsTest {
    static double smooth(double a, double b, double v) {
        double t = Math.max(0, Math.min(1, (v-a)/(b-a)));
        return t*t*(3-2*t);
    }
    static void check(boolean condition, String text) { if (!condition) throw new AssertionError(text); }
    public static void main(String[] args) {
        check(!LensBounds.INVALID.valid(), "invalid fallback");
        check(!new LensBounds(Float.NaN, .5f, .2f, .2f).valid(), "NaN fallback");
        for (int width : new int[]{640,1920,3840}) for (int height : new int[]{480,1080,2160}) {
            var b = new LensBounds(.625f, .375f, .125f, .25f);
            for (int p=0;p<=100;p++) {
                double expected = EyeBoxRules.radius(p/100f, .125*width, -.125*height, .125*width, .25*height);
                check(Math.abs(b.pupilRadius(p/100f,width,height)-expected)<.001, "legacy pupil unchanged");
            }
            for (double strength : new double[]{0,.025,.08,.20}) for (double caStrength : new double[]{0,.0015,.006,.02}) {
                double previous = -1;
                double previousRed = -1;
                for (int i=0;i<=1000;i++) {
                    double radius=i/1000.0, edge=smooth(.35,1,radius);
                    double guard=smooth(.08,.25,radius), peripheral=edge*edge;
                    double warp=strength*peripheral*guard;
                    double delta=radius*.25;
                    double cap=1.25*Math.max(1,caStrength/.006);
                    double ca=Math.min(caStrength,cap/Math.max(delta*width,1))*peripheral*guard;
                    double sampled=radius*(1-warp);
                    check(sampled>=previous, "no radial foldover"); previous=sampled;
                    check(radius*(1-warp-ca)>=0 && sampled<=radius+1e-12, "inward samples");
                    double red=radius*(1-warp-ca);
                    check(red>=previousRed, "no red channel foldover"); previousRed=red;
                    check(ca*delta*width<=cap+1e-7, "texel cap");
                    double shade=1-(.65*edge+.35*smooth(.82,1.05,radius));
                    check(shade>=0 && shade<=1, "bounded shading");
                    if(radius<=.35) check(warp==0 && ca==0 && shade==1, "sharp bright center");
                    if(strength==0 && caStrength==0) check(sampled==radius, "disabled identity");
                }
            }
        }
        // Extreme settings intentionally fold the image; ensure the test does not ban this feature.
        double prev=-1;
        boolean folded=false;
        for(int i=0;i<=1000;i++) {
            double r=i/1000.0, e=smooth(.35,1,r);
            double sample=r*(1-e*e*smooth(.08,.25,r));
            check(Double.isFinite(sample), "finite extreme UV");
            if(sample<prev) folded=true;
            prev=sample;
        }
        check(folded, "intentional foldover available at strength 1");
        // The screen-center guard must protect the aiming ray even if lens center is offset.
        check(smooth(.08,.25,0)==0, "off-axis aiming ray unchanged");
        System.out.println("LensOptics: legacy pupil, invalid fallback, center, identity, monotonic UV, texel cap passed");
    }
}
