import com.shumai.taczpip.client.EyeBoxRules;
public final class EyeBoxRulesTest {
    public static void main(String[] args) {
        if (EyeBoxRules.radius(0, 100, 0, 100, 100) != 0) throw new AssertionError("hip fire");
        if (EyeBoxRules.radius(.5, 100, 0, 100, 100) != 50) throw new AssertionError("circular pupil");
        if (EyeBoxRules.radius(.5, 0, 0, 0, 100) != 0) throw new AssertionError("invalid bounds");
        if (EyeBoxRules.radius(Double.NaN, 0, 0, 100, 100) != 0) throw new AssertionError("NaN");
        for (int offset = 0; offset <= 1000; offset += 50) {
            double full = EyeBoxRules.radius(.98, offset, -offset, 100, 150);
            if (full < Math.hypot(offset, offset) + Math.hypot(100,150)) throw new AssertionError("settled clipping");
            double prev = 0;
            for (int i=0; i<=100; i++) {
                double r = EyeBoxRules.radius(i/100.0, offset, -offset, 100,150);
                if (r < prev) throw new AssertionError("transition discontinuity");
                prev=r;
            }
        }
        System.out.println("EyeBoxRules: transition, offsets, full coverage and invalid input passed");
    }
}
