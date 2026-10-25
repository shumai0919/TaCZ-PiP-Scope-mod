package com.shumai.taczpip.mixin;

import com.shumai.taczpip.client.DhTemporalState;
import com.shumai.taczpip.client.ScopeFramebufferPrototype;
import org.lwjgl.opengl.GL20;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = {
    "com.seibel.distanthorizons.common.render.openGl.terrain.GlDhTerrainShaderProgram",
    "com.seibel.distanthorizons.common.render.openGl.terrain.GlDhTerrainShaderProgram_forge"
}, remap = false)
public abstract class DhModernJitterMixin {
    @Shadow private int frameIndexMod8;
    @Shadow public int uFrameMod8;

    @Inject(method = "fillUniformData", at = @At("HEAD"))
    private void taczpip$saveCounter(CallbackInfo ci) {
        if (!ScopeFramebufferPrototype.isRenderingWorld()) return;
        int saved = frameIndexMod8;
        DhTemporalState.restoreAfterPass(() -> frameIndexMod8 = saved);
    }

    @Inject(method = "fillUniformData", at = @At("RETURN"))
    private void taczpip$scopeJitter(CallbackInfo ci) {
        if (ScopeFramebufferPrototype.isRenderingWorld())
            GL20.glUniform1f(uFrameMod8, DhTemporalState.jitter(frameIndexMod8));
    }
}
