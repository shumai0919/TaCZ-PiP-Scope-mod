import com.shumai.taczpip.client.*;
public final class ReticleOpeningTest {
    public static void main(String[] args) {
        int n=128;
        for(boolean circle:new boolean[]{true,false}) {
            int[] pixels=new int[n*n];
            for(int y=0;y<n;y++)for(int x=0;x<n;x++) {
                boolean inside=circle ? Math.hypot(x-63.5,y-63.5)<54
                        : x>=10&&x<118&&y>=10&&y<118;
                pixels[y*n+x]=inside ? 0 : 0xff000000;
                // Interior black crosshair must not define the outline.
                if(x==64||y==64)pixels[y*n+x]=0xff000000;
            }
            float[] outline=ReticleOpening.trace(pixels,n,n);
            if(circle ? outline.length<=8 : outline.length!=8)
                throw new AssertionError("shape swapped, circle="+circle);
            var points=new java.util.ArrayList<double[]>();
            for(int i=0;i<outline.length;i+=2)
                points.add(new double[]{.2+outline[i]*.4,.3+outline[i+1]*.2});
            var bounds=new LensBounds(.4f,.4f,.2f,.1f,PupilShape.hull(points),true);
            float[] planes=PupilShape.aperturePlanes(bounds,1920,1080);
            if(planes.length!=outline.length*2)throw new AssertionError("lost projected outline");
            for(int i=0;i<planes.length;i+=4) {
                float d=planes[i]*(.4f-.5f)*1920+planes[i+1]*(.4f-.5f)*1080-planes[i+2];
                if(d<=0)throw new AssertionError("translated center not inside");
            }
        }
        System.out.println("ReticleOpening: round/square, interior crosshair and translated projection passed");
    }
}
