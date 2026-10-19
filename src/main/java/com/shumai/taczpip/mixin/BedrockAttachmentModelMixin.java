package com.shumai.taczpip.mixin;

import com.shumai.taczpip.client.ScopeLensComposite;
import com.shumai.taczpip.client.ReticleRenderType;
import com.shumai.taczpip.client.OpticalModel;
import com.shumai.taczpip.client.LensStencilRules;
import com.shumai.taczpip.client.ReticleBackgroundFilter;
import com.shumai.taczpip.client.ActiveLensRules;
import com.shumai.taczpip.client.OpticsPolicy;
import com.shumai.taczpip.client.RenderMetrics;
import org.spongepowered.asm.mixin.Unique;
import com.shumai.taczpip.PrototypeConfig;
import com.tacz.guns.client.model.BedrockAttachmentModel;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import org.lwjgl.opengl.GL11;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// All named targets belong to TaCZ or RenderSystem, not obfuscated Minecraft members.
@Mixin(value = BedrockAttachmentModel.class, remap = false)
public abstract class BedrockAttachmentModelMixin implements OpticalModel {
    @Shadow public abstract boolean isScope();
    @Shadow public abstract boolean isSight();
    @Shadow protected List<List<BedrockPart>> divisionNodePaths;
    @Shadow protected List<List<BedrockPart>> ocularNodePaths;
    @Shadow protected List<Boolean> isScopeOcular;
    @Shadow public abstract List<BedrockPart> getScopeViewPath(int view);
    @Unique private final PoseStack taczpip$measurePose = new PoseStack();
    @Unique private double[] taczpip$lensX = new double[0];
    @Unique private double[] taczpip$lensY = new double[0];
    @Unique private boolean[] taczpip$opticalFlags = new boolean[0];
    @Unique private Boolean taczpip$nativeHud;

    @Override
    public boolean taczpip$nativeHud() {
        if(taczpip$nativeHud!=null) return taczpip$nativeHud;
        boolean found=false;
        if(isScope()) for(int i=0;i<ocularNodePaths.size() && i<127;i++) {
            if(!LensStencilRules.optical(isSight(),isScopeOcular,i)) continue;
            var path=ocularNodePaths.get(i);
            if(path.isEmpty()) continue;
            var root=path.get(path.size()-1);
            // Empty anchor with exclusively emissive child geometry is a HUD,
            // not a physical aperture. Fall back for the whole attachment to
            // avoid mixing native and promoted stencil indices in hybrid models.
            if(root.cubes.isEmpty() && !root.children.isEmpty() &&
                    com.shumai.taczpip.client.HudOpticRules.emissiveOnly(root,
                            p->p.children,p->!p.cubes.isEmpty(),p->p.illuminated)) found=true;
        }
        taczpip$nativeHud=found;
        return found;
    }

    @Override
    public boolean taczpip$isOpticalView(int view) {
        if(taczpip$nativeHud()) return false;
        if (!isSight()) return taczpip$hasOpticalLens();
        var eye = getScopeViewPath(view);
        if (eye == null || eye.isEmpty()) return false;
        PoseStack eyePose = taczpip$measurePose;
        eyePose.setIdentity();
        for (var part : eye) part.translateAndRotateAndScale(eyePose);
        double eyeX = eyePose.last().pose().m30(), eyeY = eyePose.last().pose().m31();
        int count = Math.min(ocularNodePaths.size(), isScopeOcular.size());
        if (taczpip$lensX.length != count) {
            taczpip$lensX = new double[count]; taczpip$lensY = new double[count];
            taczpip$opticalFlags = new boolean[count];
        }
        double[] x = taczpip$lensX, y = taczpip$lensY;
        boolean[] optical = taczpip$opticalFlags;
        for (int i = 0; i < count; i++) {
            PoseStack lensPose = taczpip$measurePose;
            lensPose.setIdentity();
            for (var part : ocularNodePaths.get(i)) part.translateAndRotateAndScale(lensPose);
            x[i] = lensPose.last().pose().m30();
            y[i] = lensPose.last().pose().m31();
            optical[i] = Boolean.TRUE.equals(isScopeOcular.get(i));
        }
        return ActiveLensRules.optical(eyeX, eyeY, x, y, optical);
    }
    @Override
    public boolean taczpip$hasOpticalLens() {
        if (!isScope() || taczpip$nativeHud()) return false;
        for (int i = 0; i < ocularNodePaths.size() && i < 127; i++) {
            if (LensStencilRules.optical(isSight(), isScopeOcular, i)) return true;
        }
        return false;
    }
    @Shadow private void renderTempPart(PoseStack pose, ItemDisplayContext context, RenderType type,
                                       int light, int overlay, List<BedrockPart> path) { throw new AssertionError(); }

