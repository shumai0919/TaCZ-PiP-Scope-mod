package com.shumai.taczpip.mixin;

import com.shumai.taczpip.client.DhScopeBuffers;
import com.shumai.taczpip.client.DhScopeContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.seibel.distanthorizons.core.render.renderer.LodRenderer", remap = false)
public abstract class DhScopeBuffersMixin {
    // After initial renderer setup, before DH compares cached dimensions/reallocates.
    @Inject(method = "setupGLStateAndRenderObjects", at = @At(value = "INVOKE",
            target = "Lcom/seibel/distanthorizons/core/wrapperInterfaces/minecraft/IMinecraftRenderWrapper;getTargetFrameBufferViewportWidth()I",
            ordinal = 0), require = 1)
    private void taczpip$buffers(CallbackInfo ci) { DhScopeContext.prepareBuffers(this); }

    @Inject(method = "cleanup", at = @At("HEAD"), require = 1)
    private void taczpip$cleanup(CallbackInfo ci) { DhScopeBuffers.release(this); }
}
