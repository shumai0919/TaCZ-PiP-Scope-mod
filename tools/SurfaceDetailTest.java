/** CPU reference for shader coverage/blend bounds; not a GPU render test. */
public class SurfaceDetailTest {
    static double smooth(double a,double b,double x) {
        double t=Math.max(0,Math.min(1,(x-a)/(b-a)));return t*t*(3-2*t);
    }
    static double coverage(double distance,double footprint) {
        double width=.0015,aa=Math.max(footprint,.0001);
        return (1-smooth(Math.max(0,width-aa),width+aa,distance))*Math.min(1,width/aa);
    }
    public static void main(String[] args) {
        for(double density:new double[]{0,.01,.5,1,1.01,2,4}) {
            double count=12*density,total=0;
            for(int i=0;i<48;i++)total+=Math.max(0,Math.min(1,count-i));
            if(Math.abs(total-count)>1e-9)throw new AssertionError("density count/fade");
        }
        for(double size:new double[]{.0001,.1,1,4}) {
            double halfLength=.18*size,width=.004*size;
            if(!Double.isFinite(1/halfLength)||width<=0)throw new AssertionError("size stability");
            if(Math.abs(halfLength/width-45)>1e-9)throw new AssertionError("proportional size");
        }
        for(double strength:new double[]{0,.1,.12,.5,1})
        for(double source:new double[]{0,.01,.25,.75,1})
        for(double protect:new double[]{0,.2,1})
        for(double smudge:new double[]{0,.1,.5,1}) {
            double dirty=source*(1-strength*smudge*protect*.35);
            for(int sample=0;sample<=100;sample++) {
                double mark=coverage(sample*.0001,.002)*strength*protect;
                double light=smooth(.05,.75,dirty);
                double result=dirty+(1-dirty)*mark*light*.9;
                if(!Double.isFinite(result)||result<0||result>1)throw new AssertionError("blend bounds");
                if((strength==0||protect==0)&&result!=source)throw new AssertionError("bypass/aim protection");
            }
        }
        double previous=1;
        for(int i=1;i<=1000;i++) {
            double c=coverage(0,i*.0001);
            if(c<0||c>previous+1e-12)throw new AssertionError("subpixel attenuation");
            previous=c;
        }
        if(coverage(.2,.001)!=0)throw new AssertionError("finite stroke extent");
        for(int i=0;i<=45;i++) if(smooth(.45,.85,i/100.0)!=0)throw new AssertionError("clean center");
        if(smooth(.45,.85,.85)!=1)throw new AssertionError("peripheral intensity");
        for(int i=0;i<=18;i++) if(smooth(.18,.42,i/100.0)!=0)throw new AssertionError("scratch clean center");
        if(smooth(.18,.42,.3)<=0 || smooth(.18,.42,.5)!=1)throw new AssertionError("middle scratches missing");
        System.out.println("SurfaceDetail: bounded blend, zero bypass, aim protection and subpixel attenuation passed (CPU reference)");
    }
}
