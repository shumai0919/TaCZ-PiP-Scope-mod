package com.shumai.taczpip.mixin;

import com.shumai.taczpip.client.ShaderScopeContext;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.irisshaders.iris.pipeline.PipelineManager", remap = false)
public abstract class OculusPipelineLifecycleMixin {
    @Inject(method = "destroyPipeline", at = @At("HEAD"))
    private void taczpip$destroyScope(CallbackInfo ci) {
        ShaderScopeContext.onManagerDestroyed(this);
    }
}
