package com.shumai.taczpip.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.RenderType;
import org.lwjgl.opengl.GL11;
import java.util.IdentityHashMap;
import java.util.Map;

/** Override depth AFTER the source type sets up, not before endBatch(). */
public final class ReticleRenderType extends RenderType {
    private static final Map<RenderType, RenderType> TYPES = new IdentityHashMap<>();
    private record Key(RenderType source,net.minecraft.resources.ResourceLocation texture) {}
    private static final Map<Key,RenderType> FILTERED=new java.util.HashMap<>();

    private ReticleRenderType(RenderType source, DepthState state) {
        this(source,state,null);
    }
    private ReticleRenderType(RenderType source, DepthState state,net.minecraft.resources.ResourceLocation texture) {
        super("tacz_pip_reticle", source.format(), source.mode(), source.bufferSize(), false, false,
                () -> {
                    state.enabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
                    state.write = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
                    state.func = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
                    source.setupRenderState();
                    state.texture=RenderSystem.getShaderTexture(0);
                    if(texture!=null) RenderSystem.setShaderTexture(0,texture);
                    RenderSystem.disableDepthTest();
                    RenderSystem.depthMask(false);
                }, () -> {
                    source.clearRenderState();
                    if(texture!=null) RenderSystem.setShaderTexture(0,state.texture);
                    RenderSystem.depthFunc(state.func);
                    RenderSystem.depthMask(state.write);
                    if (state.enabled) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
                });
    }
    public static RenderType overlay(RenderType source) {
        return TYPES.computeIfAbsent(source, key -> new ReticleRenderType(key, new DepthState()));
    }
    public static RenderType overlay(RenderType source,net.minecraft.resources.ResourceLocation texture) {
        if(texture==null) return overlay(source);
        return FILTERED.computeIfAbsent(new Key(source,texture),key->new ReticleRenderType(source,new DepthState(),texture));
    }
    public static void clearCache() { TYPES.clear(); FILTERED.clear(); ReticleTexture.clear(); }
    private static final class DepthState { boolean enabled, write; int func,texture; }
}
