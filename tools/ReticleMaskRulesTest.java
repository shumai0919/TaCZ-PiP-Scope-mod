import com.shumai.taczpip.client.ReticleMaskRules;
import java.io.File;
import javax.imageio.ImageIO;
public class ReticleMaskRulesTest {
    static void check(boolean v,String s){if(!v)throw new AssertionError(s);}
    public static void main(String[] args)throws Exception {
        int n=64; int[] image=new int[n*n];
        for(int y=0;y<n;y++)for(int x=0;x<n;x++) {
            boolean border=Math.hypot(x-31.5,y-31.5)>27;
            image[y*n+x]=border||x==32||y==32?0xff000000:0;
        }
        image[2*n+2]=0xff0000ff; // colored marker outside clear area must survive
        boolean[] remove=ReticleMaskRules.exterior(image,n,n);
        check(remove[0],"outer black removed");
        check(!remove[2*n+2],"colored marks retained");
        for(int i=7;i<57;i++){check(!remove[32*n+i],"horizontal reticle");check(!remove[i*n+32],"vertical reticle");}
        int[] onlyLines=new int[n*n];
        for(int i=0;i<n;i++)onlyLines[i*n+32]=0xff000000;
        for(boolean b:ReticleMaskRules.exterior(onlyLines,n,n))check(!b,"no exterior: unchanged");
        for(String name:new String[]{"scope_standard_8x","scope_acog_ta31","scope_lpvo_1_6"}) {
            var png=ImageIO.read(new File(args[0]+"/"+name+".png"));
            int startX=name.equals("scope_standard_8x")?256:128;
            int startY=name.equals("scope_lpvo_1_6")?128:0;
            int size=name.equals("scope_standard_8x")?256:name.equals("scope_lpvo_1_6")?384:128;
            if(name.equals("scope_acog_ta31")){startX*=2;startY*=2;size*=2;}
            int[] data=new int[size*size];
            for(int y=0;y<size;y++)for(int x=0;x<size;x++){
                int argb=png.getRGB(startX+x,startY+y); // convert to NativeImage ABGR
                data[y*size+x]=(argb&0xff00ff00)|((argb>>16)&255)|((argb&255)<<16);
            }
            boolean[] mask=ReticleMaskRules.exterior(data,size,size); int count=0;
            for(int i=0;i<mask.length;i++)if(mask[i])count++;
            check(count>0,"real mask recognized "+name);
            System.out.println(name+": exterior pixels separated="+count);
        }
        System.out.println("Reticle mask: exterior, central crosshair, colored markers, unchanged fallback passed");
    }
}
