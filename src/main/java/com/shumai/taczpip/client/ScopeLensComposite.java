package com.shumai.taczpip.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.shumai.taczpip.PrototypeConfig;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

/** Screen-space image clipped by TaCZ's existing inner-lens stencil. */
public final class ScopeLensComposite {
    private static Runnable reticle;
    private static int[] lastOpticalIndices = new int[0];
    private static EyeBoxState[] eyes = new EyeBoxState[0];
    private static LensBounds[] lensBounds = new LensBounds[0];
    private static float aimingProgress;
    private static final net.minecraft.resources.ResourceLocation[] SURFACE_TEXTURES = {
            new net.minecraft.resources.ResourceLocation("tacz_pip_scope","textures/effect/lens_surface.png"),
            new net.minecraft.resources.ResourceLocation("tacz_pip_scope","textures/effect/lens_surface_fine.png"),
            new net.minecraft.resources.ResourceLocation("tacz_pip_scope","textures/effect/lens_surface_wiped.png"),
            new net.minecraft.resources.ResourceLocation("tacz_pip_scope","textures/effect/lens_surface_smudged.png")
    };
    public static void setAimingProgress(float progress) { aimingProgress=Float.isFinite(progress)?Math.max(0,Math.min(1,progress)):1; }
    static float[] shadowPlanes(int i,int width,int height) {
        return i<lensBounds.length && i<eyes.length
                ? PupilShape.eyePlanes(lensBounds[i],eyes[i].visibility(),width,height,eyes[i].shiftX(),eyes[i].shiftY()) : new float[0];
    }
    public static void setLensBounds(LensBounds[] bounds) { lensBounds = bounds; }
    public static void setEyeBox(EyeBoxState[] states) { eyes=states; }
    public static void softenMaskEdge() {
        if (!available() || !hasComposite() || !OpticsPolicy.isOpticalViewSelected()
                || !PrototypeConfig.LENS_EFFECTS.get() || !PrototypeConfig.EYEBOX_EFFECT.get()
                || PrototypeConfig.DEBUG_LENS_MASK.get()) return;
        ScopeMaskFeather.draw(lastOpticalIndices, eyes, lensBounds);
    }
    private ScopeLensComposite() {}

    public static void clearReticle() {
        reticle = null; lastOpticalIndices = new int[0]; lensBounds = new LensBounds[0];
        eyes = new EyeBoxState[0]; aimingProgress=1; ShaderLensLate.reset();
    }
    public static void setReticle(Runnable render) { reticle = render; }
    public static boolean hasComposite() { return reticle != null; }
    public static void redrawReticle() { if (reticle != null) reticle.run(); }
    public static boolean available() {
        var mc = Minecraft.getInstance();
        return PrototypeConfig.LENS.get() && ScopeFramebufferPrototype.canCompositeOrCover()
                && OpticsPolicy.isLocalGun() && ScopeImageShader.get() != null
                && mc.options.getCameraType().isFirstPerson() && GL11.glIsEnabled(GL11.GL_STENCIL_TEST);
    }

