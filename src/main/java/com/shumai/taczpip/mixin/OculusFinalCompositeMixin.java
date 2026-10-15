package com.shumai.taczpip.mixin;

import com.shumai.taczpip.client.ShaderLensLate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.irisshaders.iris.pipeline.IrisRenderingPipeline", remap = false)
public abstract class OculusFinalCompositeMixin {
    @Inject(method = "finalizeLevelRendering", at = @At("TAIL"), remap = false)
    private void taczpip$finalLens(CallbackInfo ci) { ShaderLensLate.draw(); }
}
