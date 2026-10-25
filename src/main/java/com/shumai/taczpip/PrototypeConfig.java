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
    public static final ForgeConfigSpec.BooleanValue LENS_REFLECTION, PERIPHERAL_BLUR;
    public static final ForgeConfigSpec.DoubleValue REFLECTION_STRENGTH, PERIPHERAL_BLUR_STRENGTH;
    public static final ForgeConfigSpec.BooleanValue LENS_DIRT, LENS_SCRATCHES;
    public static final ForgeConfigSpec.DoubleValue DIRT_STRENGTH, SCRATCH_STRENGTH;
    public static final ForgeConfigSpec.DoubleValue SCRATCH_DENSITY, SCRATCH_SIZE;
    public static final ForgeConfigSpec.IntValue SURFACE_PATTERN;
    public static final ForgeConfigSpec.IntValue SURFACE_TEXTURE_TYPE;
    public static final ForgeConfigSpec.BooleanValue EDGE_REFRACTION, LENS_GHOST, GUN_FOCUS_BLUR, IDLE_GLASS, SURFACE_TEXTURE;
    public static final ForgeConfigSpec.DoubleValue EDGE_REFRACTION_STRENGTH, GHOST_STRENGTH, GUN_BLUR_STRENGTH, IDLE_GLASS_STRENGTH;
    public static final ForgeConfigSpec.IntValue PIP_RESOLUTION, PIP_MAX_FPS;
    public static final ForgeConfigSpec.BooleanValue PIP_ENTITIES, PIP_BLOCK_ENTITIES, PIP_PARTICLES;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        // Basic. Preserve existing config paths and saved values.
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

        // Optical image and aperture. Preserve existing config paths and saved values.
        LENS_DISTORTION = builder.translation("tacz_pip_scope.config.lensDistortion").define("lensDistortion", true);
        DISTORTION_STRENGTH = builder.comment("Lens distortion strength; high values can fold the image.")
                .translation("tacz_pip_scope.config.distortionStrength").defineInRange("distortionStrength", 0.08, 0.0, 1.0);
        EDGE_REFRACTION = builder.translation("tacz_pip_scope.config.edgeRefraction").define("edgeRefraction", true);
        EDGE_REFRACTION_STRENGTH = builder.comment("Narrow edge refraction strength; zero disables it.")
                .translation("tacz_pip_scope.config.edgeRefractionStrength").defineInRange("edgeRefractionStrength", 0.3, 0.0, 1.0);
        CHROMATIC_ABERRATION = builder.translation("tacz_pip_scope.config.chromaticAberration").define("chromaticAberration", true);
        CHROMATIC_STRENGTH = builder.comment("Color separation strength.")
                .translation("tacz_pip_scope.config.chromaticStrength").defineInRange("chromaticStrength", 0.0015, 0.0, 0.02);
        LENS_VIGNETTE = builder.translation("tacz_pip_scope.config.lensVignette").define("lensVignette", true);
        VIGNETTE_STRENGTH = builder.comment("Darkening at the lens edge.")
                .translation("tacz_pip_scope.config.vignetteStrength").defineInRange("vignetteStrength", 0.6, 0.0, 1.0);
        PERIPHERAL_BLUR = builder.translation("tacz_pip_scope.config.peripheralBlur").define("peripheralBlur", true);
        PERIPHERAL_BLUR_STRENGTH = builder.comment("Peripheral image softness; zero disables it.")
                .translation("tacz_pip_scope.config.peripheralBlurStrength").defineInRange("peripheralBlurStrength", 0.15, 0.0, 1.0);
        EYEBOX_EFFECT = builder.comment("Soften the aperture-shaped aiming mask edge.")
                .translation("tacz_pip_scope.config.eyeboxEffect").define("eyeboxEffect", true);
        MASK_EDGE_SOFTNESS = builder.comment("Aiming mask feather width; zero disables feathering.")
                .translation("tacz_pip_scope.config.maskEdgeSoftness").defineInRange("maskEdgeSoftness", 0.1, 0.0, 1.0);

        // Surface. Preserve existing config paths and saved values.
        LENS_COATING = builder.translation("tacz_pip_scope.config.lensCoating").define("lensCoating", true);
        COATING_STRENGTH = builder.comment("Approximate colored lens reflection strength; zero disables it.")
                .translation("tacz_pip_scope.config.coatingStrength").defineInRange("coatingStrength", 0.12, 0.0, 1.0);
        LENS_REFLECTION = builder.translation("tacz_pip_scope.config.lensReflection").define("lensReflection", true);
        REFLECTION_STRENGTH = builder.comment("View-dependent soft reflection; zero disables it.")
                .translation("tacz_pip_scope.config.reflectionStrength").defineInRange("reflectionStrength", 0.10, 0.0, 1.0);
        LENS_HAZE = builder.translation("tacz_pip_scope.config.lensHaze").define("lensHaze", true);
        HAZE_STRENGTH = builder.comment("Brightness-driven lens veil strength; zero disables it.")
                .translation("tacz_pip_scope.config.hazeStrength").defineInRange("hazeStrength", 0.08, 0.0, 1.0);
        LENS_GHOST = builder.translation("tacz_pip_scope.config.lensGhost").define("lensGhost", true);
        GHOST_STRENGTH = builder.comment("Approximate bright-scene ghost strength; zero disables it.")
                .translation("tacz_pip_scope.config.ghostStrength").defineInRange("ghostStrength", 0.1, 0.0, 1.0);
        // Keep paths/defaults stable: the procedural fallback is still used, not obsolete.
        SURFACE_TEXTURE = builder.comment("ON: image mode for dirt/scratches. OFF: procedural mode, not effects disabled.")
                .translation("tacz_pip_scope.config.surfaceTexture").define("surfaceTexture", true);
        SURFACE_TEXTURE_TYPE = builder.comment("Image mode only, shared by dirt/scratches: 0 original, 1 fine wear, 2 wiping marks, 3 smudges.")
                .translation("tacz_pip_scope.config.surfaceTextureType").defineInRange("surfaceTextureType", 2, 0, 3);
        SURFACE_PATTERN = builder.comment("Both modes: shared dirt/scratch pattern number. Rotates/shifts the image or changes procedural placement; does not select the image type.")
                .translation("tacz_pip_scope.config.surfacePattern").defineInRange("surfacePattern", 6, 0, 9999);
        LENS_DIRT = builder.translation("tacz_pip_scope.config.lensDirt").define("lensDirt", true);
        DIRT_STRENGTH = builder.comment("Both modes: darkening from dirt, independent of scratch opacity. Zero disables dirt.")
                .translation("tacz_pip_scope.config.dirtStrength").defineInRange("dirtStrength", 0.12, 0.0, 1.0);
        LENS_SCRATCHES = builder.translation("tacz_pip_scope.config.lensScratches").define("lensScratches", true);
        SCRATCH_STRENGTH = builder.comment("Both modes: scratch visibility, not count or placement. Zero disables scratches.")
                .translation("tacz_pip_scope.config.scratchStrength").defineInRange("scratchStrength", 0.6, 0.0, 1.0);
        SCRATCH_DENSITY = builder.comment("Procedural-only scratch count multiplier; ignored with surface texture enabled. Zero disables procedural scratches.")
                .translation("tacz_pip_scope.config.scratchDensity").defineInRange("scratchDensity", 4.0, 0.0, 4.0);
        SCRATCH_SIZE = builder.comment("Both modes: scratch image scale (may crop edges) or procedural line length/width. Does not scale dirt. Zero disables scratches.")
                .translation("tacz_pip_scope.config.scratchSize").defineInRange("scratchSize", 1.4, 0.0, 4.0);

        // Focus and non-ADS. Preserve existing config paths and saved values.
        GUN_FOCUS_BLUR = builder.translation("tacz_pip_scope.config.gunFocusBlur").define("gunFocusBlur", true);
        GUN_BLUR_STRENGTH = builder.comment("Inner gun layer focus softness; zero preserves the sharp path.")
                .translation("tacz_pip_scope.config.gunBlurStrength").defineInRange("gunBlurStrength", 0.2, 0.0, 1.0);
        IDLE_GLASS = builder.translation("tacz_pip_scope.config.idleGlass").define("idleGlass", true);
        IDLE_GLASS_STRENGTH = builder.comment("Non-ADS glass highlight strength; zero disables it.")
                .translation("tacz_pip_scope.config.idleGlassStrength").defineInRange("idleGlassStrength", 0.25, 0.0, 1.0);

        // Performance. Preserve existing config paths and saved values.
        builder.comment("Scope-only performance settings.").translation("tacz_pip_scope.config.performance").push("performance");
        PIP_RESOLUTION = builder.comment("Scope render resolution percent. Lower values reduce detail.")
                .translation("tacz_pip_scope.config.pipResolution").defineInRange("pipResolution", 75, 25, 100);
        PIP_MAX_FPS = builder.comment("Maximum scope world updates per second. Zero is unlimited; lower values cause visual lag.")
                .translation("tacz_pip_scope.config.pipMaxFps").defineInRange("pipMaxFps", 30, 0, 240);
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
