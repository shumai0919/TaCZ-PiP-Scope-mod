import com.shumai.taczpip.client.ActiveLensRules;
public class ActiveLensRulesTest {
    public static void main(String[] args) {
        double[] x = {0, 0}, y = {5.26563, 2.125};
        boolean[] flags = {false, true};
        if (ActiveLensRules.optical(0, 5.21875, x, y, flags)) throw new AssertionError("dot");
        if (!ActiveLensRules.optical(0, 2.0625, x, y, flags)) throw new AssertionError("scope");
        if (!ActiveLensRules.optical(2, 0, new double[]{2, 0}, new double[]{0, 0}, new boolean[]{true, false})) throw new AssertionError("canted/reordered");
        if (ActiveLensRules.optical(0, 0, new double[]{0, 0}, new double[]{0, 0}, flags)) throw new AssertionError("ambiguous");
        if (ActiveLensRules.optical(0, 0, new double[0], new double[0], flags)) throw new AssertionError("empty");
        if (ActiveLensRules.optical(Double.NaN, 0, x, y, flags)) throw new AssertionError("invalid");
        System.out.println("ActiveLensRules: all checks passed");
    }
}
