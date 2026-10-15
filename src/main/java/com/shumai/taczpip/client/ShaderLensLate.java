package com.shumai.taczpip.client;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/** Preserve the lens stencil across TaCZ's clear; composite after shader post-processing. */
public final class ShaderLensLate {
    private static TextureTarget mask;
    private static Runnable foreground;
    private static Matrix4f projection, modelView;
    private static boolean drawing;
    public static boolean isDrawing() { return drawing; }
    public static void reset() { foreground = null; }

    public static void capture(Runnable renderForeground) {
        if (drawing || !OculusCompat.shadersActive() || !ScopeLensComposite.hasComposite()) return;
        var mc = Minecraft.getInstance();
        var main = mc.getMainRenderTarget();
        int read = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int draw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        try {
            if (mask == null) { mask = new TextureTarget(main.width, main.height, true, Minecraft.ON_OSX); mask.enableStencil(); }
            else if (mask.width != main.width || mask.height != main.height) mask.resize(main.width, main.height, Minecraft.ON_OSX);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, draw);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, mask.frameBufferId);
            GL30.glBlitFramebuffer(0, 0, main.width, main.height, 0, 0, main.width, main.height,
                    GL11.GL_STENCIL_BUFFER_BIT, GL11.GL_NEAREST);
            projection = new Matrix4f(RenderSystem.getProjectionMatrix());
            modelView = new Matrix4f(RenderSystem.getModelViewStack().last().pose());
            foreground = renderForeground;
        } finally {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, read);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, draw);
        }
    }

    public static void draw() {
        if (foreground == null || drawing || ShaderScopeContext.active()) return;
        var task = foreground;
        foreground = null;
        var main = Minecraft.getInstance().getMainRenderTarget();
        int read = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int draw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        boolean stencil = GL11.glIsEnabled(GL11.GL_STENCIL_TEST);
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean depthWrite = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        int stencilMask = GL11.glGetInteger(GL11.GL_STENCIL_WRITEMASK);
        int stencilFunc = GL11.glGetInteger(GL11.GL_STENCIL_FUNC);
        int stencilRef = GL11.glGetInteger(GL11.GL_STENCIL_REF);
        int stencilValueMask = GL11.glGetInteger(GL11.GL_STENCIL_VALUE_MASK);
        var oldProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        var sorting = RenderSystem.getVertexSorting();
        var stack = RenderSystem.getModelViewStack();
        stack.pushPose();
        drawing = true;
        try {
            main.bindWrite(true);
            // bindWrite binds BOTH framebuffer targets; set the read source afterwards.
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, mask.frameBufferId);
            GL30.glBlitFramebuffer(0, 0, main.width, main.height, 0, 0, main.width, main.height,
                    GL11.GL_STENCIL_BUFFER_BIT, GL11.GL_NEAREST);
            GL11.glEnable(GL11.GL_STENCIL_TEST);
            RenderSystem.setProjectionMatrix(projection, sorting);
            stack.last().pose().set(modelView);
            RenderSystem.applyModelViewMatrix();
            ScopeLensComposite.draw(new int[0]); // Already promoted in the original pass.
            task.run();
        } finally {
            drawing = false;
            RenderSystem.stencilMask(0xff);
            RenderSystem.clearStencil(0);
            RenderSystem.clear(GL11.GL_STENCIL_BUFFER_BIT, Minecraft.ON_OSX);
            RenderSystem.stencilMask(stencilMask);
            RenderSystem.stencilFunc(stencilFunc, stencilRef, stencilValueMask);
            RenderSystem.depthMask(depthWrite);
            if (depth) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
            if (!stencil) GL11.glDisable(GL11.GL_STENCIL_TEST);
            RenderSystem.setProjectionMatrix(oldProjection, sorting);
            stack.popPose(); RenderSystem.applyModelViewMatrix();
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, read);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, draw);
        }
    }

    public static void release() { reset(); if (mask != null) { mask.destroyBuffers(); mask = null; } }
}
