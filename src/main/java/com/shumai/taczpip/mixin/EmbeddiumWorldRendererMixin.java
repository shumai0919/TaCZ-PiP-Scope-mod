package com.shumai.taczpip.mixin;

import com.shumai.taczpip.client.EmbeddiumCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Optional target: no Embeddium classes are linked when it is absent. */
@Pseudo
@Mixin(targets = "me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer", remap = false)
public abstract class EmbeddiumWorldRendererMixin {
    @Inject(method = "setupTerrain", at = @At("HEAD"), cancellable = true, require = 1)
    private void taczpip$reuseMainTerrain(CallbackInfo ci) {
        // PiP keeps the main camera and only narrows its projection. The main
        // visible set is a superset: keep it and the viewport/entity culling state.
        // Do not repeat chunk uploads, async updates or tickVisibleRenders.
        if (EmbeddiumCompat.reuseMainTerrain()) ci.cancel();
    }
}
