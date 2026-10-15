package com.shumai.taczpip.mixin;
import com.shumai.taczpip.client.OpticsPolicy;
import com.shumai.taczpip.client.FovRules;
import com.shumai.taczpip.PrototypeConfig;
import net.minecraft.client.Minecraft;
import com.tacz.guns.api.item.nbt.AttachmentItemDataAccessor;
import com.tacz.guns.client.event.CameraSetupEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.client.event.ViewportEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CameraSetupEvent.class, remap = false)
public abstract class CameraSetupEventMixin {
    // Cancel only TaCZ zoom, preserving vanilla movement and other mods' FOV.
    @Inject(method = "applyScopeMagnification", at = @At("HEAD"), cancellable = true)
    private static void taczpip$worldFov(ViewportEvent.ComputeFov event, CallbackInfo ci) {
        var mc = Minecraft.getInstance();
        // Empty hand and rendered/held item transitions must take the same path.
        // TaCZ's asynchronous dynamics otherwise resumes from an obsolete target.
        if (FovRules.bypass(PrototypeConfig.ENABLED.get(), event.usedConfiguredFov(),
                mc.options.getCameraType().isFirstPerson(),
                mc.player != null && event.getCamera().getEntity() == mc.player)) ci.cancel();
    }
    @Redirect(method = "applyGunModelFovModifying", at = @At(value = "INVOKE",
            target = "Lcom/tacz/guns/api/item/nbt/AttachmentItemDataAccessor;getZoomNumberFromTag(Lnet/minecraft/nbt/CompoundTag;)I"))
    private static int taczpip$fixedModelFov(CompoundTag tag) {
        return OpticsPolicy.modelViewStage(AttachmentItemDataAccessor.getZoomNumberFromTag(tag));
    }
}
