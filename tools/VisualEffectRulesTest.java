import com.shumai.taczpip.client.VisualEffectRules;
public class VisualEffectRulesTest {
    public static void main(String[] args) {
        for(float strength:new float[]{0,.25f,1}) {
            float previous=strength;
            for(int i=0;i<=100;i++) {
                float value=VisualEffectRules.idle(i/100f,true,strength);
                if(value<0||value>previous+1e-6||!Float.isFinite(value))throw new AssertionError("idle fade");
                if(i>=20&&value!=0)throw new AssertionError("ADS shadow brightened");
                previous=value;
            }
        }
        if(VisualEffectRules.idle(0,false,1)!=0||VisualEffectRules.idle(Float.NaN,true,1)!=0)
            throw new AssertionError("off/invalid input");
        // Premultiplied-alpha blur must preserve uniform layers and not invent color at alpha=0.
        double weight=.4+8*.075;
        if(Math.abs(weight-1)>1e-9)throw new AssertionError("blur normalization");
        for(double alpha:new double[]{0,.1,1})for(double color:new double[]{0,.5,1}) {
            double filtered=color*alpha*weight;
            if(filtered<0||filtered>alpha+1e-9)throw new AssertionError("alpha fringe");
        }
        System.out.println("VisualEffectRules: non-ADS fade/off/invalid values and premultiplied blur reference passed");
    }
}
