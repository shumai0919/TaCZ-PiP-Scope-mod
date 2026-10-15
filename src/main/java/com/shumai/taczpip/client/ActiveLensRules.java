package com.shumai.taczpip.client;

/** Match a pack's eye position to the nearest lens axis, not zoom magnitude or ID. */
public final class ActiveLensRules {
    private ActiveLensRules() {}
    public static boolean optical(double x, double y, double[] lensX, double[] lensY, boolean[] flags) {
        if (!Double.isFinite(x) || !Double.isFinite(y)) return false;
        double best = Double.POSITIVE_INFINITY;
        boolean optical = false;
        for (int i = 0; i < Math.min(flags.length, Math.min(lensX.length, lensY.length)); i++) {
            double dx = x - lensX[i], dy = y - lensY[i];
            double distance = dx * dx + dy * dy;
            if (!Double.isFinite(distance)) continue;
            if (distance < best - 1e-10) { best = distance; optical = flags[i]; }
            else if (Math.abs(distance - best) <= 1e-10 && !flags[i]) optical = false;
        }
        return optical;
    }
}
