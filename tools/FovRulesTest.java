import com.shumai.taczpip.client.FovRules;

public final class FovRulesTest {
    public static void main(String[] args) {
        for (int flags = 0; flags < 16; flags++) {
            boolean actual = FovRules.bypass((flags & 1) != 0, (flags & 2) != 0,
                    (flags & 4) != 0, (flags & 8) != 0);
            if (actual != (flags == 15)) throw new AssertionError("FOV flags " + flags);
        }
        // No held-item parameter: gun -> empty -> gun cannot switch this route.
        System.out.println("FovRules: all 16 configurations passed");
    }
}
