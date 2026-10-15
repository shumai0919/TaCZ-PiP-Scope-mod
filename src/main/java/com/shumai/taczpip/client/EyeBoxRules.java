package com.shumai.taczpip.client;

/** Screen-space entrance pupil during ADS transitions, not a physical optical simulation. */
public final class EyeBoxRules {
    private EyeBoxRules() {}
    public static double radius(double progress, double centerX, double centerY, double halfWidth, double halfHeight) {
        if (!Double.isFinite(progress) || !Double.isFinite(centerX) || !Double.isFinite(centerY)
                || !Double.isFinite(halfWidth) || !Double.isFinite(halfHeight)
                || halfWidth <= 0 || halfHeight <= 0) return 0;
        double p = Math.max(0, Math.min(1, progress));
        double base = Math.max(halfWidth, halfHeight) * p;
        // Fully contain the ocular before disabling the pupil gate. Recoil and
        // high zoom at settled ADS must never reintroduce an artificial edge.
        double full = Math.hypot(centerX, centerY) + Math.hypot(halfWidth, halfHeight) + 2;
        double t = Math.max(0, Math.min(1, (p - .85) / .13));
        t = t * t * (3 - 2 * t);
        return base + (full - base) * t;
    }
}
