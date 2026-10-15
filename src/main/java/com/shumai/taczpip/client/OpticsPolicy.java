package com.shumai.taczpip.client;

import com.shumai.taczpip.PrototypeConfig;
import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.other.KeepingItemRenderer;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.nbt.AttachmentItemDataAccessor;
import net.minecraft.client.Minecraft;

/** Data-driven classification for built-in and gun-pack optics. */
public final class OpticsPolicy {
    private OpticsPolicy() {}
    public static net.minecraft.resources.ResourceLocation scopeTexture(Object model) {
        if (!isLocalGun()) return null;
        var stack = KeepingItemRenderer.getRenderer().getCurrentItem();
        var gun = IGun.getIGunOrNull(stack);
        var id = gun.getAttachmentId(stack, AttachmentType.SCOPE);
        if (DefaultAssets.isEmptyAttachmentId(id)) id = gun.getBuiltInAttachmentId(stack, AttachmentType.SCOPE);
        return TimelessAPI.getClientAttachmentIndex(id)
                .filter(index -> index.getAttachmentModel() == model)
                .map(index -> index.getModelTexture()).orElse(null);
    }
    public static boolean isLocalGun() {
        var mc = Minecraft.getInstance();
        return PrototypeConfig.ENABLED.get() && mc.player != null
                && mc.options.getCameraType().isFirstPerson() && mc.getCameraEntity() == mc.player
                && IGun.getIGunOrNull(KeepingItemRenderer.getRenderer().getCurrentItem()) != null;
    }
    public static boolean isSupportedScopeActive() {
        if (!isLocalGun()) return false;
        var stack = KeepingItemRenderer.getRenderer().getCurrentItem();
        var gun = IGun.getIGunOrNull(stack);
        if (gun == null) return false;
        var id = gun.getAttachmentId(stack, AttachmentType.SCOPE);
        if (DefaultAssets.isEmptyAttachmentId(id)) id = gun.getBuiltInAttachmentId(stack, AttachmentType.SCOPE);
        return TimelessAPI.getClientAttachmentIndex(id)
                .map(index -> index.isScope() && index.getAttachmentModel() instanceof OpticalModel model
                        && model.taczpip$hasOpticalLens()).orElse(false);
    }
    public static boolean isOpticalViewSelected() {
        if (!isLocalGun()) return false;
        var stack = KeepingItemRenderer.getRenderer().getCurrentItem();
        var gun = IGun.getIGunOrNull(stack);
        var id = gun.getAttachmentId(stack, AttachmentType.SCOPE);
        if (DefaultAssets.isEmptyAttachmentId(id)) id = gun.getBuiltInAttachmentId(stack, AttachmentType.SCOPE);
        int counter = AttachmentItemDataAccessor.getZoomNumberFromTag(gun.getAttachmentTag(stack, AttachmentType.SCOPE));
        return TimelessAPI.getClientAttachmentIndex(id).map(index -> {
            int[] views = index.getViews();
            if (counter < 0 || views == null || views.length == 0) return false;
            int view = views[counter % views.length] - 1;
            return view >= 0 && index.getAttachmentModel() instanceof OpticalModel model
                    && model.taczpip$isOpticalView(view);
        }).orElse(false);
    }
    public static int modelViewStage(int original) {
        if (!PrototypeConfig.LOCK_MODEL_VIEW.get() || !isSupportedScopeActive()) return original;
        var stack = KeepingItemRenderer.getRenderer().getCurrentItem();
        var gun = IGun.getIGunOrNull(stack);
        var id = gun.getAttachmentId(stack, AttachmentType.SCOPE);
        if (DefaultAssets.isEmptyAttachmentId(id)) id = gun.getBuiltInAttachmentId(stack, AttachmentType.SCOPE);
        // Same view => one stable model FOV; a different view still switches sights.
        // This affects rendering only, never the NBT zoom or PiP magnification.
        return TimelessAPI.getClientAttachmentIndex(id).map(index -> ModelViewRules.stage(
                original, index.isSight(), index.getViews(),
                index.getZoom() == null ? 0 : index.getZoom().length)).orElse(original);
    }
}
