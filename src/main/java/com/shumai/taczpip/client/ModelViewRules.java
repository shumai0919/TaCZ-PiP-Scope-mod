package com.shumai.taczpip.client;

/** Map visual zoom stages to a stable stage without changing the selected sight. */
public final class ModelViewRules {
    private ModelViewRules() {}
    public static int stage(int original, boolean hybrid, int[] views, int zoomCount) {
        if (original < 0) return original;
        if (!hybrid) return 0;
        // Unknown or inconsistent pack data: preserve TaCZ's selection.
        if (views == null || views.length == 0 || zoomCount <= 0)
            return original;
        int view = views[original % views.length];
        if (view <= 0) return original;
        // TaCZ stores a cumulative switch counter, NOT a bounded stage index.
        // Match its views[counter % views.length], including after many cycles.
        for (int i = 0; i < views.length; i++) {
            if (views[i % views.length] == view) return i;
        }
        return original;
    }
}
