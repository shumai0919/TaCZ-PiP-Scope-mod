import com.shumai.taczpip.client.MaskFeatherRules;
public final class MaskFeatherRulesTest {
    static void check(boolean v) { if (!v) throw new AssertionError(); }
    public static void main(String[] args) {
        check(MaskFeatherRules.width(100,100,.5f,0)==0);
        check(MaskFeatherRules.width(Float.NaN,100,.5f,.12)==0);
        check(MaskFeatherRules.width(100,100,.98f,.12)==0);
        check(MaskFeatherRules.width(100,100,.5f,1)==100);
        for(float radius : new float[]{.1f,1,100,300,1000}) for(double strength : new double[]{.001,.12,.4,.75,1}) {
            float w=MaskFeatherRules.width(radius,300,.5f,strength);
            check(w>0 && w<=Math.min(radius,300));
            double prev=0;
            for(int i=0;i<=1000;i++) {
                double distance=radius*i/1000.0;
                double t=Math.max(0,Math.min(1,(distance-(radius-w))/w));
                double alpha=t*t*(3-2*t);
                check(alpha>=prev && alpha>=0 && alpha<=1); prev=alpha;
            }
            check(Math.abs(prev-1)<.0001);
            float last=Float.MAX_VALUE;
            for(int i=900;i<=980;i++) {
                float next=MaskFeatherRules.width(radius,300,i/1000f,strength);
                check(next<=last); last=next;
            }
            check(last==0);
            check(Math.abs(MaskFeatherRules.width(radius*2,600,.5f,strength)-2*w)<.001);
        }
        System.out.println("Mask feather: zero/invalid, inward width, smooth coverage, resolution scaling, settled ADS fade passed");
    }
}
