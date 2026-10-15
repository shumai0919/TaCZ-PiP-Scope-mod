package com.shumai.taczpip.client;

/** Pure sizing and scheduling rules; no Minecraft or GL state. */
public final class PipPerformanceRules {
    private PipPerformanceRules() {}
    public static int dimension(int full, int percent) {
        return Math.max(1, (int)Math.round(full * Math.max(25, Math.min(100, percent)) / 100.0));
    }
    public static boolean due(boolean cached, long now, long previous, int fps) {
        return !cached || fps <= 0 || now < previous || now - previous >= 1_000_000_000L / fps;
    }
}
