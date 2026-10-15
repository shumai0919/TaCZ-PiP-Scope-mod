import com.shumai.taczpip.client.PipPerformanceRules;

public class PipPerformanceRulesTest {
    public static void main(String[] args) {
        check(PipPerformanceRules.dimension(1920, 100) == 1920);
        check(PipPerformanceRules.dimension(1920, 75) == 1440);
        check(PipPerformanceRules.dimension(1080, 50) == 540);
        check(PipPerformanceRules.dimension(1, 25) == 1);
        check(PipPerformanceRules.dimension(1920, -1) == 480);
        check(PipPerformanceRules.dimension(1920, 150) == 1920);
        check(PipPerformanceRules.due(false, 1, 1, 1)); // Invalidated/first frame.
        check(PipPerformanceRules.due(true, 1, 1, 0)); // Unlimited.
        check(!PipPerformanceRules.due(true, 33_333_332, 0, 30));
        check(PipPerformanceRules.due(true, 33_333_333, 0, 30));
        check(PipPerformanceRules.due(true, 0, 1, 30)); // Clock rollback.
        check(!PipPerformanceRules.due(true, 999_999_999, 0, 1));
        check(PipPerformanceRules.due(true, 1_000_000_000, 0, 1));
        System.out.println("PASS: PiP dimensions, bounds, unlimited/capped updates, invalidation, clock rollback");
    }
    private static void check(boolean valid) { if (!valid) throw new AssertionError(); }
}
