package com.shumai.taczpip.mixin;

import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import com.shumai.taczpip.PrototypeConfig;
import com.shumai.taczpip.client.ScopeFramebufferPrototype;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public abstract class ScopeEntityFilterMixin {
    @Inject(method = {"render", "m_114384_"}, at = @At("HEAD"), cancellable = true, remap = false, require = 1)
    private void taczpip$filter(CallbackInfo ci) {
        if (ScopeFramebufferPrototype.isRenderingWorld() && !PrototypeConfig.PIP_ENTITIES.get()) ci.cancel();
    }
}
