package com.shumai.taczpip.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.logging.LogUtils;
import com.shumai.taczpip.PrototypeConfig;
import com.shumai.taczpip.TaczPipScope;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.client.other.KeepingItemRenderer;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import net.minecraft.Util;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import java.util.Locale;

/** Experimental vanilla second pass. Lens compositing belongs to Phase 6. */
@Mod.EventBusSubscriber(modid = TaczPipScope.MOD_ID, value = Dist.CLIENT)
public final class ScopeFramebufferPrototype {
    private static TextureTarget target;
    private static PoseStack worldView;
    private static final PoseStack SAVED_WORLD_VIEW = new PoseStack();
    private static boolean rendering, ready, failed, warned;
    private static float magnification;
    private static long frameCount;
    private static final ZoomTransition ZOOM_TRANSITION = new ZoomTransition();
    private static String zoomContext;
    private static boolean cached;
    private static long lastWorldTime;
    private static String cacheSettings;
    private static Object cacheLevel;

    public static void invalidateFrame() { cached = false; ready = false; }
    public static int renderWidth() { return rendering && target != null ? target.width : 0; }
    public static int renderHeight() { return rendering && target != null ? target.height : 0; }

    private ScopeFramebufferPrototype() {}

    public static boolean hasLensFrame() { return lensTarget() != null; }
    public static float lensMagnification() { return magnification; }
    public static boolean isRenderingWorld() { return rendering; }

    static boolean compatibleRenderer() {
        return Minecraft.getInstance().options.graphicsMode().get() != GraphicsStatus.FABULOUS
                && OculusCompat.allowsPip() && DhScopeContext.supported()
                && (!ModList.get().isLoaded("embeddium") || EmbeddiumCompat.supportedVersion())
                // Embeddium declares a Rubidium compatibility stub in its own JAR.
                && (!ModList.get().isLoaded("rubidium") || EmbeddiumCompat.supportedVersion())
                && !ModList.get().isLoaded("acceleratedrendering");
    }

    // A missing ADS frame must not briefly reinstate TaCZ's shrinking circle mask.
    static boolean canCompositeOrCover() {
        return !failed && !rendering && PrototypeConfig.ENABLED.get()
                && PrototypeConfig.FRAMEBUFFER.get() && compatibleRenderer();
    }

    static RenderTarget lensTarget() {
        return ready && !rendering && PrototypeConfig.FRAMEBUFFER.get()
                && PrototypeConfig.ENABLED.get() ? target : null;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void render(RenderLevelStageEvent event) {
        if (rendering) return;
        if (OculusCompat.shadersActive()) return; // Prepared before Oculus draws the hands.
        // AFTER_LEVEL's pose stack is NOT the world view in Forge 1.20.1.
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            worldView = SAVED_WORLD_VIEW;
            worldView.last().pose().set(event.getPoseStack().last().pose());
            worldView.last().normal().set(event.getPoseStack().last().normal());
            return;
        }
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        PoseStack view = worldView;
        worldView = null;
        renderWorld(view, event.getPartialTick(), Util.getNanos(), event.getCamera(), event.getProjectionMatrix());
    }

    public static void beforeShaderWorld(PoseStack pose, float partialTick, long frameStartTime,
                                         net.minecraft.client.Camera camera, Matrix4f projection) {
        if (rendering || !OculusCompat.shadersActive()) return;
        PoseStack copy = new PoseStack();
        copy.last().pose().set(pose.last().pose());
        copy.last().normal().set(pose.last().normal());
        renderWorld(copy, partialTick, frameStartTime, camera, projection);
    }

