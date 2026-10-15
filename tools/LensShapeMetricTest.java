import com.shumai.taczpip.client.*;
public final class LensShapeMetricTest {
    static double radius(float[] c,double x,double y) {
        double r=0;for(int i=0;i<c.length;i+=2)r=Math.max(r,c[i]*x+c[i+1]*y);return r;
    }
    static void near(double a,double b){if(Math.abs(a-b)>.0001)throw new AssertionError(a+" != "+b);}
    public static void main(String[] args) {
        var square=new LensBounds(.5f,.5f,.2f,.1f,new float[]{.3f,.4f,.7f,.4f,.7f,.6f,.3f,.6f});
        var s=LensShapeMetric.coefficients(square);
        near(radius(s,0,0),0);near(radius(s,.2,.1),1);near(radius(s,.2,0),1);near(radius(s,.1,.05),.5);
        for(int count:new int[]{4,6,16})for(double rotation:new double[]{0,.21,.67}) {
            float[] vertices=new float[count*2];
            for(int i=0;i<count;i++){double a=i*2*Math.PI/count+rotation;vertices[i*2]=(float)(.5+.2*Math.cos(a));vertices[i*2+1]=(float)(.5+.2*Math.sin(a));}
            var c=LensShapeMetric.coefficients(new LensBounds(.5f,.5f,.2f,.2f,vertices));
            for(int i=0;i<count;i++){near(radius(c,vertices[i*2]-.5,vertices[i*2+1]-.5),1);}
            if(count==16)for(int i=0;i<360;i++) {
                double a=Math.toRadians(i),r=radius(c,.1*Math.cos(a),.1*Math.sin(a));
                if(Math.abs(r-.5)>.011)throw new AssertionError("round appearance changed too much");
            }
        }
        if(LensShapeMetric.coefficients(LensBounds.INVALID).length!=0)throw new AssertionError("invalid fallback");
        System.out.println("LensShapeMetric: rectangle corners/edges, rotated polygons, circle within 2.2%, center and fallback passed");
    }
}
