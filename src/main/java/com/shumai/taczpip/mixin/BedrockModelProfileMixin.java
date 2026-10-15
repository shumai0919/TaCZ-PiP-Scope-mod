package com.shumai.taczpip.mixin;

import com.shumai.taczpip.client.GunDrawProfile;
import com.shumai.taczpip.client.VertexReuseContext;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.client.renderer.RenderType;
import com.tacz.guns.client.model.bedrock.BedrockModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BedrockModel.class, remap = false)
public abstract class BedrockModelProfileMixin {
    private static final String DRAW = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/client/renderer/RenderType;IIFFFF)V";
    @Inject(method = DRAW, at = @At("HEAD"))
    private void taczpip$vertices(PoseStack pose, ItemDisplayContext context, RenderType type,
                                 int light, int overlay, float r, float g, float b, float a, CallbackInfo ci) {
        VertexReuseContext.begin(this, context);
        GunDrawProfile.modelStart();
    }

    @Inject(method = DRAW, at = @At(value = "INVOKE", target = "Lcom/tacz/guns/compat/oculus/OculusCompat;endBatch(Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)Z"))
    private void taczpip$submit(CallbackInfo ci) { GunDrawProfile.beforeSubmit(); }

    @Inject(method = DRAW, at = @At("RETURN"))
    private void taczpip$done(CallbackInfo ci) { GunDrawProfile.modelEnd(); VertexReuseContext.end(); }
}
