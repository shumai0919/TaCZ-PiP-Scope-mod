import com.shumai.taczpip.client.LensStencilRules;
import java.util.Arrays;
import java.util.List;

public class LensStencilRulesTest {
    private static void indices(boolean selective, List<Boolean> flags, int count, int... expected) {
        if (!Arrays.equals(LensStencilRules.indices(selective, flags, count), expected))
            throw new AssertionError("Incorrect optical selection");
    }
    public static void main(String[] args) {
        indices(false, List.of(), 2, 1, 2);
        indices(true, List.of(false, true), 2, 2);
        indices(true, List.of(true, false), 2, 1);
        indices(true, List.of(false, true, true), 3, 2, 3);
        indices(true, List.of(false, false), 2);
        indices(true, List.of(), 2);
        indices(false, List.of(), 0);
        for (int i = 0; i < 127; i++) {
            int high = LensStencilRules.reticleReference(true, i);
            int low = LensStencilRules.reticleReference(false, i);
            if (high <= 127 || high >= 255 || low <= 0 || low > 127 || (high ^ low) != 255)
                throw new AssertionError("Stencil partition broken");
        }
        if (LensStencilRules.indices(false, List.of(), 200).length != 127)
            throw new AssertionError("Stencil overflow");
        System.out.println("Lens stencil tests PASSED: hybrid selection, missing flags, all 127 references, overflow");
    }
}
