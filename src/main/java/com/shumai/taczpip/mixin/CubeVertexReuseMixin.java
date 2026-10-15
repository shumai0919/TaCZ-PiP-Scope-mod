package com.shumai.taczpip.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.shumai.taczpip.client.CubeVertexReuse;
import com.shumai.taczpip.client.VertexReuseContext;
import com.tacz.guns.client.model.bedrock.BedrockCubeBox;
import com.tacz.guns.client.model.bedrock.BedrockCubePerFace;
import com.tacz.guns.client.model.bedrock.BedrockPolygon;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = {BedrockCubeBox.class, BedrockCubePerFace.class}, remap = false)
public abstract class CubeVertexReuseMixin {
    @Shadow(remap = false) @Final private BedrockPolygon[] polygons;
    @Unique private CubeVertexReuse taczpip$cache;
    @Inject(method = "compile", at = @At("HEAD"), cancellable = true)
    private void taczpip$reuse(PoseStack.Pose pose, VertexConsumer consumer, int light, int overlay,
                              float r, float g, float b, float a, CallbackInfo ci) {
        if (!VertexReuseContext.ENABLED) return;
        if (VertexReuseContext.PROFILING) VertexReuseContext.cubeCalls++;
        if (!VertexReuseContext.eligible()) {
            if (VertexReuseContext.PROFILING) VertexReuseContext.cubeOutsideScope++;
            return;
        }
        // Custom/subclass geometry (including mesh loaders) remains on its original path.
        Class<?> type = getClass();
        if (type != BedrockCubeBox.class && type != BedrockCubePerFace.class) {
            if (VertexReuseContext.PROFILING) VertexReuseContext.cubeSubclass++;
            return;
        }
        if (taczpip$cache == null) taczpip$cache = new CubeVertexReuse();
        if (taczpip$cache.render(polygons, pose, consumer, light, overlay, r, g, b, a)) ci.cancel();
    }
}
