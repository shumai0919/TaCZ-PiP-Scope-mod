package com.shumai.taczpip.mixin;
import com.shumai.taczpip.client.OpticsPolicy;
import com.tacz.guns.api.item.nbt.AttachmentItemDataAccessor;
import com.tacz.guns.client.event.FirstPersonRenderGunEvent;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = FirstPersonRenderGunEvent.class, remap = false)
public abstract class FirstPersonPositionMixin {
    @Redirect(method = "applyFirstPersonPositioningTransform", at = @At(value = "INVOKE",
            target = "Lcom/tacz/guns/api/item/nbt/AttachmentItemDataAccessor;getZoomNumberFromTag(Lnet/minecraft/nbt/CompoundTag;)I"))
    private static int taczpip$fixedEyePosition(CompoundTag tag) {
        return OpticsPolicy.modelViewStage(AttachmentItemDataAccessor.getZoomNumberFromTag(tag));
    }
}
