package com.shumai.taczpip.client;

import com.shumai.taczpip.PrototypeConfig;
import com.shumai.taczpip.TaczPipScope;
import com.tacz.guns.client.model.BedrockGunModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import java.nio.file.Files;
import java.util.ArrayDeque;
import com.mojang.logging.LogUtils;
import com.tacz.guns.client.model.bedrock.BedrockModel;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** Experimental, render-thread-only cache scope. Never reuses across frames. */
@Mod.EventBusSubscriber(modid = TaczPipScope.MOD_ID, value = Dist.CLIENT)
public final class VertexReuseContext {
    public static final boolean ENABLED = Files.isRegularFile(FMLPaths.CONFIGDIR.get().resolve("taczpip-vertex-reuse.enabled"));
    public static final boolean PROFILING = PrototypeConfig.METRICS.get();
    private static final ArrayDeque<Entry> scopes = new ArrayDeque<>();
    private static final Entry NONE = new Entry(null, false);
    private static boolean inner;
    private static long frame;
    public static long captures, hits, misses;
    public static long modelCalls, eligibleCaptures, eligibleReplays, cubeCalls, cubeOutsideScope, cubeSubclass;
    private static final long[] rejected = new long[8];
    private static final String[] REASONS = {"not_gun", "not_first_person", "pip_world", "disabled", "no_lens_frame", "no_shader", "unsupported_scope", "aux_sight"};
    private static int diagnosticModels;
    private static final java.util.Set<String> described = new java.util.HashSet<>();
    record Entry(Object owner, boolean replay) {}
    private VertexReuseContext() {}
    public static void begin(Object model, ItemDisplayContext context) {
        if (!ENABLED) return;
        if (PROFILING) modelCalls++;
        int reason = !(model instanceof BedrockGunModel) ? 0 : !context.firstPerson() ? 1
                : ScopeFramebufferPrototype.isRenderingWorld() ? 2
                : !(PrototypeConfig.SHOW_GUN.get() && PrototypeConfig.LENS.get()) ? 3
                : !ScopeFramebufferPrototype.hasLensFrame() ? 4 : !OculusCompat.shadersActive() ? 5
                : !OpticsPolicy.isSupportedScopeActive() ? 6 : !OpticsPolicy.isOpticalViewSelected() ? 7 : -1;
        boolean eligible = reason < 0;
        if (PROFILING) {
            if (eligible) { if (inner) eligibleReplays++; else eligibleCaptures++; }
            else rejected[reason]++;
            if (inner) describe(model, context, reason);
        }
        scopes.push(eligible ? new Entry(model, inner) : NONE);
    }
    public static void end() { if (ENABLED && !scopes.isEmpty()) scopes.pop(); }
    static Entry current() { return scopes.peek(); }
    public static boolean eligible() { return current() != null && current().owner() != null; }
    static long frame() { return frame; }
    public static void inner(Runnable draw) {
        boolean previous = inner;
        try { inner = true; draw.run(); }
        finally { inner = previous; }
    }
    public static void resetCounts() {
        captures = hits = misses = modelCalls = eligibleCaptures = eligibleReplays = cubeCalls = cubeOutsideScope = cubeSubclass = 0;
        java.util.Arrays.fill(rejected, 0);
    }
    public static void report() {
        LogUtils.getLogger().info("[PiP vertex route] model_calls={} capture_passes={} replay_passes={} cube_hooks={} cube_outside_scope={} cube_subclass={}",
                modelCalls, eligibleCaptures, eligibleReplays, cubeCalls, cubeOutsideScope, cubeSubclass);
        for (int i=0;i<rejected.length;i++) if (rejected[i]>0)
            LogUtils.getLogger().info("[PiP vertex route] rejected={} calls={}", REASONS[i], rejected[i]);
    }
    private static void describe(Object model, ItemDisplayContext context, int reason) {
        String key = model.getClass().getName() + ":" + context + ":" + reason;
        if (diagnosticModels >= 8 || !described.add(key)) return;
        diagnosticModels++;
        Map<String,Integer> parts = new LinkedHashMap<>(), cubes = new LinkedHashMap<>();
        if (model instanceof BedrockModel bedrock) {
            var seen = new IdentityHashMap<BedrockPart,Boolean>();
            for (var part : bedrock.getShouldRender()) collect(part, seen, parts, cubes);
        }
        LogUtils.getLogger().info("[PiP vertex model] class={} context={} gate={} parts={} cubes={}",
                model.getClass().getName(), context, reason < 0 ? "eligible" : REASONS[reason], parts, cubes);
    }
    private static void collect(BedrockPart part, IdentityHashMap<BedrockPart,Boolean> seen,
                                Map<String,Integer> parts, Map<String,Integer> cubes) {
        if (part == null || seen.put(part,Boolean.TRUE) != null || seen.size()>10000) return;
        parts.merge(part.getClass().getName(),1,Integer::sum);
        for (var cube : part.cubes) cubes.merge(cube.getClass().getName(),1,Integer::sum);
        for (var child : part.children) collect(child,seen,parts,cubes);
    }
    @SubscribeEvent
    public static void frame(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            frame++;
            scopes.clear(); // Also bounds recovery after an exceptional render.
            inner = false;
        }
    }
}
