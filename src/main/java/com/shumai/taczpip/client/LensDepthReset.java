package com.shumai.taczpip.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;

/** Discard unzoomed attachment depth only where the current stencil admits the lens. */
public final class LensDepthReset {
    private LensDepthReset() {}

    public static void clear() {
        var projection = new Matrix4f(RenderSystem.getProjectionMatrix());
        var sorting = RenderSystem.getVertexSorting();
        var shader = RenderSystem.getShader();
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean write = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        int func = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        int stencilWrite = GL11.glGetInteger(GL11.GL_STENCIL_WRITEMASK);
        var modelView = RenderSystem.getModelViewStack();
        try (var memory = MemoryStack.stackPush()) {
            var colors = memory.malloc(4);
            GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK, colors);
            modelView.pushPose();
            try {
                modelView.setIdentity();
                RenderSystem.applyModelViewMatrix();
                RenderSystem.setProjectionMatrix(new Matrix4f(), sorting);
                RenderSystem.setShader(GameRenderer::getPositionShader);
                RenderSystem.colorMask(false, false, false, false);
                RenderSystem.stencilMask(0);
                RenderSystem.enableDepthTest();
                RenderSystem.depthFunc(GL11.GL_ALWAYS);
                RenderSystem.depthMask(true);
                RenderSystem.disableCull();
                // Unlike glClear, a draw honors the existing lens stencil test.
                var buffer = Tesselator.getInstance().getBuilder();
                buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
                buffer.vertex(-1, -1, 1).endVertex();
                buffer.vertex(1, -1, 1).endVertex();
                buffer.vertex(1, 1, 1).endVertex();
                buffer.vertex(-1, 1, 1).endVertex();
                BufferUploader.drawWithShader(buffer.end());
            } finally {
                RenderSystem.colorMask(colors.get(0) != 0, colors.get(1) != 0,
                        colors.get(2) != 0, colors.get(3) != 0);
                RenderSystem.stencilMask(stencilWrite);
                RenderSystem.depthFunc(func);
                RenderSystem.depthMask(write);
                if (depth) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
                if (cull) RenderSystem.enableCull(); else RenderSystem.disableCull();
                RenderSystem.setShader(() -> shader);
                RenderSystem.setProjectionMatrix(projection, sorting);
                modelView.popPose();
                RenderSystem.applyModelViewMatrix();
            }
        }
    }
}
