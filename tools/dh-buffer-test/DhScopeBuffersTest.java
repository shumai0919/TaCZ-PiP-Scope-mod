import com.shumai.taczpip.client.DhScopeBuffers;
import com.seibel.distanthorizons.core.render.glObject.texture.DhFramebuffer;
import net.minecraft.client.Minecraft;
public class DhScopeBuffersTest {
    public static class Renderer {
        int cachedWidth=1920, cachedHeight=1080;
        Object framebuffer=new DhFramebuffer(), nullableColorTexture=new DhFramebuffer(), depthTexture=new DhFramebuffer();
        boolean usingMcFrameBuffer=false;
    }
    public static void main(String[] args) {
        Renderer r=new Renderer();
        Object mainF=r.framebuffer, mainD=r.depthTexture;
        Object scopeD=null, scopeF=null;
        for(int i=0;i<120;i++) {
            Runnable restore=DhScopeBuffers.swap(r);
            try {
                if(i==0) {
                    check(r.depthTexture==null);
                    r.cachedWidth=960; r.cachedHeight=540;
                    r.depthTexture=new DhFramebuffer(); r.nullableColorTexture=new DhFramebuffer();
                    scopeD=r.depthTexture; scopeF=r.framebuffer;
                } else { check(r.depthTexture==scopeD && r.framebuffer==scopeF); }
            } finally { restore.run(); }
            check(r.cachedWidth==1920 && r.cachedHeight==1080 && r.depthTexture==mainD && r.framebuffer==mainF);
        }
        Minecraft.getInstance().target.width=480;
        Runnable restore=DhScopeBuffers.swap(r);
        try {
            check(((DhFramebuffer)scopeD).destroyed && ((DhFramebuffer)scopeF).destroyed);
            check(!((DhFramebuffer)mainD).destroyed);
            throw new RuntimeException("simulated render failure");
        } catch(RuntimeException expected) {
        } finally { restore.run(); }
        check(r.depthTexture==mainD && r.framebuffer==mainF);
        DhScopeBuffers.releaseAll();
        DhScopeBuffers.releaseAll();
        check(!((DhFramebuffer)mainD).destroyed && !((DhFramebuffer)mainF).destroyed);
        System.out.println("PASS: 120-frame bank reuse, main restoration, resize cleanup, exception restoration, idempotent cleanup");
    }
    static void check(boolean b) { if(!b) throw new AssertionError(); }
}
