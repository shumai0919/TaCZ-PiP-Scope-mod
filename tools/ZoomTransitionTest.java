import com.shumai.taczpip.client.ZoomTransition;

/** Standalone deterministic regression tests; no Minecraft/OpenGL required. */
public final class ZoomTransitionTest {
    private static void close(double actual, double expected) {
        if (Math.abs(actual - expected) > 1e-8) throw new AssertionError(actual + " != " + expected);
    }
    public static void main(String[] args) {
        ZoomTransition z = new ZoomTransition();
        close(z.update(5, 0, .2), 5);
        close(z.update(10, 1_000_000_000L, .2), 5); // No instant jump.
        close(z.update(10, 1_100_000_000L, .2), 1 / (.5 / 5 + .5 / 10));
        close(z.update(10, 1_200_000_000L, .2), 10);
        close(z.update(5, 1_300_000_000L, .2), 10);
        double middle = z.update(5, 1_400_000_000L, .2);
        close(z.update(25, 1_400_000_000L, .2), middle); // Mid-transition reversal is continuous.
        close(z.update(25, 1_600_000_000L, .2), 25);
        z.reset();
        close(z.update(1.25, 2_000_000_000L, .2), 1.25); // New weapon/ADS does not inherit zoom.
        close(z.update(6.25, 2_000_000_001L, 0), 6.25);
        for (int fps : new int[]{30, 60, 144}) {
            z.reset();
            z.update(5, 0, .2);
            z.update(10, 0, .2);
            double previous = 5;
            for (int i = 1; i <= fps; i++) {
                double value = z.update(10, Math.round(i * 1e9 / fps), .2);
                if (value < previous || value > 10) throw new AssertionError("Overshoot");
                previous = value;
            }
            close(previous, 10);
        }
        System.out.println("ZoomTransition: all checks passed (30/60/144 FPS, reversal, reset, instant mode)");
    }
}
