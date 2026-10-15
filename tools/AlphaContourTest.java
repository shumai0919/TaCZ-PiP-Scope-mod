import com.shumai.taczpip.client.*;
import java.util.*;
import javax.imageio.ImageIO;
import java.io.File;
public class AlphaContourTest {
    static void check(boolean ok,String message) { if(!ok) throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        boolean[] square=new boolean[36]; Arrays.fill(square,true);
        check(AlphaContour.trace(square,6,6).length==0,"opaque square must retain geometry");
        boolean[] rounded=square.clone();
        for(int i:new int[]{0,5,30,35}) rounded[i]=false;
        test(rounded,6,6);
        var image=ImageIO.read(new File(args[0]));
        boolean[] actual=new boolean[36];
        for(int y=0;y<6;y++) for(int x=0;x<6;x++) actual[y*6+x]=(image.getRGB(x+22,y+15)>>>24)>=128;
        check(Arrays.equals(rounded,actual),"actual M2 aperture");
        test(actual,6,6);
        rounded[2]=false;
        check(AlphaContour.trace(rounded,6,6).length==0,"asymmetric masks fall back");
        rounded=square.clone(); rounded[21]=false;
        check(AlphaContour.trace(rounded,6,6).length==0,"hole fallback");
        System.out.println("AlphaContour: actual M2 texture, opaque square, symmetry/hole fallback and pixel containment passed");
    }
    static void test(boolean[] mask,int w,int h) {
        float[] contour=AlphaContour.trace(mask,w,h);
        check(contour.length==32,"16-edge alpha contour");
        var points=new ArrayList<double[]>();
        for(int i=0;i<contour.length;i+=2) points.add(new double[]{contour[i],contour[i+1]});
        var hull=PupilShape.hull(points);
        check(hull.length>=6,"nondegenerate hull");
        // Verify the convex hull cannot bridge a transparent source pixel.
        for(int i=0;i<hull.length/2;i++) for(int j=0;j<=128;j++) {
            int n=(i+1)%(hull.length/2); double t=j/128.0;
            int x=(int)((hull[2*i]*(1-t)+hull[2*n]*t)*w);
            int y=(int)((hull[2*i+1]*(1-t)+hull[2*n+1]*t)*h);
            check(mask[y*w+x],"hull containment");
        }
    }
}
