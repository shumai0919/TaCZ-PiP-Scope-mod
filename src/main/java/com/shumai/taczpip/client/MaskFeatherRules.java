package com.shumai.taczpip.client;

public final class MaskFeatherRules {
    private MaskFeatherRules() {}
    public static float width(float pupilRadius, float lensRadius, float progress, double strength) {
        if (!Float.isFinite(pupilRadius) || !Float.isFinite(lensRadius) || !Float.isFinite(progress)
                || !Double.isFinite(strength) || pupilRadius <= 0 || lensRadius <= 0
                || progress >= .98f || strength <= 0) return 0;
        double t = Math.max(0, Math.min(1, (progress - .90) / .08));
        // Disappear continuously before the original pupil gate is removed.
        double fade = 1 - t*t*(3-2*t);
        return (float)(Math.min(pupilRadius, lensRadius) * Math.min(strength, 1.0) * fade);
    }
}
