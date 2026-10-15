package com.shumai.taczpip.mixin;

import com.shumai.taczpip.client.ShaderScopeContext;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.irisshaders.iris.pathways.HandRenderer", remap = false)
public abstract class OculusHandMixin {
    @Inject(method = {"renderSolid", "renderTranslucent"}, at = @At("HEAD"), cancellable = true)
    private void taczpip$noScopeHands(CallbackInfo ci) {
        if (ShaderScopeContext.active()) ci.cancel();
    }
}
