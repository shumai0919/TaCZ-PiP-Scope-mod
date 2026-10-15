package com.shumai.taczpip.mixin;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import com.shumai.taczpip.PrototypeConfig;
import com.shumai.taczpip.client.ScopeFramebufferPrototype;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class ScopeBlockEntityFilterMixin {
    @Inject(method = {"render", "m_112267_"}, at = @At("HEAD"), cancellable = true, remap = false, require = 1)
    private void taczpip$filter(CallbackInfo ci) {
        if (ScopeFramebufferPrototype.isRenderingWorld() && !PrototypeConfig.PIP_BLOCK_ENTITIES.get()) ci.cancel();
    }
}
