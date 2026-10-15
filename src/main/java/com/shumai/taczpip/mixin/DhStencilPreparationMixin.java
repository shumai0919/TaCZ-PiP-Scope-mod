package com.shumai.taczpip.mixin;

import com.shumai.taczpip.client.DhStencilPreparation;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GameRenderer.class, priority = 500)
public abstract class DhStencilPreparationMixin {
    // GameRenderer.render precedes LevelRenderer/DH terrain and Oculus world
    // pipeline setup. Not a hand/ADS hook, and not invoked by the PiP sub-pass.
    @Inject(method = {"render(FJZ)V", "m_109093_(FJZ)V"}, at = @At("HEAD"), remap = false, require = 1)
    private void taczpip$prepareDepthStencil(CallbackInfo ci) {
        DhStencilPreparation.beforeFrame();
    }
}
