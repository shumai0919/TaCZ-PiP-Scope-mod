package com.shumai.taczpip.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.MixinEnvironment;

/** Opt-in dev-client checks (-PpipRenderChecks), never enabled in Prism by default. */
public final class RenderChecks {
    private RenderChecks() {}
    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("PiP render check failed: " + message);
    }
    public static void run() {
        RenderSystem.assertOnRenderThread();
        MixinEnvironment.getCurrentEnvironment().audit();
        checkEmbeddiumTerrainReuse();
        checkReticleBackground();
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean write = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        int func = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        var shader = RenderSystem.getShader();
        int texture = RenderSystem.getShaderTexture(0);
        var source = RenderType.entityCutout(new ResourceLocation("minecraft", "textures/block/stone.png"));
        try {
            RenderSystem.disableDepthTest();
            source.setupRenderState();
            require(GL11.glIsEnabled(GL11.GL_DEPTH_TEST), "expected original entityCutout to re-enable depth");
            source.clearRenderState();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            var overlay = ReticleRenderType.overlay(source);
            overlay.setupRenderState();
            require(!GL11.glIsEnabled(GL11.GL_DEPTH_TEST), "reticle must ignore gun depth at draw time");
            require(!GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK), "reticle must not write depth");
            overlay.clearRenderState();
            require(GL11.glIsEnabled(GL11.GL_DEPTH_TEST), "previous depth enable must be restored");
            require(GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK), "previous depth write must be restored");
            require(GL11.glGetInteger(GL11.GL_DEPTH_FUNC) == GL11.GL_LEQUAL, "previous depth function must be restored");
            LogUtils.getLogger().info("[TaCZ PiP Scope] Render checks PASSED: Mixin audit, original depth bug reproduced, reticle draw state and restoration verified");
        } finally {
            RenderSystem.depthFunc(func);
            RenderSystem.depthMask(write);
            if (depth) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
            RenderSystem.setShaderTexture(0, texture);
            RenderSystem.setShader(() -> shader);
        }
    }

    private static void checkEmbeddiumTerrainReuse() {
        if (!net.minecraftforge.fml.ModList.get().isLoaded("embeddium")) return;
        if (OculusCompat.shadersActive()) return; // Early shader scopes intentionally update terrain.
        require(EmbeddiumCompat.supportedVersion(), "unsupported Embeddium test version");
        require(!EmbeddiumCompat.reuseMainTerrain(), "normal terrain update must not be skipped");
        try {
            Class<?> renderer = Class.forName("me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer");
            Object instance = renderer.getMethod("instance").invoke(null);
            Class<?> viewport = Class.forName("me.jellysquid.mods.sodium.client.render.viewport.Viewport");
            var setup = renderer.getMethod("setupTerrain", net.minecraft.client.Camera.class,
                    viewport, int.class, boolean.class, boolean.class);
            var rendering = ScopeFramebufferPrototype.class.getDeclaredField("rendering");
            rendering.setAccessible(true);
            boolean previous = rendering.getBoolean(null);
            try {
                rendering.setBoolean(null, true);
                require(EmbeddiumCompat.reuseMainTerrain(), "PiP must reuse terrain");
                // No world/camera exists in this menu-only check. Returning safely
                // proves the injected cancellation runs before Embeddium setup.
                setup.invoke(instance, null, null, 0, false, false);
            } finally {
                rendering.setBoolean(null, previous);
            }
            require(!EmbeddiumCompat.reuseMainTerrain(), "PiP test state must be restored");
            if (net.minecraft.client.Minecraft.getInstance().options.graphicsMode().get()
                    != net.minecraft.client.GraphicsStatus.FABULOUS) {
                require(ScopeFramebufferPrototype.compatibleRenderer(), "Embeddium Rubidium stub must be allowed");
            }
            LogUtils.getLogger().info("[TaCZ PiP Scope] Embeddium terrain reuse checks PASSED (menu-only injection check, not world visual validation)");
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Embeddium terrain reuse check failed", error);
        }
    }

    private static void checkReticleBackground() {
        var root = new com.tacz.guns.client.model.bedrock.BedrockPart("division_2");
        var bg = new com.tacz.guns.client.model.bedrock.BedrockPart("bg");
        var reticle = new com.tacz.guns.client.model.bedrock.BedrockPart("reticle");
        root.children.add(bg);
        root.children.add(reticle);
        ReticleBackgroundFilter.render(root, () -> {
            require(!bg.visible, "background must be hidden during optical reticle pass");
            require(reticle.visible, "ordinary black reticle must remain visible");
        });
        require(bg.visible, "background visibility must be restored");
        bg.visible = false;
        ReticleBackgroundFilter.render(root, () -> {});
        require(!bg.visible, "previously hidden background must remain hidden");
        bg.visible = true;
        RuntimeException expected = new RuntimeException("test");
        try { ReticleBackgroundFilter.render(root, () -> { throw expected; }); }
        catch (RuntimeException error) { if (error != expected) throw error; }
        require(bg.visible, "background must be restored even on failure");
        LogUtils.getLogger().info("[TaCZ PiP Scope] Reticle background checks PASSED");
    }
}
