package com.shumai.taczpip.mixin;

import com.mojang.logging.LogUtils;
import com.shumai.taczpip.client.ScopeFramebufferPrototype;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.seibel.distanthorizons.core.api.internal.ClientApi", remap = false)
public abstract class DhClientApiMixin {
    @Unique private static boolean taczpip$loggedDh;
    @Inject(method = "renderLods", at = @At("HEAD"), remap = false, require = 0)
    private void taczpip$observeDh(CallbackInfo ci) {
        if (ScopeFramebufferPrototype.isRenderingWorld() && !taczpip$loggedDh) {
            taczpip$loggedDh = true;
            LogUtils.getLogger().info("[TaCZ PiP Scope] DH scope render hook reached; magnification={}. Visual LOD output still requires verification.", ScopeFramebufferPrototype.lensMagnification());
        }
    }
}