    private static void renderWorld(PoseStack view, float partialTick, long frameStartTime,
                                    net.minecraft.client.Camera camera, Matrix4f worldProjection) {
        ready = false;
        ScopeLensComposite.clearReticle();
        Minecraft mc = Minecraft.getInstance();
        boolean force = Boolean.getBoolean("taczpip.shaderWorldChecks")
                && !Boolean.getBoolean("taczpip.shaderAdsChecks");
        if (failed || !PrototypeConfig.FRAMEBUFFER.get() || !PrototypeConfig.ENABLED.get()
                || mc.player == null || mc.level == null || mc.screen != null
                || !mc.options.getCameraType().isFirstPerson()
                || (!force && (!OpticsPolicy.isSupportedScopeActive() || !OpticsPolicy.isOpticalViewSelected())) || view == null) {
            resetZoom();
            return;
        }
        if (!compatibleRenderer()) {
            if (!warned) {
                LogUtils.getLogger().warn("[TaCZ PiP Scope] Unsupported renderer combination. Requires Fast/Fancy, Embeddium 0.3.31, Oculus 1.8.0, DH 2.2.1-a when installed; AcceleratedRendering remains unsupported.");
                warned = true;
            }
            resetZoom();
            return;
        }
        float progress = force ? 1 : IClientPlayerGunOperator.fromLocalPlayer(mc.player)
                .getClientAimingProgress(partialTick);
        if (!Float.isFinite(progress) || progress < 0.01f) {
            resetZoom();
            return;
        }
        var stack = KeepingItemRenderer.getRenderer().getCurrentItem();
        IGun gun = IGun.getIGunOrNull(stack);
        if (gun == null && !force) { resetZoom(); return; }
        float zoom = force ? 4 : gun.getAimingZoom(stack);
        if (!Float.isFinite(zoom) || zoom < 1 || zoom > 100) { resetZoom(); return; }
        // Network NBT updates can replace ItemStack during a zoom change.
        // Identify the weapon/scope without its mutable zoom/ammo NBT.
        String context = force ? "shader-world-check" : mc.player.getInventory().selected + ":" + gun.getGunId(stack)
                + ":" + gun.getAttachmentId(stack, AttachmentType.SCOPE)
                + ":" + gun.getBuiltInAttachmentId(stack, AttachmentType.SCOPE);
        if (!context.equals(zoomContext)) {
            ZOOM_TRANSITION.reset();
            cached = false;
            zoomContext = context;
        }
        double smoothZoom = ZOOM_TRANSITION.update(zoom, System.nanoTime(), PrototypeConfig.ZOOM_TRANSITION_SECONDS.get());

        RenderTarget main = mc.getMainRenderTarget();
        int width = PipPerformanceRules.dimension(main.width, PrototypeConfig.PIP_RESOLUTION.get());
        int height = PipPerformanceRules.dimension(main.height, PrototypeConfig.PIP_RESOLUTION.get());
        boolean shaderPass = OculusCompat.shadersActive();
        String settings = main.width + ":" + main.height + ":" + width + ":" + height + ":" + shaderPass
                + ":" + PrototypeConfig.PIP_MAX_FPS.get() + ":" + PrototypeConfig.PIP_ENTITIES.get()
                + ":" + PrototypeConfig.PIP_BLOCK_ENTITIES.get() + ":" + PrototypeConfig.PIP_PARTICLES.get();
        if (!settings.equals(cacheSettings) || cacheLevel != mc.level) cached = false;
        cacheSettings = settings;
        cacheLevel = mc.level;
        long now = System.nanoTime();
        if (!PipPerformanceRules.due(cached, now, lastWorldTime, PrototypeConfig.PIP_MAX_FPS.get())) {
            ready = true; // Only reuse the world image, never the current-frame lens mask/foreground.
            return;
        }
        cached = false;
        magnification = (float) (1 + (smoothZoom - 1) * Math.min(1, progress));
        Matrix4f projection = new Matrix4f(RenderSystem.getProjectionMatrix());
        var sorting = RenderSystem.getVertexSorting();
        Matrix3f inverseView = new Matrix3f(RenderSystem.getInverseViewRotationMatrix());
        PostChain outlines = mc.levelRenderer.entityEffect;
        // Preserve the main view's wider frustum/chunk visibility list: only the
        // projection narrows. Extra culling work is preferable to missing chunks.
        Matrix4f scopeProjection = new Matrix4f().scaling(magnification, magnification, 1)
                .mul(worldProjection);
        rendering = true;
        long metricStart = RenderMetrics.begin();
        GpuRenderMetrics.beginWorld();
        RenderSystem.getModelViewStack().pushPose();
        mc.getProfiler().push("tacz_pip_world");
        try {
            // Oculus uses the swapped main target for its own color/depth sizes.
            // Fabulous auxiliary targets remain unsupported (see compatibleRenderer).
            if (target == null) {
                target = new TextureTarget(width, height, true, Minecraft.ON_OSX);
                LogUtils.getLogger().info("[TaCZ PiP Scope] Phase 5 framebuffer created: {}x{}", main.width, main.height);
            } else if (target.width != width || target.height != height) {
                // Oculus' normal resize path does not rebuild every DH attachment
                // and shader history target. Destroy the scope pipeline BEFORE its
                // textures are replaced, preventing stale/recycled GL texture IDs.
                // This is a one-time cost on resolution changes, never per frame.
                if (shaderPass) ShaderScopeContext.release();
                target.resize(width, height, Minecraft.ON_OSX);
            }
            target.setFilterMode(org.lwjgl.opengl.GL11.GL_LINEAR);
            mc.mainRenderTarget = target;
            if (shaderPass) target.enableStencil();
            mc.levelRenderer.entityEffect = null; // Do not overwrite main-view glow texture.
            target.bindWrite(true);
            RenderSystem.setProjectionMatrix(scopeProjection, sorting);
            // Do not call GameRenderer.renderLevel: that would repeat camera/recoil,
            // picking, FOV events, and hand rendering. Reuse the already-updated camera.
            try (var dhContext = DhScopeContext.enter();
                 var shaderContext = shaderPass ? ShaderScopeContext.enter() : null) {
                // Oculus derives frameTimeCounter from this timestamp. Reuse the main
                // frame's GLFW-based clock; System.nanoTime() has a different origin.
                mc.levelRenderer.renderLevel(view, partialTick, frameStartTime, false,
                        camera, mc.gameRenderer, mc.gameRenderer.lightTexture(), scopeProjection);
            }
            ready = true;
            cached = true;
            lastWorldTime = now;
            if (++frameCount == 1) LogUtils.getLogger().info("[TaCZ PiP Scope] Phase 5 first world frame rendered ({}x)", zoom);
        } catch (RuntimeException exception) {
            failed = true;
            LogUtils.getLogger().error("[TaCZ PiP Scope] Phase 5 disabled until reconnect after render failure", exception);
        } finally {
            mc.mainRenderTarget = main;
            mc.levelRenderer.entityEffect = outlines;
            main.bindWrite(true);
            RenderSystem.setProjectionMatrix(projection, sorting);
            RenderSystem.setInverseViewRotationMatrix(inverseView);
            RenderSystem.getModelViewStack().popPose();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
            mc.getProfiler().pop();
            rendering = false;
            RenderMetrics.end(RenderMetrics.WORLD, metricStart);
            GpuRenderMetrics.endWorld();
        }
    }

