package com.shumai.taczpip.client;

/** Render-thread-only, scoped to the extra inner-gun draw. No GL synchronization. */
public final class GunDrawProfile {
    private static Sample current;
    private static final class Sample { long vertices, submit; }
    private GunDrawProfile() {}

    public static void draw(Runnable draw) {
        long start = RenderMetrics.begin();
        if (start == 0) { draw.run(); return; }
        Sample previous = current;
        current = new Sample();
        try { draw.run(); }
        finally {
            current = previous;
            RenderMetrics.end(RenderMetrics.BODY_MODEL, start);
        }
    }
    public static void modelStart() {
        if (current != null) current.vertices = RenderMetrics.begin();
    }
    public static void beforeSubmit() {
        if (current == null || current.vertices == 0) return;
        RenderMetrics.end(RenderMetrics.BODY_VERTICES, current.vertices);
        current.vertices = 0;
        current.submit = RenderMetrics.begin();
    }
    public static void modelEnd() {
        if (current == null || current.submit == 0) return;
        // Includes fallback flush, functional renderers and final list reset.
        RenderMetrics.end(RenderMetrics.BODY_SUBMIT_TAIL, current.submit);
        current.submit = 0;
    }
}
