package com.shumai.taczpip.client;

import java.util.*;
import net.minecraft.client.resources.language.I18n;

/** Optional Configured integration. No Configured classes are linked when it is absent. */
public final class ConfigDisplayOrder {
    private ConfigDisplayOrder() {}
    public static final List<String> KEYS=List.of(
        "enableWorldFovPrototype","enableLensComposite","zoomTransitionSeconds","lockOpticalModelView","showGunInsidePipLens","lensEffects",
        "lensDistortion","distortionStrength","edgeRefraction","edgeRefractionStrength","chromaticAberration","chromaticStrength",
        "lensVignette","vignetteStrength","peripheralBlur","peripheralBlurStrength","eyeboxEffect","maskEdgeSoftness",
        "lensCoating","coatingStrength","lensReflection","reflectionStrength","lensHaze","hazeStrength","lensGhost","ghostStrength",
        "surfaceTexture","surfaceTextureType","surfacePattern","lensDirt","dirtStrength","lensScratches","scratchStrength","scratchSize","scratchDensity",
        "gunFocusBlur","gunBlurStrength","idleGlass","idleGlassStrength","performance",
        "pipResolution","pipMaxFps","pipEntities","pipBlockEntities","pipParticles");
    public static void sort(Object screen,List<Object> items,Comparator<Object> original) {
        try {
            Object config=screen.getClass().getMethod("getActiveConfig").invoke(screen);
            if(!"tacz_pip_scope".equals(config.getClass().getMethod("getModId").invoke(config))) {
                items.sort(original);return;
            }
            Map<String,Integer> order=new HashMap<>();
            for(int i=0;i<KEYS.size();i++)order.put(I18n.get("tacz_pip_scope.config."+KEYS.get(i)),i);
            Map<Object,Integer> ranks=new IdentityHashMap<>();
            for(Object item:items) {
                String label=(String)item.getClass().getMethod("getLabel").invoke(item);
                ranks.put(item,order.getOrDefault(label,Integer.MAX_VALUE));
            }
            items.sort(Comparator.<Object>comparingInt(ranks::get).thenComparing(original));
        } catch(ReflectiveOperationException|RuntimeException unavailable) {
            items.sort(original); // Different Configured versions retain their normal UI.
        }
    }
}