    public static void draw(int[] opticalIndices) {
        var target = ScopeFramebufferPrototype.lensTarget();
        if (!OpticsPolicy.isOpticalViewSelected()) target = null;
        var mc = Minecraft.getInstance();
        if (!available()) return;
        if (!ShaderLensLate.isDrawing()) lastOpticalIndices = opticalIndices;
        long metricStart = RenderMetrics.begin();
        var projection = new Matrix4f(RenderSystem.getProjectionMatrix());
        var sorting = RenderSystem.getVertexSorting();
        var shader = RenderSystem.getShader();
        int texture = RenderSystem.getShaderTexture(0);
        int surfaceTexture = RenderSystem.getShaderTexture(1);
        float[] color = RenderSystem.getShaderColor().clone();
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        int func = GL11.glGetInteger(GL11.GL_STENCIL_FUNC);
        int ref = GL11.glGetInteger(GL11.GL_STENCIL_REF);
        int mask = GL11.glGetInteger(GL11.GL_STENCIL_VALUE_MASK);
        int writeMask = GL11.glGetInteger(GL11.GL_STENCIL_WRITEMASK);
        int stencilFail = GL11.glGetInteger(GL11.GL_STENCIL_FAIL);
        int depthFail = GL11.glGetInteger(GL11.GL_STENCIL_PASS_DEPTH_FAIL);
        int depthPass = GL11.glGetInteger(GL11.GL_STENCIL_PASS_DEPTH_PASS);
        var modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        try {
            // Use the actual ocular silhouette, not TaCZ's fixed-radius circle.
            // The replacement pass runs before TaCZ's circle/black mask;
            // original ocular indices 1..127 are still intact.
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableBlend();
            RenderSystem.disableCull();
            modelView.setIdentity();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(new Matrix4f(), sorting);
            RenderSystem.setShader(ScopeImageShader::get);
            ScopeImageShader.get().safeGetUniform("OpticsEnabled").set(0f);
            ScopeImageShader.get().safeGetUniform("IdleGlass").set(0f);
            RenderSystem.setShaderTexture(1,SURFACE_TEXTURES[Math.max(0,Math.min(SURFACE_TEXTURES.length-1,
                    PrototypeConfig.SURFACE_TEXTURE_TYPE.get()))]);
            ScopeImageShader.get().safeGetUniform("MaskDebug").set(PrototypeConfig.DEBUG_LENS_MASK.get() ? 1.0f : 0.0f);
            ScopeImageShader.get().safeGetUniform("LensReady").set(target == null ? 0.0f : 1.0f);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            RenderSystem.setShaderTexture(0, target == null ? 0 : target.getColorTextureId());
            ScopeImageShader.get().safeGetUniform("PupilLimited").set(0f);
            // Cover the excluded optical area, including the late shader pass.
            // Do not draw a textured square or touch auxiliary dot-site indices.
            ScopeImageShader.get().safeGetUniform("LensReady").set(0f);
            RenderSystem.stencilMask(0);
            RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
            for (int i=0;i<lastOpticalIndices.length;i++) {
                var b=i<lensBounds.length?lensBounds[i]:null;
                float idle=VisualEffectRules.idle(aimingProgress,
                        PrototypeConfig.LENS_EFFECTS.get()&&PrototypeConfig.IDLE_GLASS.get()&&OpticsPolicy.isOpticalViewSelected(),
                        PrototypeConfig.IDLE_GLASS_STRENGTH.get().floatValue());
                if(b!=null&&b.valid()&&idle>0) {
                    ScopeImageShader.get().safeGetUniform("LensCenter").set(b.x(),b.y());
                    ScopeImageShader.get().safeGetUniform("LensRadius").set(b.rx(),b.ry());
                    ScopeImageShader.get().safeGetUniform("LensView").set(b.tiltX(),b.tiltY());
                    ScopeImageShader.get().safeGetUniform("IdleGlass").set(idle);
                } else ScopeImageShader.get().safeGetUniform("IdleGlass").set(0f);
                RenderSystem.stencilFunc(GL11.GL_EQUAL, lastOpticalIndices[i], 0xff);
                drawQuad();
            }
            ScopeImageShader.get().safeGetUniform("IdleGlass").set(0f);
            ScopeImageShader.get().safeGetUniform("LensReady").set(target == null ? 0f : 1f);
            ScopeImageShader.get().safeGetUniform("ViewportSize").set((float)mc.getWindow().getWidth(), (float)mc.getWindow().getHeight());
            RenderSystem.colorMask(false, false, false, false);
            RenderSystem.stencilMask(0xff);
            RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_INVERT);
            // Promote only optical oculars; hybrid dot-sight indices stay low.
            for (int i = 0; i < opticalIndices.length; i++) {
                int index = opticalIndices[i];
                var eye=i<eyes.length ? eyes[i] : new EyeBoxState(true,0,0);
                // Shape comes from the pack, but displacement depends only on the ocular.
                PupilUniforms.set(ScopeImageShader.get(),shadowPlanes(i,mc.getWindow().getWidth(),mc.getWindow().getHeight()));
                ScopeImageShader.get().safeGetUniform("PupilLimited").set(eye.limited() ? 1f : 0f);
                ScopeImageShader.get().safeGetUniform("PupilRadius").set(eye.radius());
                RenderSystem.stencilFunc(GL11.GL_EQUAL, index, 0xff);
                drawQuad();
            }
            ScopeImageShader.get().safeGetUniform("PupilLimited").set(0f);
            RenderSystem.colorMask(true, true, true, true);
            RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
            RenderSystem.stencilFunc(GL11.GL_LESS, 127, 0xff);
            if (!PrototypeConfig.LENS_EFFECTS.get() || target == null) {
                drawQuad(); // Exact legacy texture sample and stencil path with effects disabled.
            } else {
                // Keep separate geometry/parameters for each optical ocular (including hybrids).
                // The shader-on late pass uses the same measurements as the saved stencil.
                for (int i = 0; i < lastOpticalIndices.length; i++) {
                    var bounds = i < lensBounds.length ? lensBounds[i] : null;
                    boolean valid = bounds != null && bounds.valid() && bounds.x() >= 0 && bounds.x() <= 1
                            && bounds.y() >= 0 && bounds.y() <= 1;
                    var imageShader = ScopeImageShader.get();
                    imageShader.safeGetUniform("OpticsEnabled").set(valid ? 1f : 0f);
                    if (valid) {
                        imageShader.safeGetUniform("LensCenter").set(bounds.x(), bounds.y());
                        imageShader.safeGetUniform("LensRadius").set(bounds.rx(), bounds.ry());
                        var eye=bounds.eye();
                        float viewX=0,viewY=0;
                        if(eye!=null&&eye.visible()&&eye.finite()) {
                            viewX=Math.max(-2,Math.min(2,(eye.pupilX()-eye.centerX())/bounds.rx()));
                            viewY=Math.max(-2,Math.min(2,(eye.pupilY()-eye.centerY())/bounds.ry()));
                        }
                        imageShader.safeGetUniform("LensView").set(viewX,viewY);
                        imageShader.safeGetUniform("EdgeRefraction").set(PrototypeConfig.EDGE_REFRACTION.get()
                                ? PrototypeConfig.EDGE_REFRACTION_STRENGTH.get().floatValue() : 0f);
                        imageShader.safeGetUniform("Ghost").set(PrototypeConfig.LENS_GHOST.get()
                                ? PrototypeConfig.GHOST_STRENGTH.get().floatValue() : 0f);
                        imageShader.safeGetUniform("SurfaceTexture").set(PrototypeConfig.SURFACE_TEXTURE.get()?1f:0f);
                        imageShader.safeGetUniform("Reflection").set(PrototypeConfig.LENS_REFLECTION.get()
                                ? PrototypeConfig.REFLECTION_STRENGTH.get().floatValue() : 0f);
                        imageShader.safeGetUniform("PeripheralBlur").set(PrototypeConfig.PERIPHERAL_BLUR.get()
                                ? PrototypeConfig.PERIPHERAL_BLUR_STRENGTH.get().floatValue() : 0f);
                        imageShader.safeGetUniform("Dirt").set(PrototypeConfig.LENS_DIRT.get()
                                ? PrototypeConfig.DIRT_STRENGTH.get().floatValue() : 0f);
                        float scratchDensity=PrototypeConfig.SCRATCH_DENSITY.get().floatValue();
                        float scratchSize=PrototypeConfig.SCRATCH_SIZE.get().floatValue();
                        imageShader.safeGetUniform("ScratchDensity").set(scratchDensity);
                        imageShader.safeGetUniform("ScratchSize").set(scratchSize);
                        imageShader.safeGetUniform("SurfacePattern").set(PrototypeConfig.SURFACE_PATTERN.get().floatValue());
                        imageShader.safeGetUniform("Scratches").set(PrototypeConfig.LENS_SCRATCHES.get() && (PrototypeConfig.SURFACE_TEXTURE.get() || scratchDensity>0) && scratchSize>0
                                ? PrototypeConfig.SCRATCH_STRENGTH.get().floatValue() : 0f);
                        float[] shape=LensShapeMetric.coefficients(bounds);
                        imageShader.safeGetUniform("OpticEdgeCount").set(shape.length/2f);
                        for(int edge=0;edge<shape.length/2;edge++)
                            imageShader.safeGetUniform("OpticEdges["+edge+"]").set(shape[edge*2],shape[edge*2+1]);
                        imageShader.safeGetUniform("Distortion").set(PrototypeConfig.LENS_DISTORTION.get()
                                ? PrototypeConfig.DISTORTION_STRENGTH.get().floatValue() : 0f);
                        imageShader.safeGetUniform("Chromatic").set(PrototypeConfig.CHROMATIC_ABERRATION.get()
                                ? PrototypeConfig.CHROMATIC_STRENGTH.get().floatValue() : 0f);
                        imageShader.safeGetUniform("Vignette").set(PrototypeConfig.LENS_VIGNETTE.get()
                                ? PrototypeConfig.VIGNETTE_STRENGTH.get().floatValue() : 0f);
                        imageShader.safeGetUniform("Coating").set(PrototypeConfig.LENS_COATING.get()
                                ? PrototypeConfig.COATING_STRENGTH.get().floatValue() : 0f);
                        imageShader.safeGetUniform("Haze").set(PrototypeConfig.LENS_HAZE.get()
                                ? PrototypeConfig.HAZE_STRENGTH.get().floatValue() : 0f);
                    }
                    RenderSystem.stencilFunc(GL11.GL_EQUAL, 255 - lastOpticalIndices[i], 0xff);
                    drawQuad();
                }
            }
        } finally {
            ScopeImageShader.get().safeGetUniform("OpticsEnabled").set(0f);
            ScopeImageShader.get().safeGetUniform("IdleGlass").set(0f);
            ScopeImageShader.get().safeGetUniform("PupilLimited").set(0f);
            ScopeImageShader.get().safeGetUniform("MaskDebug").set(0.0f);
            ScopeImageShader.get().safeGetUniform("LensReady").set(1.0f);
            RenderSystem.colorMask(true, true, true, true);
            RenderSystem.stencilMask(writeMask);
            RenderSystem.stencilOp(stencilFail, depthFail, depthPass);
            RenderSystem.stencilFunc(func, ref, mask);
            RenderSystem.setShaderTexture(0, texture);
            RenderSystem.setShaderTexture(1, surfaceTexture);
            RenderSystem.setShaderColor(color[0], color[1], color[2], color[3]);
            RenderSystem.setShader(() -> shader);
            RenderSystem.setProjectionMatrix(projection, sorting);
            modelView.popPose();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.depthMask(depthMask);
            if (depth) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
            if (blend) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
            if (cull) RenderSystem.enableCull(); else RenderSystem.disableCull();
            RenderMetrics.end(RenderMetrics.COMPOSITE, metricStart);
        }
    }

    private static void drawQuad() {
            var buffer = Tesselator.getInstance().getBuilder();
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            buffer.vertex(-1, -1, 0).uv(0, 0).endVertex();
            buffer.vertex(1, -1, 0).uv(1, 0).endVertex();
            buffer.vertex(1, 1, 0).uv(1, 1).endVertex();
            buffer.vertex(-1, 1, 0).uv(0, 1).endVertex();
            BufferUploader.drawWithShader(buffer.end());
    }
}
