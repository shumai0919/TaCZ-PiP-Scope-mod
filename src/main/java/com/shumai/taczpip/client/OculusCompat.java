package com.shumai.taczpip.client;

import com.mojang.logging.LogUtils;
import java.lang.reflect.Method;
import net.minecraftforge.fml.ModList;

/** Version-gated optional API bridge for the isolated shader scope path. */
public final class OculusCompat {
    private static Object api;
    private static Method shaderPackInUse;
    private static Method unlockDepthColor;
    private static boolean unavailable;

    private OculusCompat() {}

    public static boolean supportedVersion() {
        return ModList.get().getModContainerById("oculus")
                .map(mod -> "1.8.0".equals(mod.getModInfo().getVersion().toString()))
                .orElse(false);
    }

    public static boolean allowsPip() {
        if (!ModList.get().isLoaded("oculus")) return true;
        if (!supportedVersion() || !EmbeddiumCompat.supportedVersion() || unavailable) return false;
        try {
            if (shaderPackInUse == null) {
                Class<?> type = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                api = type.getMethod("getInstance").invoke(null);
                shaderPackInUse = type.getMethod("isShaderPackInUse");
            }
            // Read on every check: switching packs must not retain an old decision.
            shaderPackInUse.invoke(api); // Validate the optional API before allowing either path.
            return true;
        } catch (ReflectiveOperationException | LinkageError error) {
            unavailable = true;
            LogUtils.getLogger().warn("[TaCZ PiP Scope] Oculus API unavailable; PiP remains disabled", error);
            return false;
        }
    }

    public static boolean shadersActive() {
        if (!ModList.get().isLoaded("oculus") || !allowsPip()) return false;
        try { return Boolean.TRUE.equals(shaderPackInUse.invoke(api)); }
        catch (ReflectiveOperationException error) { return false; }
    }

    /** Iris suppresses color writes from non-pack shaders during world rendering.
     * Only our lens shader opts out, after its own apply(), with stencil intact. */
    public static void allowLensColorWrites() {
        if (!shadersActive()) return;
        try {
            if (unlockDepthColor == null) unlockDepthColor = Class.forName(
                    "net.irisshaders.iris.gl.blending.DepthColorStorage").getMethod("unlockDepthColor");
            unlockDepthColor.invoke(null);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Cannot restore lens color mask", error);
        }
    }
}
