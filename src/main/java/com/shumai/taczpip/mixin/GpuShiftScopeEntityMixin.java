package com.shumai.taczpip.mixin;

import com.shumai.taczpip.client.ScopeFramebufferPrototype;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** GpuShift 1.2.6: main-view distance/budget decisions must not hide scope targets. */
@Pseudo
@Mixin(targets = "com.orf.gpushift.client.GpuShiftClientRuntime", remap = false)
public abstract class GpuShiftScopeEntityMixin {
    @Inject(method = "shouldCullLivingEntity", at = @At("HEAD"), cancellable = true, require = 1)
    private void taczpip$preserveScopeTargets(CallbackInfoReturnable<Boolean> ci) {
        if (ScopeFramebufferPrototype.isRenderingWorld()) ci.setReturnValue(false);
    }
}
