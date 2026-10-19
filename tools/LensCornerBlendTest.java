public final class LensCornerBlendTest {
    static double radius(double[] edges) {
        double max=0;for(double v:edges)max=Math.max(max,v);
        double sum=0;for(double v:edges)sum+=Math.exp((v-max)/.035);
        double t=Math.min(1,max/.25);t=t*t*(3-2*t);
        return max+.035*Math.log(Math.max(1,sum))*t;
    }
    static double square(double t) {return radius(new double[]{.65+t,.65-t,-.65-t,-.65+t});}
    public static void main(String[] args) {
        double h=1e-5,center=square(0);
        double left=(center-square(-h))/h,right=(square(h)-center)/h;
        if(Math.abs(right-left)>.002)throw new AssertionError("corner gradient seam");
        if(radius(new double[]{0,0,0,0})!=0)throw new AssertionError("center changed");
        if(Math.abs(radius(new double[]{.7,.1,-.7,-.1})-.7)>1e-6)throw new AssertionError("unrelated edge changed");
        double[] e={.8,.79,-.8,-.79};double a=radius(e);
        if(a!=radius(new double[]{-.79,.79,.8,-.8}))throw new AssertionError("edge order");
        if(!Double.isFinite(radius(new double[]{10000,-10000,0,0})))throw new AssertionError("overflow");
        if(a<.8||a>.8+.035*Math.log(4))throw new AssertionError("bounded correction");
        System.out.println("LensCornerBlend: corner gradient continuous, center/isolated edge preserved, symmetric and finite");
    }
}
