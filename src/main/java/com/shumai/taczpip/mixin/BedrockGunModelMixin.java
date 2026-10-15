package com.shumai.taczpip.mixin;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.shumai.taczpip.PrototypeConfig;
import com.shumai.taczpip.client.OpticsPolicy;
import com.shumai.taczpip.client.ScopeFramebufferPrototype;
import com.shumai.taczpip.client.ScopeLensComposite;
import com.shumai.taczpip.client.LensDepthReset;
import com.shumai.taczpip.client.RenderMetrics;
import com.tacz.guns.client.model.BedrockGunModel;
import com.tacz.guns.client.model.BedrockAnimatedModel;
import com.tacz.guns.client.resource.pojo.model.BedrockModelPOJO;
import com.tacz.guns.client.resource.pojo.model.BedrockVersion;
import org.joml.Matrix4f;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BedrockGunModel.class, remap = false)
public abstract class BedrockGunModelMixin extends BedrockAnimatedModel {
    protected BedrockGunModelMixin(BedrockModelPOJO pojo, BedrockVersion version) { super(pojo, version); }

    // The original body pass keeps TaCZ's outside-lens stencil test. Add a
    // second body-only pass, after it but before TaCZ clears the lens stencil.
    // Calling the superclass excludes the separately rendered scope attachment
    // and avoids repeating animation/camera updates or recursive gun rendering.
    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lcom/tacz/guns/util/RenderHelper;disableItemEntityStencilTest()V"))
    private void taczpip$keepVisibleGun(PoseStack pose, ItemStack gun, ItemDisplayContext context,
                                     RenderType type, int light, int overlay, CallbackInfo ci) {
        if (context.firstPerson() && com.shumai.taczpip.client.OculusCompat.shadersActive()
                && !com.shumai.taczpip.client.ShaderLensLate.isDrawing()) {
            // Do not put magnified hand depth into the shader pack's world
            // buffers: its AO/lighting passes interpret it as scene geometry.
            PoseStack saved = new PoseStack();
            saved.last().pose().set(pose.last().pose());
            saved.last().normal().set(pose.last().normal());
            // TaCZ clears per-bone sway/reload transforms after the normal hand pass.
            // A PoseStack copy alone cannot preserve those independent model transforms.
            var bones = new com.shumai.taczpip.client.GunPoseSnapshot(modelMap.values().stream()
                    .map(wrapper -> wrapper.getModelRenderer()).toList());
            var gunModel = (BedrockGunModel)(Object)this;
            boolean hand = gunModel.getRenderHand();
            com.shumai.taczpip.client.ShaderLensLate.capture(() -> bones.run(() -> {
                boolean previousHand = gunModel.getRenderHand();
                try {
                    gunModel.setRenderHand(hand);
                    taczpip$keepVisibleGun(saved, gun, context, type, light, overlay, ci);
                } finally { gunModel.setRenderHand(previousHand); }
            }));
            return;
        }
        if (context.firstPerson() && !PrototypeConfig.DEBUG_LENS_MASK.get() && PrototypeConfig.SHOW_GUN.get() && PrototypeConfig.LENS.get()
                && OpticsPolicy.isSupportedScopeActive() && ScopeFramebufferPrototype.hasLensFrame()
                && ScopeLensComposite.hasComposite() && OpticsPolicy.isOpticalViewSelected()) {
            var projection = new Matrix4f(RenderSystem.getProjectionMatrix());
            var sorting = RenderSystem.getVertexSorting();
            int func = GL11.glGetInteger(GL11.GL_STENCIL_FUNC);
            int ref = GL11.glGetInteger(GL11.GL_STENCIL_REF);
            int mask = GL11.glGetInteger(GL11.GL_STENCIL_VALUE_MASK);
            long metricStart = RenderMetrics.begin();
            try {
                float zoom = ScopeFramebufferPrototype.lensMagnification();
                RenderSystem.setProjectionMatrix(new Matrix4f().scaling(zoom, zoom, 1).mul(projection), sorting);
                RenderSystem.stencilFunc(GL11.GL_LESS, 127, 0xff);
                // The attachment was drawn with the unzoomed hand projection.
                // Its depth cannot occlude a body rendered with a different projection.
                com.shumai.taczpip.client.ScopeGunResolution.render(
                        () -> com.shumai.taczpip.client.VertexReuseContext.inner(
                                () -> com.shumai.taczpip.client.GunDrawProfile.draw(
                                        () -> super.render(pose, context, type, light, overlay))));
            } finally {
                RenderSystem.setProjectionMatrix(projection, sorting);
                RenderSystem.stencilFunc(func, ref, mask);
                RenderMetrics.end(RenderMetrics.BODY, metricStart);
            }
            if (!PrototypeConfig.DEFER_RETICLE.get()) ScopeLensComposite.redrawReticle();
        }
        // Also run when the inner body is disabled, on the auxiliary sight, or
        // before a PiP frame exists. Stencil is still intact at this injection.
        if (PrototypeConfig.DEFER_RETICLE.get() && context.firstPerson()
                && !PrototypeConfig.DEBUG_LENS_MASK.get() && ScopeLensComposite.hasComposite())
            ScopeLensComposite.redrawReticle();
        if (context.firstPerson()) ScopeLensComposite.softenMaskEdge();
    }
}