    // Replace the complete optical mask/reticle pass. Previously the original
    // black ocular draw still ran after our stencil promotion/composite.
    @Inject(method = "renderOcularAndDivision", at = @At("HEAD"), cancellable = true, remap = false)
    private void taczpip$drawLens(PoseStack pose, ItemDisplayContext context, RenderType type,
                                 int light, int overlay, boolean selective, CallbackInfo ci) {
        if (isScope() && taczpip$hasOpticalLens() && ScopeLensComposite.available()) {
            var mc = net.minecraft.client.Minecraft.getInstance();
            float progress = com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator.fromLocalPlayer(mc.player)
                    .getClientAimingProgress(mc.getFrameTime());
            int[] indices = LensStencilRules.indices(selective, isScopeOcular, ocularNodePaths.size());
            var eyes = new com.shumai.taczpip.client.EyeBoxState[indices.length];
            var bounds = new com.shumai.taczpip.client.LensBounds[indices.length];
            for (int i = 0; i < indices.length; i++) {
                int node=indices[i]-1;
                var divisionPath=node<divisionNodePaths.size() ? divisionNodePaths.get(node) : java.util.List.<BedrockPart>of();
                float[] shape=divisionPath.isEmpty() ? new float[0]
                        : com.shumai.taczpip.client.ReticleTexture.openingShape(OpticsPolicy.scopeTexture(this),
                            divisionPath.get(divisionPath.size()-1),context);
                bounds[i] = com.shumai.taczpip.client.EyeBoxMeasure.shaped(pose, context,
                        ocularNodePaths.get(node), OpticsPolicy.scopeTexture(this),shape);
                eyes[i] = com.shumai.taczpip.client.EyeBoxState.measure(bounds[i],progress,
                        mc.getWindow().getWidth(), mc.getWindow().getHeight());
            }
            ScopeLensComposite.setEyeBox(eyes);
            ScopeLensComposite.setAimingProgress(progress);
            ScopeLensComposite.setLensBounds(bounds);
            ScopeLensComposite.draw(indices);
            if (PrototypeConfig.DEBUG_LENS_MASK.get()) { ci.cancel(); return; }
            PoseStack saved = new PoseStack();
            saved.last().pose().set(pose.last().pose());
            saved.last().normal().set(pose.last().normal());
            var reticleTexture = OpticsPolicy.scopeTexture(this);
            ScopeLensComposite.setReticle(() -> {
                long metricStart = RenderMetrics.begin();
                boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
                boolean writeDepth = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
                int func = GL11.glGetInteger(GL11.GL_STENCIL_FUNC);
                int ref = GL11.glGetInteger(GL11.GL_STENCIL_REF);
                int mask = GL11.glGetInteger(GL11.GL_STENCIL_VALUE_MASK);
                try {
                    RenderSystem.disableDepthTest();
                    RenderSystem.depthMask(false);
                    for (int i = 0; i < Math.min(ocularNodePaths.size(), divisionNodePaths.size()) && i < 127; i++) {
                        boolean optical = LensStencilRules.optical(selective, isScopeOcular, i);
                        if (optical && !OpticsPolicy.isOpticalViewSelected()) continue;
                        RenderSystem.stencilFunc(GL11.GL_EQUAL, LensStencilRules.reticleReference(optical, i), 0xff);
                        var path = divisionNodePaths.get(i);
                        var filtered = optical && !path.isEmpty() ? com.shumai.taczpip.client.ReticleTexture.get(
                                reticleTexture, path.get(path.size()-1), context) : null;
                        Runnable draw = () -> renderTempPart(saved, context, ReticleRenderType.overlay(type,filtered), light, overlay, path);
                        if (optical && !path.isEmpty()) {
                            var division=path.get(path.size()-1);
                            ReticleBackgroundFilter.render(division, () ->
                                    com.shumai.taczpip.client.ReticleTexture.render(reticleTexture,division,draw));
                        } else {
                            draw.run();
                        }
                    }
                } finally {
                    RenderSystem.stencilFunc(func, ref, mask);
                    RenderSystem.depthMask(writeDepth);
                    if (depth) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
                    RenderMetrics.end(RenderMetrics.RETICLE, metricStart);
                }
            });
            if (!PrototypeConfig.DEFER_RETICLE.get()) ScopeLensComposite.redrawReticle();
            ci.cancel();
        }
    }
}
