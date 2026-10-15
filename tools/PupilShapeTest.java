import com.shumai.taczpip.client.*;
import java.util.*;
public class PupilShapeTest {
    static void check(boolean ok) { if(!ok) throw new AssertionError(); }
    public static void main(String[] args) {
        check(PupilShape.hull(List.of(new double[]{Double.NaN,0})).length==0);
        for(int sides : new int[]{4,6,8,12,16,24,32}) for(double angle : new double[]{0,.2,.8}) {
            var points=new ArrayList<double[]>();
            for(int i=0;i<sides;i++) {
                double a=angle+i*2*Math.PI/sides;
                points.add(new double[]{.7+.15*Math.cos(a),.3+.12*Math.sin(a)});
            }
            points.add(points.get(0)); // duplicate vertices from cube faces
            var hull=PupilShape.hull(points);
            check(hull.length==Math.min(sides,16)*2);
            var b=new LensBounds(.7f,.3f,.15f,.12f,hull);
            for(int width : new int[]{640,1920,3840}) {
                var full=PupilShape.planes(b,.98f,width,1080);
                check(full.length==Math.min(sides,16)*4);
                for(var v:points) for(int e=0;e<full.length;e+=4)
                    check(full[e]*(v[0]-.5)*width+full[e+1]*(v[1]-.5)*1080-full[e+2]>=-0.01);
                var previous=PupilShape.planes(b,0,width,1080);
                for(int step=1;step<=100;step++) {
                    var planes=PupilShape.planes(b,step/100f,width,1080);
                    for(int e=0;e<planes.length;e+=4) {
                        check(Float.isFinite(planes[e+2]));
                        check(planes[e+2]<=previous[e+2]+.001);
                    }
                    previous=planes;
                }
            }
        }
        // Square retains its corners instead of a circular inset at half ADS.
        var square=PupilShape.hull(List.of(new double[]{.25,.25},new double[]{.75,.25},
                new double[]{.75,.75},new double[]{.25,.75}));
        var edges=PupilShape.planes(new LensBounds(.5f,.5f,.25f,.25f,square),.5f,1000,1000);
        for(int e=0;e<edges.length;e+=4) check(edges[e]*120+edges[e+1]*120-edges[e+2]>0);
        check(PupilShape.planes(LensBounds.INVALID,.5f,1000,1000).length==0);
        // Bounding box center lies on the skewed triangle edge; vertex centroid remains inside.
        var skew=new LensBounds(.5f,.5f,.4f,.4f,new float[]{.1f,.1f,.9f,.2f,.9f,.9f});
        check(PupilShape.planes(skew,.5f,1000,1000).length==12);
        var settled=PupilShape.planes(skew,.98f,1000,1000);
        for(int i=0;i<skew.outline().length;i+=2)for(int e=0;e<settled.length;e+=4)
            check(settled[e]*(skew.outline()[i]-.5)*1000+settled[e+1]*(skew.outline()[i+1]-.5)*1000-settled[e+2]>=-.01);
        System.out.println("PupilShape: polygons, rotated poses, corner coverage, continuity, invalid fallback passed");
    }
}
