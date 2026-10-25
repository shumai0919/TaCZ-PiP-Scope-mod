package com.shumai.taczpip.mixin;

import com.shumai.taczpip.client.ScopeFramebufferPrototype;
import com.shumai.taczpip.client.DhTemporalState;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Main-view temporal history must never consume a differently projected PiP frame. */
@Pseudo
@Mixin(targets = {
        "com.seibel.distanthorizons.common.render.openGl.postProcessing.antialiasing.GlDhTaaRenderer",
        "com.seibel.distanthorizons.common.render.openGl.postProcessing.antialiasing.GlDhTaaRenderer_forge"
}, remap = false)
public abstract class DhModernTaaMixin {
    @Inject(method = "render(Lcom/seibel/distanthorizons/core/render/RenderParams;)V",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void taczpip$keepMainHistory(@Coerce Object params, CallbackInfo ci) {
        if (!ScopeFramebufferPrototype.isRenderingWorld()) return;
        if (!DhTemporalState.enabled()) ci.cancel();
        else DhTemporalState.beforeRender(this, params);
    }

    @Inject(method = "render(Lcom/seibel/distanthorizons/core/render/RenderParams;)V", at = @At("RETURN"))
    private void taczpip$historyReady(CallbackInfo ci) {
        if (ScopeFramebufferPrototype.isRenderingWorld() && DhTemporalState.enabled())
            DhTemporalState.afterRender(this);
    }
}
