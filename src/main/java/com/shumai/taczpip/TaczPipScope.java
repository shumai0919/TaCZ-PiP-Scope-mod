package com.shumai.taczpip;

import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(TaczPipScope.MOD_ID)
public final class TaczPipScope {
    public static final String MOD_ID = "tacz_pip_scope";
    private static final Logger LOGGER = LogUtils.getLogger();

    public TaczPipScope() {
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(
                net.minecraftforge.fml.config.ModConfig.Type.CLIENT, PrototypeConfig.SPEC);

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::onCommonSetup);
    }

    private void onCommonSetup(final FMLCommonSetupEvent event) {
        if (!ModList.get().isLoaded("tacz")) {
            LOGGER.error("[TaCZ Enhanced PiP Scopes] TaCZ was not detected");
        }
    }
}
