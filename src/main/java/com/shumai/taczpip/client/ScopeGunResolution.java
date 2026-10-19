package com.shumai.taczpip.client;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.shumai.taczpip.PrototypeConfig;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.lwjgl.opengl.*;

/** Independent transparent gun layer: same pixel grid as PiP, but refreshed every frame. */
public final class ScopeGunResolution {
    private static TextureTarget gunTarget;
    private ScopeGunResolution() {}

    public static void render(Runnable gun) {
        var lens = ScopeFramebufferPrototype.lensTarget();
        var mc = Minecraft.getInstance();
        var main = mc.getMainRenderTarget();
        var shader = ScopeImageShader.gunComposite();
        float blur=PrototypeConfig.LENS_EFFECTS.get()&&PrototypeConfig.GUN_FOCUS_BLUR.get()
                ? PrototypeConfig.GUN_BLUR_STRENGTH.get().floatValue() : 0f;
        if (lens == null || shader == null || (blur<=0 && lens.width == main.width && lens.height == main.height)) {
            long depthStart = RenderMetrics.begin();
            try { LensDepthReset.clear(); }
            finally { RenderMetrics.end(RenderMetrics.BODY_DEPTH, depthStart); }
            gun.run();
            return;
        }
        int read = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int draw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int[] viewport = new int[4]; GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        boolean stencil = GL11.glIsEnabled(GL11.GL_STENCIL_TEST);
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST), blend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE), write = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        int sr = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB), dr = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        int sa = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA), da = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        int er = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_RGB), ea = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_ALPHA);
        int stencilMask = GL11.glGetInteger(GL11.GL_STENCIL_WRITEMASK);
        int stencilFunc = GL11.glGetInteger(GL11.GL_STENCIL_FUNC);
        int stencilRef = GL11.glGetInteger(GL11.GL_STENCIL_REF);
        int stencilValueMask = GL11.glGetInteger(GL11.GL_STENCIL_VALUE_MASK);
        var oldShader = RenderSystem.getShader();
        int texture = RenderSystem.getShaderTexture(0);
        var projection = new Matrix4f(RenderSystem.getProjectionMatrix());
        var sorting = RenderSystem.getVertexSorting();
        var modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        try {
            if (gunTarget == null) gunTarget = new TextureTarget(lens.width, lens.height, true, Minecraft.ON_OSX);
            else if (gunTarget.width != lens.width || gunTarget.height != lens.height)
                gunTarget.resize(lens.width, lens.height, Minecraft.ON_OSX);
            gunTarget.setFilterMode(GL11.GL_LINEAR);
            mc.mainRenderTarget = gunTarget;
            gunTarget.setClearColor(0, 0, 0, 0);
            RenderSystem.depthMask(true);
            gunTarget.clear(Minecraft.ON_OSX);
            gunTarget.bindWrite(true);
            GL11.glDisable(GL11.GL_STENCIL_TEST);
            RenderSystem.enableDepthTest();
            gun.run();

            mc.mainRenderTarget = main;
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, read);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, draw);
            RenderSystem.viewport(viewport[0], viewport[1], viewport[2], viewport[3]);
            // Use the ORIGINAL full-resolution ocular stencil. No scaled stencil/depth copy.
            if (stencil) GL11.glEnable(GL11.GL_STENCIL_TEST);
            RenderSystem.stencilFunc(stencilFunc, stencilRef, stencilValueMask);
            RenderSystem.stencilMask(0);
            RenderSystem.disableDepthTest(); RenderSystem.depthMask(false); RenderSystem.disableCull();
            modelView.setIdentity(); RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(new Matrix4f(), sorting);
            RenderSystem.setShader(() -> shader);
            RenderSystem.setShaderTexture(0, gunTarget.getColorTextureId());
            float radius=blur*12f*main.height/1080f;
            shader.safeGetUniform("GunBlurStep").set(radius/main.width,radius/main.height);
            RenderSystem.enableBlend();
            var buffer = Tesselator.getInstance().getBuilder();
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            buffer.vertex(-1,-1,0).uv(0,0).endVertex();
            buffer.vertex(1,-1,0).uv(1,0).endVertex();
            buffer.vertex(1,1,0).uv(1,1).endVertex();
            buffer.vertex(-1,1,0).uv(0,1).endVertex();
            BufferUploader.drawWithShader(buffer.end());
        } finally {
            mc.mainRenderTarget = main;
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, read);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, draw);
            RenderSystem.viewport(viewport[0], viewport[1], viewport[2], viewport[3]);
            if (stencil) GL11.glEnable(GL11.GL_STENCIL_TEST); else GL11.glDisable(GL11.GL_STENCIL_TEST);
            RenderSystem.stencilMask(stencilMask);
            RenderSystem.stencilFunc(stencilFunc, stencilRef, stencilValueMask);
            RenderSystem.blendFuncSeparate(sr,dr,sa,da); GL20.glBlendEquationSeparate(er,ea);
            if (blend) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
            if (cull) RenderSystem.enableCull(); else RenderSystem.disableCull();
            RenderSystem.depthMask(write);
            if (depth) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
            RenderSystem.setShaderTexture(0,texture); RenderSystem.setShader(() -> oldShader);
            RenderSystem.setProjectionMatrix(projection,sorting);
            modelView.popPose(); RenderSystem.applyModelViewMatrix();
        }
    }
    public static void release() {
        if (gunTarget != null) { gunTarget.destroyBuffers(); gunTarget = null; }
    }
}
