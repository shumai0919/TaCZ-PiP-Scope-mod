package com.shumai.taczpip.mixin;

import com.shumai.taczpip.client.DhTemporalState;
import com.shumai.taczpip.client.ScopeFramebufferPrototype;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = {
    "com.seibel.distanthorizons.common.render.openGl.postProcessing.antialiasing.GlDhTaaShader",
    "com.seibel.distanthorizons.common.render.openGl.postProcessing.antialiasing.GlDhTaaShader_forge"
}, remap = false)
public abstract class DhModernTaaSeedMixin {
    @Inject(method = "renderPrep(III)V", at = @At("RETURN"))
    private void taczpip$seed(CallbackInfo ci) {
        if (ScopeFramebufferPrototype.isRenderingWorld() && DhTemporalState.enabled())
            DhTemporalState.seedHistory(this);
    }
}
