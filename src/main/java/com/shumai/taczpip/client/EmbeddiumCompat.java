package com.shumai.taczpip.client;

import net.minecraftforge.fml.ModList;

/** Deliberately version-gated: other renderers/versions need their own validation. */
public final class EmbeddiumCompat {
    private EmbeddiumCompat() {}

    public static boolean supportedVersion() {
        return ModList.get().getModContainerById("embeddium")
                .map(mod -> "0.3.31+mc1.20.1".equals(mod.getModInfo().getVersion().toString()))
                .orElse(false);
    }

    public static boolean reuseMainTerrain() {
        // Shader scopes run BEFORE the main view, and have their own shadow pass.
        // They must build terrain normally; the vanilla late pass can reuse it.
        return ScopeFramebufferPrototype.isRenderingWorld() && supportedVersion()
                && !OculusCompat.shadersActive() && !DhScopeContext.loaded();
    }
}
