import com.shumai.taczpip.client.*;
public final class NormalizedOpeningTest {
    public static void main(String[] args) {
        for(int count:new int[]{4,8,16}) {
            float[] a=new float[count*2],b=new float[count*2];
            for(int i=0;i<count;i++) {
                a[i*2]=(float)(.5+.4*Math.cos(i*2*Math.PI/count));
                a[i*2+1]=(float)(.5+.4*Math.sin(i*2*Math.PI/count));
                b[i*2]=a[i*2]*3+10;b[i*2+1]=a[i*2+1]*3-7;
            }
            float[] x=NormalizedOpening.fit(a),y=NormalizedOpening.fit(b);
            if(x.length!=count*2||y.length!=x.length)throw new AssertionError("lost shape");
            for(int i=0;i<x.length;i++)if(Math.abs(x[i]-y[i])>1e-5)
                throw new AssertionError("source offset/size affected shape");
        }
        if(NormalizedOpening.fit(new float[]{0,0,0,1,0,2}).length!=0)
            throw new AssertionError("degenerate shape");
        System.out.println("NormalizedOpening: contour retained; source offset/scale independent; invalid rejected");
    }
}
