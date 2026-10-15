package com.shumai.taczpip;

import net.minecraftforge.common.ForgeConfigSpec;

public final class PrototypeConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.BooleanValue LENS;
    public static final java.util.function.Supplier<Boolean> FRAMEBUFFER = () -> PrototypeConfig.LENS.get();
    public static final java.util.function.Supplier<Boolean> PREVIEW = () -> false;
    public static final ForgeConfigSpec.DoubleValue ZOOM_TRANSITION_SECONDS;
    public static final ForgeConfigSpec.BooleanValue LOCK_MODEL_VIEW;
    public static final ForgeConfigSpec.BooleanValue SHOW_GUN;
    // Internal switches are not registered in the user-facing configuration.
    public static final java.util.function.Supplier<Boolean> DEBUG_LENS_MASK = () -> false;
    // Normal-use build: stale launcher arguments/marker files cannot enable profiling.
    private static final boolean PROFILE = false;
    public static final java.util.function.Supplier<Boolean> METRICS = () -> PROFILE;
    public static final java.util.function.Supplier<Boolean> GPU_METRICS = () -> PROFILE;
    public static final java.util.function.Supplier<Boolean> DEFER_RETICLE = () -> true;
    public static final ForgeConfigSpec.BooleanValue LENS_EFFECTS, LENS_DISTORTION, CHROMATIC_ABERRATION, LENS_VIGNETTE, EYEBOX_EFFECT;
    public static final ForgeConfigSpec.DoubleValue DISTORTION_STRENGTH, CHROMATIC_STRENGTH, VIGNETTE_STRENGTH;
    public static final ForgeConfigSpec.DoubleValue MASK_EDGE_SOFTNESS;
    public static final ForgeConfigSpec.BooleanValue LENS_COATING, LENS_HAZE;
    public static final ForgeConfigSpec.DoubleValue COATING_STRENGTH, HAZE_STRENGTH;
    public static final ForgeConfigSpec.IntValue PIP_RESOLUTION, PIP_MAX_FPS;
    public static final ForgeConfigSpec.BooleanValue PIP_ENTITIES, PIP_BLOCK_ENTITIES, PIP_PARTICLES;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        ENABLED = builder.comment("Keep the background unzoomed while aiming.")
                .translation("tacz_pip_scope.config.enableWorldFovPrototype")
                .define("enableWorldFovPrototype", true);
        LENS = builder.comment("Render and display the magnified scope view.")
                .translation("tacz_pip_scope.config.enableLensComposite")
                .define("enableLensComposite", true);
        ZOOM_TRANSITION_SECONDS = builder.comment("Zoom transition duration in seconds; zero is instant.")
                .translation("tacz_pip_scope.config.zoomTransitionSeconds")
                .defineInRange("zoomTransitionSeconds", 0.20, 0.0, 1.0);
        LOCK_MODEL_VIEW = builder.comment("Keep the aiming position stable when switching magnification.")
                .translation("tacz_pip_scope.config.lockOpticalModelView")
                .define("lockOpticalModelView", true);
        SHOW_GUN = builder.comment("Show the gun through the scope.")
                .translation("tacz_pip_scope.config.showGunInsidePipLens")
                .define("showGunInsidePipLens", true);
        LENS_EFFECTS = builder.comment("Enable lens appearance effects.")
                .translation("tacz_pip_scope.config.lensEffects").define("lensEffects", true);
        LENS_DISTORTION = builder.translation("tacz_pip_scope.config.lensDistortion").define("lensDistortion", true);
        DISTORTION_STRENGTH = builder.comment("Lens distortion strength; high values can fold the image.")
                .translation("tacz_pip_scope.config.distortionStrength").defineInRange("distortionStrength", 0.08, 0.0, 1.0);
        CHROMATIC_ABERRATION = builder.translation("tacz_pip_scope.config.chromaticAberration").define("chromaticAberration", true);
        CHROMATIC_STRENGTH = builder.comment("Color separation strength.")
                .translation("tacz_pip_scope.config.chromaticStrength").defineInRange("chromaticStrength", 0.0015, 0.0, 0.02);
        LENS_VIGNETTE = builder.translation("tacz_pip_scope.config.lensVignette").define("lensVignette", true);
        // Configured 2.2.3 expects text before Forge's generated Range: suffix.
        VIGNETTE_STRENGTH = builder.comment("Darkening at the lens edge.")
                .translation("tacz_pip_scope.config.vignetteStrength").defineInRange("vignetteStrength", 0.6, 0.0, 1.0);
        LENS_COATING = builder.translation("tacz_pip_scope.config.lensCoating").define("lensCoating", true);
        COATING_STRENGTH = builder.comment("Approximate colored lens reflection strength; zero disables it.")
                .translation("tacz_pip_scope.config.coatingStrength").defineInRange("coatingStrength", 0.12, 0.0, 1.0);
        LENS_HAZE = builder.translation("tacz_pip_scope.config.lensHaze").define("lensHaze", true);
        HAZE_STRENGTH = builder.comment("Brightness-driven lens veil strength; zero disables it.")
                .translation("tacz_pip_scope.config.hazeStrength").defineInRange("hazeStrength", 0.08, 0.0, 1.0);
        EYEBOX_EFFECT = builder.comment("Soften the aperture-shaped aiming mask edge.")
                .translation("tacz_pip_scope.config.eyeboxEffect").define("eyeboxEffect", true);
        MASK_EDGE_SOFTNESS = builder.comment("Aiming mask feather width; zero disables feathering.")
                .translation("tacz_pip_scope.config.maskEdgeSoftness").defineInRange("maskEdgeSoftness", 0.1, 0.0, 1.0);
        builder.comment("Scope-only performance settings.").translation("tacz_pip_scope.config.performance").push("performance");
        PIP_RESOLUTION = builder.comment("Scope render resolution percent. Lower values reduce detail.")
                .translation("tacz_pip_scope.config.pipResolution").defineInRange("pipResolution", 100, 25, 100);
        PIP_MAX_FPS = builder.comment("Maximum scope world updates per second. Zero is unlimited; lower values cause visual lag.")
                .translation("tacz_pip_scope.config.pipMaxFps").defineInRange("pipMaxFps", 0, 0, 240);
        PIP_ENTITIES = builder.comment("Render entities inside the scope. Disabling also hides players and mobs.")
                .translation("tacz_pip_scope.config.pipEntities").define("pipEntities", true);
        PIP_BLOCK_ENTITIES = builder.comment("Render block entities such as chests inside the scope.")
                .translation("tacz_pip_scope.config.pipBlockEntities").define("pipBlockEntities", true);
        PIP_PARTICLES = builder.comment("Render particles inside the scope.")
                .translation("tacz_pip_scope.config.pipParticles").define("pipParticles", true);
        builder.pop();
        SPEC = builder.build();
    }

    private PrototypeConfig() {}
}
