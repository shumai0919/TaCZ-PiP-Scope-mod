package com.shumai.taczpip.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.shumai.taczpip.PrototypeConfig;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;

/** Feather the existing pupil inward AFTER world, gun and reticle; never expand the stencil. */
public final class ScopeMaskFeather {
    private ScopeMaskFeather() {}
    public static void draw(int[] indices, EyeBoxState[] eyes, LensBounds[] bounds) {
        var shader = ScopeImageShader.maskFeather();
        double strength = PrototypeConfig.MASK_EDGE_SOFTNESS.get();
        if (shader == null || strength <= 0 || indices.length == 0) return;
        var window = Minecraft.getInstance().getWindow();
        int width = window.getWidth(), height = window.getHeight();
        var oldShader = RenderSystem.getShader();
        var projection = new Matrix4f(RenderSystem.getProjectionMatrix());
        var sorting = RenderSystem.getVertexSorting();
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST), blend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE), writeDepth = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        int srcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB), dstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        int srcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA), dstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        int eqRgb = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_RGB), eqAlpha = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_ALPHA);
        int func = GL11.glGetInteger(GL11.GL_STENCIL_FUNC), ref = GL11.glGetInteger(GL11.GL_STENCIL_REF);
        int mask = GL11.glGetInteger(GL11.GL_STENCIL_VALUE_MASK), writeMask = GL11.glGetInteger(GL11.GL_STENCIL_WRITEMASK);
        var modelView = RenderSystem.getModelViewStack();
        long metric = RenderMetrics.begin();
        modelView.pushPose();
        try {
            modelView.setIdentity(); RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(new Matrix4f(), sorting);
            RenderSystem.disableDepthTest(); RenderSystem.depthMask(false); RenderSystem.disableCull();
            RenderSystem.enableBlend();
            RenderSystem.stencilMask(0);
            RenderSystem.setShader(() -> shader);
            shader.safeGetUniform("ViewportSize").set((float)width, (float)height);
            for (int i=0; i<indices.length && i<eyes.length && i<bounds.length; i++) {
                var b = bounds[i];
                if (b == null || !b.valid()) continue;
                var eye=eyes[i];
                float feather = MaskFeatherRules.width(eye.radius(), Math.min(b.rx()*width, b.ry()*height), eye.visibility(), strength);
                float[] aperture=PupilShape.aperturePlanes(b,width,height);
                if (feather < .001f && aperture.length==0) continue;
                shader.safeGetUniform("ApertureEdgeCount").set(aperture.length/4f);
                for(int e=0;e<aperture.length/4;e++) shader.safeGetUniform("ApertureEdges["+e+"]")
                        .set(aperture[e*4],aperture[e*4+1],aperture[e*4+2],0f);
                shader.safeGetUniform("PupilRadius").set(eye.radius());
                PupilUniforms.set(shader,ScopeLensComposite.shadowPlanes(i,width,height));
                shader.safeGetUniform("FeatherWidth").set(feather);
                RenderSystem.stencilFunc(GL11.GL_EQUAL, 255-indices[i], 0xff);
                var buffer = Tesselator.getInstance().getBuilder();
                buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
                buffer.vertex(-1,-1,0).uv(0,0).endVertex();
                buffer.vertex(1,-1,0).uv(1,0).endVertex();
                buffer.vertex(1,1,0).uv(1,1).endVertex();
                buffer.vertex(-1,1,0).uv(0,1).endVertex();
                BufferUploader.drawWithShader(buffer.end());
            }
        } finally {
            RenderSystem.stencilMask(writeMask); RenderSystem.stencilFunc(func,ref,mask);
            RenderSystem.blendFuncSeparate(srcRgb,dstRgb,srcAlpha,dstAlpha);
            GL20.glBlendEquationSeparate(eqRgb,eqAlpha);
            if (blend) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
            if (cull) RenderSystem.enableCull(); else RenderSystem.disableCull();
            RenderSystem.depthMask(writeDepth);
            if (depth) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
            RenderSystem.setShader(() -> oldShader);
            RenderSystem.setProjectionMatrix(projection,sorting);
            modelView.popPose(); RenderSystem.applyModelViewMatrix();
            RenderMetrics.end(RenderMetrics.COMPOSITE,metric);
        }
    }
}
