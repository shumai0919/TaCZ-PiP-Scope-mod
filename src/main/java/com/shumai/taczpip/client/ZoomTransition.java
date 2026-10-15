package com.shumai.taczpip.client;

/** Time-based interpolation of tan(FOV/2), independent of rendering and FPS. */
public final class ZoomTransition {
    private double from = Double.NaN, target;
    private long start;
    private double durationNanos;

    public void reset() { from = Double.NaN; }

    public double update(double requested, long now, double seconds) {
        if (!Double.isFinite(requested) || requested < 1 || !Double.isFinite(seconds) || seconds < 0)
            throw new IllegalArgumentException("Invalid zoom transition");
        if (Double.isNaN(from)) {
            from = target = requested;
            start = now;
            durationNanos = seconds * 1_000_000_000.0;
        }
        double current = value(now);
        if (requested != target) {
            from = current;
            target = requested;
            start = now;
            durationNanos = seconds * 1_000_000_000.0;
        }
        return durationNanos <= 0 ? target : current;
    }

    private double value(long now) {
        double t = durationNanos <= 0 ? 1 : Math.max(0, Math.min(1, (now - start) / durationNanos));
        double eased = t * t * (3 - 2 * t);
        return 1 / ((1 - eased) / from + eased / target);
    }
}
