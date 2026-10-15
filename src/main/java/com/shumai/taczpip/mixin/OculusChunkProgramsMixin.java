package com.shumai.taczpip.mixin;

import com.shumai.taczpip.client.ShaderScopeContext;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.irisshaders.iris.compat.sodium.impl.shader_overrides.IrisChunkProgramOverrides", remap = false)
public abstract class OculusChunkProgramsMixin {
    @Inject(method = "deleteShaders", at = @At("HEAD"))
    private void taczpip$deleteSeparate(CallbackInfo ci) {
        ShaderScopeContext.discardPrograms(this);
    }
    @Inject(method = "getProgramOverride", at = @At("HEAD"), cancellable = true)
    private void taczpip$separatePrograms(@Coerce Object pass, @Coerce Object vertex,
                                         CallbackInfoReturnable<Object> cir) {
        if (ShaderScopeContext.shouldRoutePrograms())
            cir.setReturnValue(ShaderScopeContext.program(this, pass, vertex));
    }
}
