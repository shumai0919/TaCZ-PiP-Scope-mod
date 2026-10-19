package com.shumai.taczpip.client;

import java.util.function.Function;
import java.util.function.Predicate;

/** A virtual HUD has no unlit aperture geometry, only emissive descendants. */
public final class HudOpticRules {
    private HudOpticRules() {}
    public static <T> boolean emissiveOnly(T root, Function<T, ? extends Iterable<T>> children,
                                          Predicate<T> geometry, Predicate<T> illuminated) {
        return classify(root,false,children,geometry,illuminated)==1;
    }
    private static <T> int classify(T node, boolean inherited, Function<T, ? extends Iterable<T>> children,
                                    Predicate<T> geometry, Predicate<T> illuminated) {
        boolean lit=inherited||illuminated.test(node);
        int result=geometry.test(node)?(lit?1:2):0;
        for(T child:children.apply(node)) result|=classify(child,lit,children,geometry,illuminated);
        return result;
    }
}
