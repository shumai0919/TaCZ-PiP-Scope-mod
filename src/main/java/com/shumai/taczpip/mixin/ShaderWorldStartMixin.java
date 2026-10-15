package com.shumai.taczpip.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.shumai.taczpip.client.ScopeFramebufferPrototype;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.*;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Execute before Oculus's HEAD hook starts the main-view pipeline.
@Mixin(value = LevelRenderer.class, priority = 500)
public abstract class ShaderWorldStartMixin {
    @Inject(method = {"renderLevel", "m_109599_"}, at = @At("HEAD"), remap = false, require = 1)
    private void taczpip$beforeWorld(PoseStack pose, float partialTick, long time, boolean outline,
            Camera camera, GameRenderer renderer, LightTexture light, Matrix4f projection, CallbackInfo ci) {
        ScopeFramebufferPrototype.beforeShaderWorld(pose, partialTick, time, camera, projection);
    }
}
