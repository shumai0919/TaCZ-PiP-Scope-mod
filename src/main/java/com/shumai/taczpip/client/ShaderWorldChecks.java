package com.shumai.taczpip.client;

import com.mojang.logging.LogUtils;
import com.shumai.taczpip.TaczPipScope;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Opt-in test on a disposable COPY of a world. Never enabled in Prism. */
@Mod.EventBusSubscriber(modid = TaczPipScope.MOD_ID, value = Dist.CLIENT)
public final class ShaderWorldChecks {
    private static int frames;
    private static long started;
    private static boolean done;
    @SubscribeEvent public static void frame(TickEvent.RenderTickEvent event) {
        if (net.minecraftforge.fml.loading.FMLEnvironment.production || !Boolean.getBoolean("taczpip.shaderWorldChecks") || done || event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.screen != null) return;
        if (started == 0) started = System.nanoTime();
        frames++;
        if (Boolean.getBoolean("taczpip.shaderAdsChecks") && frames == 1) {
            // Replace only the disposable world's selected attachment with a
            // known optical sight; the copied EOS item may be in dot-site mode.
            var server = mc.getSingleplayerServer();
            var playerId = mc.player.getUUID();
            if (server != null) server.execute(() -> {
                var player = server.getPlayerList().getPlayer(playerId);
                if (player != null) {
                    var attachment = new net.minecraft.nbt.CompoundTag();
                    attachment.putString("id", "tacz:attachment");
                    attachment.putByte("Count", (byte) 1);
                    var tag = new net.minecraft.nbt.CompoundTag();
                    tag.putString("AttachmentId", "tacz:scope_standard_8x");
                    tag.putInt("ZoomNumber", 0);
                    attachment.put("tag", tag);
                    player.getMainHandItem().getOrCreateTag().put("AttachmentSCOPE", attachment);
                }
            });
        }
        if (Boolean.getBoolean("taczpip.shaderAdsChecks") && frames == 60) {
            com.tacz.guns.client.input.AimKey.AIM_KEY.setDown(true);
            com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator.fromLocalPlayer(mc.player).aim(true);
            LogUtils.getLogger().info("[TaCZ Enhanced PiP Scopes] Test ADS started: {}", mc.player.getMainHandItem().getTag());
        }
        if (frames < 120 || System.nanoTime() - started < 15_000_000_000L) return;
        done = true;
        try {
            if (!OculusCompat.shadersActive() || !ScopeFramebufferPrototype.hasLensFrame())
                throw new IllegalStateException("Shader PiP target unavailable");
            if (Boolean.getBoolean("taczpip.shaderAdsChecks") && !ScopeLensComposite.hasComposite())
                throw new IllegalStateException("Optical lens was not composited");
            if (Boolean.getBoolean("taczpip.shaderAdsChecks") &&
                    com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator.fromLocalPlayer(mc.player)
                            .getClientAimingProgress(1) < 0.99f)
                throw new IllegalStateException("Test player did not reach ADS");
            java.nio.file.Path folder = java.nio.file.Path.of("../work");
            try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                image.writeToFile(folder.resolve("shader-main-check.png"));
            }
            try (var image = Screenshot.takeScreenshot(ScopeFramebufferPrototype.lensTarget())) {
                image.writeToFile(folder.resolve("shader-scope-check.png"));
            }
            LogUtils.getLogger().info("[TaCZ Enhanced PiP Scopes] Shader world checks PASSED: {} frames, target available; images saved for visual review", frames);
        } catch (Exception error) {
            LogUtils.getLogger().error("[TaCZ Enhanced PiP Scopes] Shader world checks FAILED", error);
        }
        mc.stop();
    }
}
