package com.shumai.taczpip.client;

import java.util.List;
import java.util.stream.IntStream;

public final class LensStencilRules {
    private LensStencilRules() {}
    public static boolean optical(boolean selective, List<Boolean> flags, int index) {
        return index >= 0 && index < 127 && (!selective
                || (index < flags.size() && Boolean.TRUE.equals(flags.get(index))));
    }
    public static int[] indices(boolean selective, List<Boolean> flags, int count) {
        return IntStream.range(0, Math.min(127, Math.max(0, count)))
                .filter(i -> optical(selective, flags, i)).map(i -> i + 1).toArray();
    }
    public static int reticleReference(boolean optical, int index) {
        return optical ? (~(index + 1) & 0xff) : index + 1;
    }
}
