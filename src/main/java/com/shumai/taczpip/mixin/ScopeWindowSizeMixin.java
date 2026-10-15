package com.shumai.taczpip.mixin;

import com.mojang.blaze3d.platform.Window;
import com.shumai.taczpip.client.ScopeFramebufferPrototype;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Render-size queries (including DH) follow the reduced target only inside PiP. */
@Mixin(Window.class)
public abstract class ScopeWindowSizeMixin {
    @Inject(method = {"getWidth", "m_85441_"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void taczpip$width(CallbackInfoReturnable<Integer> ci) {
        int size = ScopeFramebufferPrototype.renderWidth();
        if (size > 0) ci.setReturnValue(size);
    }
    @Inject(method = {"getHeight", "m_85442_"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void taczpip$height(CallbackInfoReturnable<Integer> ci) {
        int size = ScopeFramebufferPrototype.renderHeight();
        if (size > 0) ci.setReturnValue(size);
    }
}
