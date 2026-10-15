package com.shumai.taczpip.mixin;

import net.minecraft.client.particle.ParticleEngine;
import com.shumai.taczpip.PrototypeConfig;
import com.shumai.taczpip.client.ScopeFramebufferPrototype;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleEngine.class)
public abstract class ScopeParticleFilterMixin {
    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;)V", at = @At("HEAD"), cancellable = true, remap = false, require = 1)
    private void taczpip$filter(CallbackInfo ci) {
        if (ScopeFramebufferPrototype.isRenderingWorld() && !PrototypeConfig.PIP_PARTICLES.get()) ci.cancel();
    }
}
