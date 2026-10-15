package com.shumai.taczpip.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.shumai.taczpip.TaczPipScope;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.io.IOException;

/** World framebuffer alpha is not lens coverage; coverage comes from stencil. */
@Mod.EventBusSubscriber(modid = TaczPipScope.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ScopeImageShader {
    private static ShaderInstance shader;
    private static ShaderInstance maskFeather;
    private static ShaderInstance gunComposite;
    public static ShaderInstance gunComposite() { return gunComposite; }
    public static ShaderInstance maskFeather() { return maskFeather; }
    private ScopeImageShader() {}

    @SubscribeEvent
    public static void register(RegisterShadersEvent event) throws IOException {
        ScopeFramebufferPrototype.invalidateFrame();
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
                new ResourceLocation(TaczPipScope.MOD_ID, "scope_gun"), DefaultVertexFormat.POSITION_TEX) {
                    @Override public void apply() {
                        super.apply();
                        OculusCompat.allowLensColorWrites();
                    }
                }, loaded -> gunComposite = loaded);
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
                new ResourceLocation(TaczPipScope.MOD_ID, "scope_mask_feather"),
                DefaultVertexFormat.POSITION_TEX) {
                    @Override public void apply() {
                        super.apply();
                        OculusCompat.allowLensColorWrites();
                    }
                }, loaded -> maskFeather = loaded);
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
                new ResourceLocation(TaczPipScope.MOD_ID, "scope_image"),
                DefaultVertexFormat.POSITION_TEX) {
                    @Override public void apply() {
                        super.apply();
                        OculusCompat.allowLensColorWrites();
                    }
                }, loaded -> {
                    shader = loaded;
                    ReticleRenderType.clearCache();
                    ApertureAlpha.clear();
                    if (Boolean.getBoolean("taczpip.renderChecks")) RenderChecks.run();
                });
    }

    public static ShaderInstance get() { return shader; }
}
