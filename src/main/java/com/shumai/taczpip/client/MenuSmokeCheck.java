package com.shumai.taczpip.client;

import com.mojang.logging.LogUtils;
import com.shumai.taczpip.TaczPipScope;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

/** Explicit development-only launch check; never opens or changes a world. */
@Mod.EventBusSubscriber(modid = TaczPipScope.MOD_ID, value = Dist.CLIENT)
public final class MenuSmokeCheck {
    private static int frames;
    private static boolean done;

    @SubscribeEvent
    public static void frame(TickEvent.RenderTickEvent event) {
        if (FMLEnvironment.production || !Boolean.getBoolean("taczpip.menuSmoke")
                || done || event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.screen instanceof TitleScreen) || mc.getOverlay() != null) {
            frames = 0;
            return;
        }
        if (++frames < 120) return;
        done = true;
        if (ScopeImageShader.get() == null || ScopeImageShader.gunComposite() == null
                || ScopeImageShader.maskFeather() == null)
            throw new IllegalStateException("PiP menu smoke check: shaders not loaded");
        LogUtils.getLogger().info("[TaCZ Enhanced PiP Scopes] Menu smoke PASSED: title screen rendered 120 frames; PiP shaders loaded. No world/ADS validation.");
        mc.stop();
    }
}