    @SubscribeEvent
    public static void preview(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!PrototypeConfig.PREVIEW.get() || !ready || target == null || ScopeImageShader.get() == null || mc.screen != null || mc.options.hideGui) return;
        var gui = event.getGuiGraphics();
        int width = Math.min(240, event.getWindow().getGuiScaledWidth() / 3);
        int height = Math.max(1, width * target.height / target.width);
        int x = event.getWindow().getGuiScaledWidth() - width - 8, y = 20;
        gui.fill(x - 2, y - 2, x + width + 2, y + height + 2, 0xffeeeeee);
        gui.flush();
        RenderSystem.disableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.setShader(ScopeImageShader::get);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.setShaderTexture(0, target.getColorTextureId());
        Matrix4f matrix = gui.pose().last().pose();
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.vertex(matrix, x, y + height, 0).uv(0, 0).endVertex();
        buffer.vertex(matrix, x + width, y + height, 0).uv(1, 0).endVertex();
        buffer.vertex(matrix, x + width, y, 0).uv(1, 1).endVertex();
        buffer.vertex(matrix, x, y, 0).uv(0, 1).endVertex();
        BufferUploader.drawWithShader(buffer.end());
        RenderSystem.enableDepthTest();
        gui.drawString(mc.font, String.format(Locale.ROOT, "PiP Phase 5 | %.2fx", magnification), x, 8, 0xffffff);
    }

    @SubscribeEvent
    public static void disconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        ShaderScopeContext.release();
        DhScopeBuffers.releaseAll();
        ScopeGunResolution.release();
        ScopeLensComposite.clearReticle();
        resetZoom();
        ReticleRenderType.clearCache();
        ready = false;
        failed = false;
        warned = false;
        worldView = null;
        cacheLevel = null;
        cacheSettings = null;
        frameCount = 0;
        if (target != null) {
            target.destroyBuffers();
            target = null;
        }
    }

    private static void resetZoom() {
        invalidateFrame();
        ZOOM_TRANSITION.reset();
        zoomContext = null;
    }
}
