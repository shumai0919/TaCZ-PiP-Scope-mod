import com.shumai.taczpip.client.ReticleMaskRules;
public final class ReticleFringeTest {
    public static void main(String[] args) {
        int n=128;int[] p=new int[n*n];
        for(int y=0;y<n;y++)for(int x=0;x<n;x++) {
            double r=Math.hypot(x-63.5,y-63.5);
            p[y*n+x]=r>55?0xff000000:r>54?0x80000000:0;
        }
        p[64*n+64]=0x80000000; // translucent central dot must survive
        p[2*n+2]=0xff0000ff; // colored exterior marker must survive
        var mask=ReticleMaskRules.exterior(p,n,n);
        int fringe=0;
        for(int i=0;i<p.length;i++)if(p[i]==0x80000000&&i!=64*n+64) {
            if(!mask[i])throw new AssertionError("antialiased exterior rim remains");
            fringe++;
        }
        if(mask[64*n+64]||mask[2*n+2]||fringe==0)throw new AssertionError("reticle protection");
        System.out.println("ReticleFringe: removed "+fringe+" translucent rim pixels, retained interior/colored markers");
    }
}
