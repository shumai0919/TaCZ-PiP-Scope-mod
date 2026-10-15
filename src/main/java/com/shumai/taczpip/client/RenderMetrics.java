package com.shumai.taczpip.client;

import com.mojang.logging.LogUtils;
import com.shumai.taczpip.PrototypeConfig;
import com.shumai.taczpip.TaczPipScope;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** CPU wall time, including driver submission; NOT GPU execution time or FPS. */
@Mod.EventBusSubscriber(modid = TaczPipScope.MOD_ID, value = Dist.CLIENT)
public final class RenderMetrics {
    public static final int WORLD=0, COMPOSITE=1, BODY=2, RETICLE=3;
    public static final int BODY_DEPTH=6, BODY_VERTICES=7, BODY_SUBMIT_TAIL=8, BODY_MODEL=9;
    private static final String[] NAMES={"world", "composite", "body", "reticle", "frame_pip", "frame_no_pip",
            "body_depth", "body_vertices", "body_submit_tail", "body_model"};
    private static final long[] count=new long[NAMES.length], sum=new long[NAMES.length], max=new long[NAMES.length];
    private static long windowStart, frameStart;
    private static boolean pip;
    private RenderMetrics() {}
    public static long begin() { return PrototypeConfig.METRICS.get() ? System.nanoTime() : 0; }
    public static void end(int phase, long start) {
        if (start == 0) return;
        add(phase, System.nanoTime()-start);
        if (phase == WORLD) pip=true;
    }
    private static void add(int phase, long elapsed) {
        count[phase]++; sum[phase]+=elapsed; max[phase]=Math.max(max[phase],elapsed);
    }
    private static void reset() {
        java.util.Arrays.fill(count,0); java.util.Arrays.fill(sum,0); java.util.Arrays.fill(max,0);
        windowStart=0; frameStart=0; pip=false;
        VertexReuseContext.resetCounts();
    }
    @SubscribeEvent
    public static void frame(TickEvent.RenderTickEvent event) {
        if (!PrototypeConfig.METRICS.get()) return;
        var mc=Minecraft.getInstance();
        if (!PrototypeConfig.METRICS.get() || mc.level == null || mc.screen != null) { reset(); return; }
        if (event.phase == TickEvent.Phase.START) {
            frameStart=System.nanoTime(); pip=false;
            if (windowStart == 0) windowStart=frameStart;
        } else if (frameStart != 0) {
            long now=System.nanoTime();
            add(pip ? 4 : 5,now-frameStart); frameStart=0;
            if (now-windowStart >= 10_000_000_000L) {
                LogUtils.getLogger().info("[PiP profile] shaders={} DH_loaded={} resolution_percent={} pip_max_fps={} entities={} block_entities={} particles={}",
                        OculusCompat.shadersActive(), DhScopeContext.loaded(), PrototypeConfig.PIP_RESOLUTION.get(),
                        PrototypeConfig.PIP_MAX_FPS.get(), PrototypeConfig.PIP_ENTITIES.get(),
                        PrototypeConfig.PIP_BLOCK_ENTITIES.get(), PrototypeConfig.PIP_PARTICLES.get());
                LogUtils.getLogger().info("[PiP CPU metrics] window={}s size={}x{} deferredReticle={}; mean/max milliseconds (NOT GPU time/FPS)",
                        (now-windowStart)/1_000_000_000.0, mc.getWindow().getWidth(),mc.getWindow().getHeight(),PrototypeConfig.DEFER_RETICLE.get());
                for(int i=0;i<NAMES.length;i++) if(count[i]>0)
                    LogUtils.getLogger().info("[PiP CPU metrics] {} calls={} mean_ms={} max_ms={}",
                            NAMES[i],count[i],sum[i]/(count[i]*1_000_000.0),max[i]/1_000_000.0);
                LogUtils.getLogger().info("[PiP vertex reuse] enabled={} captured_cubes={} reused_cubes={} fallback_cubes={}",
                        VertexReuseContext.ENABLED, VertexReuseContext.captures, VertexReuseContext.hits, VertexReuseContext.misses);
                if (VertexReuseContext.ENABLED) VertexReuseContext.report();
                reset();
            }
        }
    }
}
