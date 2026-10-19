package com.shumai.taczpip.client;

import com.mojang.logging.LogUtils;
import com.shumai.taczpip.PrototypeConfig;
import com.shumai.taczpip.TaczPipScope;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL33;

/** Timestamp pairs are nested safely and read only after the final result is available. */
@Mod.EventBusSubscriber(modid=TaczPipScope.MOD_ID, value=Dist.CLIENT)
public final class GpuRenderMetrics {
    private static final int SLOTS=32;
    private static final String[] NAMES={"frame_no_pip", "frame_pip", "pip_world"};
    private static final int[][] queries=new int[SLOTS][4];
    private static final boolean[] pending=new boolean[SLOTS], hasWorld=new boolean[SLOTS];
    private static final long[] count=new long[3], sum=new long[3], max=new long[3];
    private static boolean initialized, unsupported, smokeDone;
    private static int cursor, active=-1;
    private static long window, dropped;
    private GpuRenderMetrics() {}

    private static boolean initialize() {
        if (unsupported) return false;
        if (initialized) return true;
        if (!(GL.getCapabilities().OpenGL33 || GL.getCapabilities().GL_ARB_timer_query)
                || GL15.glGetQueryi(GL33.GL_TIMESTAMP, GL15.GL_QUERY_COUNTER_BITS) < 64) {
            unsupported=true;
            LogUtils.getLogger().warn("[PiP GPU metrics] 64-bit timer queries unavailable; measurement disabled");
            return false;
        }
        for(int[] slot: queries) for(int i=0;i<4;i++) slot[i]=GL15.glGenQueries();
        initialized=true;
        return true;
    }
    public static void beginWorld() {
        if(active<0 || hasWorld[active]) return;
        GL33.glQueryCounter(queries[active][1],GL33.GL_TIMESTAMP);
        hasWorld[active]=true;
    }
    public static void endWorld() {
        if(active>=0 && hasWorld[active]) GL33.glQueryCounter(queries[active][2],GL33.GL_TIMESTAMP);
    }
    private static void add(int index,long elapsed) {
        if(elapsed<0) return;
        count[index]++; sum[index]+=elapsed; max[index]=Math.max(max[index],elapsed);
    }
    private static void poll(boolean smoke) {
        for(int s=0;s<SLOTS;s++) {
            if(!pending[s] || GL15.glGetQueryObjecti(queries[s][3],GL15.GL_QUERY_RESULT_AVAILABLE)==0) continue;
            long start=GL33.glGetQueryObjectui64(queries[s][0],GL15.GL_QUERY_RESULT);
            long end=GL33.glGetQueryObjectui64(queries[s][3],GL15.GL_QUERY_RESULT);
            add(hasWorld[s]?1:0,end-start);
            if(hasWorld[s]) {
                long beginWorld=GL33.glGetQueryObjectui64(queries[s][1],GL15.GL_QUERY_RESULT);
                long endWorld=GL33.glGetQueryObjectui64(queries[s][2],GL15.GL_QUERY_RESULT);
                add(2,endWorld-beginWorld);
            }
            pending[s]=false;
            if(smoke && !smokeDone) {
                if(end<start) throw new IllegalStateException("Invalid GPU timestamp order");
                smokeDone=true;
                LogUtils.getLogger().info("[PiP GPU metrics] Asynchronous timestamp smoke check PASSED: {} ms (menu, not a world benchmark)",(end-start)/1_000_000.0);
            }
        }
    }
    private static void clearStats() {
        java.util.Arrays.fill(count,0); java.util.Arrays.fill(sum,0); java.util.Arrays.fill(max,0);
        window=0; dropped=0;
    }
    private static void release() {
        if(initialized) for(int[] slot:queries) for(int id:slot) GL15.glDeleteQueries(id);
        initialized=false; active=-1;
        java.util.Arrays.fill(pending,false); java.util.Arrays.fill(hasWorld,false);
        clearStats();
    }
    @SubscribeEvent
    public static void frame(TickEvent.RenderTickEvent event) {
        var mc=Minecraft.getInstance();
        boolean smoke=!net.minecraftforge.fml.loading.FMLEnvironment.production && Boolean.getBoolean("taczpip.renderChecks") && !smokeDone && mc.level==null;
        boolean enabled=PrototypeConfig.GPU_METRICS.get() && mc.level!=null && mc.screen==null;
        if(event.phase==TickEvent.Phase.START) {
            if(!enabled && !smoke) { if(initialized) release(); return; }
            if(!initialize()) return;
            poll(smoke);
            if(smoke && smokeDone) { release(); return; }
            long now=System.nanoTime();
            if(window==0) window=now;
            if(!smoke && now-window>=10_000_000_000L) {
                LogUtils.getLogger().info("[PiP GPU metrics] size={}x{} dropped={} deferredReticle={}; GPU timestamp elapsed ms, NOT utilization/FPS",
                        mc.getWindow().getWidth(),mc.getWindow().getHeight(),dropped,PrototypeConfig.DEFER_RETICLE.get());
                for(int i=0;i<3;i++) if(count[i]>0) LogUtils.getLogger().info("[PiP GPU metrics] {} samples={} mean_ms={} max_ms={}",NAMES[i],count[i],sum[i]/(count[i]*1_000_000.0),max[i]/1_000_000.0);
                clearStats(); window=now;
            }
            active=-1;
            // Never overwrite a query still in flight and never wait for it.
            for(int n=0;n<SLOTS;n++) {
                int slot=(cursor+n)%SLOTS;
                if(!pending[slot]) { active=slot; cursor=(slot+1)%SLOTS; break; }
            }
            if(active<0) { dropped++; return; }
            hasWorld[active]=false;
            GL33.glQueryCounter(queries[active][0],GL33.GL_TIMESTAMP);
        } else if(active>=0) {
            GL33.glQueryCounter(queries[active][3],GL33.GL_TIMESTAMP);
            pending[active]=true; active=-1;
        }
    }
}
